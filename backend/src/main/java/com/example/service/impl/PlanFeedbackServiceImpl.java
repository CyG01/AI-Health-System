package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.convert.PlanConvert;
import com.example.dto.PlanFeedbackDTO;
import com.example.entity.AiPlan;
import com.example.entity.AiPlanDetail;
import com.example.entity.AiPlanFeedback;
import com.example.entity.SysNotification;
import com.example.mapper.AiPlanDetailMapper;
import com.example.mapper.AiPlanFeedbackMapper;
import com.example.mapper.AiPlanMapper;
import com.example.mapper.SysNotificationMapper;
import com.example.service.DeepSeekService;
import com.example.service.HealthService;
import com.example.service.PlanFeedbackService;
import com.example.util.PromptSanitizer;
import com.example.vo.HealthRecordVO;
import com.example.vo.PlanFeedbackVO;
import com.example.service.impl.AutoPlanAdjustService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanFeedbackServiceImpl implements PlanFeedbackService {

    private final AiPlanFeedbackMapper aiPlanFeedbackMapper;
    private final AiPlanMapper aiPlanMapper;
    private final AiPlanDetailMapper aiPlanDetailMapper;
    private final SysNotificationMapper sysNotificationMapper;
    private final PlanConvert planConvert;
    private final DeepSeekService deepSeekService;
    private final HealthService healthService;
    private final AutoPlanAdjustService autoPlanAdjustService;

    private static final double AUTO_ADJUST_THRESHOLD = 0.3;   // 完成率 < 30% 触发自动调整
    private static final int AUTO_ADJUST_MIN_DAYS = 3;          // 至少需要 3 天数据

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanFeedbackVO submitFeedback(Long userId, PlanFeedbackDTO dto) {
        AiPlan plan = aiPlanMapper.selectById(dto.getPlanId());
        if (plan == null) {
            throw new BusinessException(404, "计划不存在");
        }
        if (!plan.getUserId().equals(userId)) {
            throw new BusinessException("无权对此计划提交反馈");
        }

        AiPlanFeedback feedback = planConvert.toFeedbackEntity(dto);
        feedback.setUserId(userId);
        aiPlanFeedbackMapper.insert(feedback);

        // 检查是否需要自动调整
        double completionRate = getCompletionRate(dto.getPlanId());
        boolean needAutoAdjust = completionRate < AUTO_ADJUST_THRESHOLD
                && plan.getDurationDays() != null
                && plan.getDurationDays() >= AUTO_ADJUST_MIN_DAYS;

        if (needAutoAdjust) {
            try {
                String aiSuggestion = generateAdjustmentSuggestion(plan, completionRate, dto);
                feedback.setAdjustmentSuggestion(aiSuggestion);
                aiPlanFeedbackMapper.updateById(feedback);

                // 发送通知
                sendAutoAdjustNotification(userId, dto.getPlanId(), completionRate);

                // 如果满意度评分 ≤ 2（不满意）且用户有反馈内容，触发自动计划调整
                if (dto.getSatisfactionScore() != null && dto.getSatisfactionScore() <= 2
                        && dto.getContent() != null && !dto.getContent().isBlank()) {
                    autoPlanAdjustService.adjustForDifficultyFeedback(
                            userId, dto.getContent(), dto.getSatisfactionScore());
                }
            } catch (Exception e) {
                log.warn("AI自动调整建议生成失败 planId={}", dto.getPlanId(), e);
            }
        }

        log.info("提交计划反馈 userId={} planId={} completionRate={}% autoAdjust={}",
                userId, dto.getPlanId(), String.format("%.0f", completionRate * 100), needAutoAdjust);
        return planConvert.toFeedbackVO(feedback);
    }

    private double getCompletionRate(Long planId) {
        LambdaQueryWrapper<AiPlanDetail> wrapper = new LambdaQueryWrapper<AiPlanDetail>()
                .eq(AiPlanDetail::getPlanId, planId);
        List<AiPlanDetail> details = aiPlanDetailMapper.selectList(wrapper);
        if (details.isEmpty()) return 1.0;

        long completed = details.stream().filter(d -> d.getStatus() != null && d.getStatus() == 1).count();
        return (double) completed / details.size();
    }

    private String generateAdjustmentSuggestion(AiPlan plan, double completionRate,
                                                 PlanFeedbackDTO dto) {
        String sanitizedType = PromptSanitizer.sanitize(
                dto.getFeedbackType() != null ? dto.getFeedbackType() : "general");
        String sanitizedContent = PromptSanitizer.sanitize(
                dto.getContent() != null ? dto.getContent() : "");
        String prompt = String.format(
                "用户健康计划完成率仅%.0f%%，反馈类型: %s，反馈内容: %s。计划类型: %s，共%d天。请分析原因并给出3条具体可执行的计划调整建议，每条不超过50字。直接输出建议，不要格式标记。",
                completionRate * 100, sanitizedType, sanitizedContent,
                plan.getPlanType(), plan.getDurationDays()
        );

        try {
            return deepSeekService.chat(prompt);
        } catch (Exception e) {
            log.warn("AI调整建议生成失败", e);
            return "建议：1) 适当降低每日任务强度；2) 从最简单的任务开始建立习惯；3) 设定小而明确的目标提升成就感。";
        }
    }

    private void sendAutoAdjustNotification(Long userId, Long planId, double completionRate) {
        try {
            SysNotification notification = new SysNotification();
            notification.setUserId(userId);
            notification.setTitle("计划完成率偏低");
            notification.setContent(String.format(
                    "您的计划当前完成率仅 %.0f%%，建议调整计划以适应您的节奏。",
                    completionRate * 100
            ));
            notification.setType("plan_adjust");
            notification.setTargetType("plan");
            notification.setTargetId(planId);
            notification.setIsRead(0);
            notification.setCreateTime(LocalDateTime.now());
            sysNotificationMapper.insert(notification);
        } catch (Exception e) {
            log.warn("发送自动调整通知失败 userId={}", userId, e);
        }
    }

    @Override
    public List<PlanFeedbackVO> getFeedbacksByUserIdAndPlanId(Long userId, Long planId) {
        LambdaQueryWrapper<AiPlanFeedback> wrapper = new LambdaQueryWrapper<AiPlanFeedback>()
                .eq(AiPlanFeedback::getUserId, userId)
                .eq(AiPlanFeedback::getPlanId, planId)
                .orderByDesc(AiPlanFeedback::getCreateTime);
        return aiPlanFeedbackMapper.selectList(wrapper).stream()
                .map(planConvert::toFeedbackVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PlanFeedbackVO> getFeedbacksByPlanId(Long planId) {
        LambdaQueryWrapper<AiPlanFeedback> wrapper = new LambdaQueryWrapper<AiPlanFeedback>()
                .eq(AiPlanFeedback::getPlanId, planId)
                .orderByDesc(AiPlanFeedback::getCreateTime);
        return aiPlanFeedbackMapper.selectList(wrapper).stream()
                .map(planConvert::toFeedbackVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PlanFeedbackVO> getFeedbacksByUserId(Long userId) {
        LambdaQueryWrapper<AiPlanFeedback> wrapper = new LambdaQueryWrapper<AiPlanFeedback>()
                .eq(AiPlanFeedback::getUserId, userId)
                .orderByDesc(AiPlanFeedback::getCreateTime);
        return aiPlanFeedbackMapper.selectList(wrapper).stream()
                .map(planConvert::toFeedbackVO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<PlanFeedbackVO> getFeedbacksByPlanIdPage(Long planId, int page, int size) {
        Page<AiPlanFeedback> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AiPlanFeedback> wrapper = new LambdaQueryWrapper<AiPlanFeedback>()
                .eq(AiPlanFeedback::getPlanId, planId)
                .orderByDesc(AiPlanFeedback::getCreateTime);
        Page<AiPlanFeedback> result = aiPlanFeedbackMapper.selectPage(pageParam, wrapper);
        Page<PlanFeedbackVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(planConvert::toFeedbackVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public Page<PlanFeedbackVO> getFeedbacksByUserIdPage(Long userId, int page, int size) {
        Page<AiPlanFeedback> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AiPlanFeedback> wrapper = new LambdaQueryWrapper<AiPlanFeedback>()
                .eq(AiPlanFeedback::getUserId, userId)
                .orderByDesc(AiPlanFeedback::getCreateTime);
        Page<AiPlanFeedback> result = aiPlanFeedbackMapper.selectPage(pageParam, wrapper);
        Page<PlanFeedbackVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(planConvert::toFeedbackVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public Page<PlanFeedbackVO> getAllFeedbacks(int page, int size) {
        Page<AiPlanFeedback> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AiPlanFeedback> wrapper = new LambdaQueryWrapper<AiPlanFeedback>()
                .orderByDesc(AiPlanFeedback::getCreateTime);
        Page<AiPlanFeedback> result = aiPlanFeedbackMapper.selectPage(pageParam, wrapper);
        Page<PlanFeedbackVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(planConvert::toFeedbackVO)
                .collect(Collectors.toList()));
        return voPage;
    }

    @Override
    public PlanFeedbackVO getFeedbackById(Long id) {
        AiPlanFeedback feedback = aiPlanFeedbackMapper.selectById(id);
        if (feedback == null) {
            throw new BusinessException(404, "反馈不存在");
        }
        return planConvert.toFeedbackVO(feedback);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlanFeedbackVO triggerAdjust(Long feedbackId) {
        AiPlanFeedback feedback = aiPlanFeedbackMapper.selectById(feedbackId);
        if (feedback == null) {
            throw new BusinessException(404, "反馈不存在");
        }

        AiPlan originalPlan = aiPlanMapper.selectById(feedback.getPlanId());
        if (originalPlan == null) {
            throw new BusinessException(404, "原计划不存在");
        }

        // 获取用户健康档案用于 AI 调整
        HealthRecordVO healthRecord = healthService.getLatestHealthRecord(originalPlan.getUserId());

        // 构建调整偏好：原计划信息 + 用户反馈意见（经注入防护过滤）
        StringBuilder preference = new StringBuilder();
        if (feedback.getAdjustmentSuggestion() != null && !feedback.getAdjustmentSuggestion().isBlank()) {
            preference.append("AdjustmentSuggestion:")
                    .append(PromptSanitizer.sanitize(feedback.getAdjustmentSuggestion()))
                    .append(".");
        }
        if (feedback.getContent() != null && !feedback.getContent().isBlank()) {
            String safeContent = PromptSanitizer.sanitize(feedback.getContent());
            preference.append("Feedback:")
                    .append(safeContent, 0, Math.min(100, safeContent.length()))
                    .append(".");
        }
        String adjustPreference = !preference.isEmpty() ? preference.toString() : "General adjustment";

        // 调用 AI 生成调整方案
        String newAiContent;
        try {
            newAiContent = deepSeekService.callApi(
                    BigDecimal.valueOf(healthRecord.getHeight()),
                    BigDecimal.valueOf(healthRecord.getWeight()),
                    healthRecord.getGoal(),
                    originalPlan.getDurationDays(),
                    adjustPreference,
                    "",
                    "",
                    "");
        } catch (Exception e) {
            log.error("AI调整计划生成失败 feedbackId={}", feedbackId, e);
            // 降级：使用原内容
            newAiContent = originalPlan.getAiContent();
        }

        // 弃用原计划
        AiPlan updatePlan = new AiPlan();
        updatePlan.setId(originalPlan.getId());
        updatePlan.setStatus(0);
        aiPlanMapper.updateById(updatePlan);

        // 创建新计划
        AiPlan newPlan = new AiPlan();
        newPlan.setUserId(originalPlan.getUserId());
        newPlan.setPlanType(originalPlan.getPlanType());
        newPlan.setPlanName(originalPlan.getPlanName() + "-调整版");
        newPlan.setDurationDays(originalPlan.getDurationDays());
        newPlan.setAiContent(newAiContent);
        newPlan.setStartDate(LocalDate.now());
        newPlan.setStatus(1);
        aiPlanMapper.insert(newPlan);

        feedback.setIsAdjusted(1);
        feedback.setNewPlanId(newPlan.getId());
        aiPlanFeedbackMapper.updateById(feedback);

        log.info("触发计划调整 feedbackId={} originalPlanId={} newPlanId={} aiGenerated={}",
                feedbackId, feedback.getPlanId(), newPlan.getId(), !newAiContent.equals(originalPlan.getAiContent()));
        return planConvert.toFeedbackVO(feedback);
    }
}