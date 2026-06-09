package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.BusinessException;
import com.example.dto.GoalMilestoneDTO;
import com.example.entity.GoalMilestone;
import com.example.entity.SysNotification;
import com.example.mapper.GoalMilestoneMapper;
import com.example.mapper.SysNotificationMapper;
import com.example.service.GoalMilestoneService;
import com.example.vo.GoalMilestoneVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class GoalMilestoneServiceImpl implements GoalMilestoneService {

    private static final Map<String, String> GOAL_TYPE_LABELS = new HashMap<>();
    static {
        GOAL_TYPE_LABELS.put("weight_loss", "减重");
        GOAL_TYPE_LABELS.put("weight_gain", "增重");
        GOAL_TYPE_LABELS.put("muscle_gain", "增肌");
        GOAL_TYPE_LABELS.put("exercise_days", "运动天数");
        GOAL_TYPE_LABELS.put("checkin_days", "连续打卡");
        GOAL_TYPE_LABELS.put("water_target", "饮水目标");
        GOAL_TYPE_LABELS.put("custom", "自定义");
    }

    private final GoalMilestoneMapper goalMilestoneMapper;
    private final SysNotificationMapper sysNotificationMapper;

    public GoalMilestoneServiceImpl(GoalMilestoneMapper goalMilestoneMapper,
                                     SysNotificationMapper sysNotificationMapper) {
        this.goalMilestoneMapper = goalMilestoneMapper;
        this.sysNotificationMapper = sysNotificationMapper;
    }

    @Override
    @Transactional
    public GoalMilestoneVO create(Long userId, GoalMilestoneDTO dto) {
        GoalMilestone goal = new GoalMilestone();
        goal.setUserId(userId);
        goal.setGoalType(dto.getGoalType());
        goal.setGoalName(dto.getGoalName());
        goal.setTargetValue(dto.getTargetValue());
        goal.setCurrentValue(BigDecimal.ZERO);
        goal.setUnit(dto.getUnit() != null ? dto.getUnit() : "");
        goal.setStartDate(dto.getStartDate() != null ? dto.getStartDate() : LocalDate.now());
        goal.setTargetDate(dto.getTargetDate());
        goal.setStatus(0);
        goalMilestoneMapper.insert(goal);

        log.info("创建目标 userId={} goalType={} name={} target={}", userId, dto.getGoalType(), dto.getGoalName(), dto.getTargetValue());
        return toVO(goal);
    }

    @Override
    @Transactional
    public GoalMilestoneVO update(Long userId, GoalMilestoneDTO dto) {
        GoalMilestone goal = goalMilestoneMapper.selectById(dto.getId());
        if (goal == null || !goal.getUserId().equals(userId)) {
            throw new BusinessException("目标不存在");
        }
        goal.setGoalType(dto.getGoalType());
        goal.setGoalName(dto.getGoalName());
        goal.setTargetValue(dto.getTargetValue());
        goal.setUnit(dto.getUnit());
        goal.setStartDate(dto.getStartDate());
        goal.setTargetDate(dto.getTargetDate());
        goalMilestoneMapper.updateById(goal);
        return toVO(goal);
    }

    @Override
    @Transactional
    public void delete(Long userId, Long goalId) {
        GoalMilestone goal = goalMilestoneMapper.selectById(goalId);
        if (goal == null || !goal.getUserId().equals(userId)) {
            throw new BusinessException("目标不存在");
        }
        goalMilestoneMapper.deleteById(goalId);
    }

    @Override
    public List<GoalMilestoneVO> list(Long userId) {
        LambdaQueryWrapper<GoalMilestone> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoalMilestone::getUserId, userId)
                .orderByAsc(GoalMilestone::getStatus)
                .orderByDesc(GoalMilestone::getCreateTime);
        return goalMilestoneMapper.selectList(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public GoalMilestoneVO getById(Long userId, Long goalId) {
        GoalMilestone goal = goalMilestoneMapper.selectById(goalId);
        if (goal == null || !goal.getUserId().equals(userId)) {
            return null;
        }
        return toVO(goal);
    }

    @Override
    @Transactional
    public GoalMilestoneVO updateStatus(Long userId, Long goalId, Integer status) {
        GoalMilestone goal = goalMilestoneMapper.selectById(goalId);
        if (goal == null || !goal.getUserId().equals(userId)) {
            throw new BusinessException("目标不存在");
        }
        goal.setStatus(status);
        if (status == 1) {
            goal.setCompletedDate(LocalDate.now());
            // 达标的自动庆祝通知
            sendAchievementNotification(userId, goal);
        }
        goalMilestoneMapper.updateById(goal);

        log.info("目标状态更新 userId={} goalId={} status={}", userId, goalId, status);
        return toVO(goal);
    }

    @Override
    @Transactional
    public void updateProgress(Long userId, String goalType, BigDecimal newValue) {
        LambdaQueryWrapper<GoalMilestone> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(GoalMilestone::getUserId, userId)
                .eq(GoalMilestone::getGoalType, goalType)
                .eq(GoalMilestone::getStatus, 0);
        List<GoalMilestone> goals = goalMilestoneMapper.selectList(wrapper);

        for (GoalMilestone goal : goals) {
            // 根据类型判断如何更新当前值
            BigDecimal updatedValue = newValue;
            if ("weight_loss".equals(goalType)) {
                // 减重: 当前值 = 起始值 - 最新体重
                updatedValue = goal.getCurrentValue().max(newValue);
            }
            goal.setCurrentValue(updatedValue);

            // 检查是否达标
            if (isTargetAchieved(goal)) {
                goal.setStatus(1);
                goal.setCompletedDate(LocalDate.now());
                sendAchievementNotification(userId, goal);
            }
            goalMilestoneMapper.updateById(goal);
        }
    }

    private boolean isTargetAchieved(GoalMilestone goal) {
        return goal.getCurrentValue().compareTo(goal.getTargetValue()) >= 0;
    }

    private void sendAchievementNotification(Long userId, GoalMilestone goal) {
        try {
            SysNotification notification = new SysNotification();
            notification.setUserId(userId);
            notification.setTitle("目标达成！");
            notification.setContent("恭喜！你的目标「" + goal.getGoalName() + "」已达成！继续加油！");
            notification.setType("achievement");
            notification.setTargetType("goal");
            notification.setTargetId(goal.getId());
            notification.setIsRead(0);
            notification.setCreateTime(LocalDateTime.now());
            sysNotificationMapper.insert(notification);
        } catch (Exception e) {
            log.warn("发送目标达成通知失败 userId={} goalId={}", userId, goal.getId(), e);
        }
    }

    private GoalMilestoneVO toVO(GoalMilestone goal) {
        GoalMilestoneVO vo = new GoalMilestoneVO();
        vo.setId(goal.getId());
        vo.setGoalType(goal.getGoalType());
        vo.setGoalTypeLabel(GOAL_TYPE_LABELS.getOrDefault(goal.getGoalType(), goal.getGoalType()));
        vo.setGoalName(goal.getGoalName());
        vo.setTargetValue(goal.getTargetValue());
        vo.setCurrentValue(goal.getCurrentValue());
        vo.setUnit(goal.getUnit());
        vo.setStartDate(goal.getStartDate());
        vo.setTargetDate(goal.getTargetDate());
        vo.setStatus(goal.getStatus());
        vo.setCompletedDate(goal.getCompletedDate());
        vo.setCreateTime(goal.getCreateTime());

        // 进度百分比
        if (goal.getTargetValue() != null && goal.getTargetValue().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal progress = goal.getCurrentValue().divide(goal.getTargetValue(), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
            vo.setProgressPercent(Math.min(100, progress.intValue()));
        } else {
            vo.setProgressPercent(0);
        }

        // 状态标签
        vo.setStatusLabel(goal.getStatus() == 0 ? "进行中" : goal.getStatus() == 1 ? "已完成" : "已放弃");

        // 剩余天数
        if (goal.getTargetDate() != null && goal.getStatus() == 0) {
            long remaining = ChronoUnit.DAYS.between(LocalDate.now(), goal.getTargetDate());
            vo.setRemainingDays(Math.max(0, remaining));
        }

        return vo;
    }
}