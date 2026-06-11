package com.example.service.impl;

import com.example.entity.*;
import com.example.mapper.*;
import com.example.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 场景管理器 — 感知用户当前场景（工作日/周末/出差）并驱动计划适配。
 *
 * 场景检测策略：
 *  - 出差：连续3天无打卡记录 + 无活跃操作 → 自动切换为 travel
 *  - 周末：周六/周日 → weekend
 *  - 工作日：周一至周五 → workday
 */
@Slf4j
@Service
public class ScenarioManager {

    private final UserProfileMapper userProfileMapper;
    private final DailyCheckinMapper dailyCheckinMapper;
    private final SysUserMapper sysUserMapper;
    private final SysNotificationMapper sysNotificationMapper;
    private final AutoPlanAdjustService autoPlanAdjustService;
    private final MemoryService memoryService;

    private static final int TRAVEL_INACTIVE_DAYS = 3;

    public ScenarioManager(UserProfileMapper userProfileMapper,
                           DailyCheckinMapper dailyCheckinMapper,
                           SysUserMapper sysUserMapper,
                           SysNotificationMapper sysNotificationMapper,
                           AutoPlanAdjustService autoPlanAdjustService,
                           MemoryService memoryService) {
        this.userProfileMapper = userProfileMapper;
        this.dailyCheckinMapper = dailyCheckinMapper;
        this.sysUserMapper = sysUserMapper;
        this.sysNotificationMapper = sysNotificationMapper;
        this.autoPlanAdjustService = autoPlanAdjustService;
        this.memoryService = memoryService;
    }

    /**
     * 每日凌晨2点：自动检测用户场景并更新。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void detectAndUpdateScenarios() {
        log.info("开始场景检测...");
        List<SysUser> users = sysUserMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getStatus, 1));

        int changed = 0;
        for (SysUser user : users) {
            try {
                UserProfile profile = userProfileMapper.selectOne(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserProfile>()
                                .eq(UserProfile::getUserId, user.getId()));
                if (profile == null) continue;

                String detectedScenario = detectScenario(user.getId(), profile);
                String currentScenario = profile.getCurrentScenario();

                if (!detectedScenario.equals(currentScenario)) {
                    profile.setCurrentScenario(detectedScenario);
                    profile.setScenarioUpdatedAt(LocalDateTime.now());
                    userProfileMapper.updateById(profile);
                    log.info("场景切换 userId={} {} -> {}", user.getId(), currentScenario, detectedScenario);
                    changed++;

                    // 场景切换后，自动调整计划
                    onScenarioChanged(user.getId(), profile, detectedScenario, currentScenario);
                }
            } catch (Exception e) {
                log.warn("场景检测失败 userId={}", user.getId(), e);
            }
        }
        log.info("场景检测完成 - 切换:{}人", changed);
    }

    /**
     * 检测用户当前场景。
     */
    public String detectScenario(Long userId, UserProfile profile) {
        LocalDate today = LocalDate.now();
        DayOfWeek dayOfWeek = today.getDayOfWeek();

        // 判断周末
        if (dayOfWeek == DayOfWeek.SATURDAY || dayOfWeek == DayOfWeek.SUNDAY) {
            return "weekend";
        }

        // 判断是否为出差状态：连续N天无打卡 + 无活跃操作
        if (isLikelyTraveling(userId)) {
            return "travel";
        }

        // 默认工作日
        return "workday";
    }

    /**
     * 判断用户是否可能在出差。
     */
    private boolean isLikelyTraveling(Long userId) {
        LocalDate today = LocalDate.now();

        // 检查最近3天是否有打卡记录
        Long recentCheckins = dailyCheckinMapper.selectCount(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DailyCheckin>()
                        .eq(DailyCheckin::getUserId, userId)
                        .ge(DailyCheckin::getCheckDate, today.minusDays(TRAVEL_INACTIVE_DAYS))
                        .le(DailyCheckin::getCheckDate, today));
        return recentCheckins == 0;
    }

    /**
     * 场景切换后，触发计划自动适配。
     */
    private void onScenarioChanged(Long userId, UserProfile profile,
                                    String newScenario, String oldScenario) {
        // 记录场景切换记忆
        memoryService.store(userId,
                "用户场景从" + (oldScenario != null ? oldScenario : "无") + "切换为" + newScenario,
                "HEALTH", 3, "SYSTEM_RECORD");

        // 出差场景：推送酒店室内运动方案
        if ("travel".equals(newScenario)) {
            handleTravelScenario(userId, profile);
        }
    }

    /**
     * 出差场景适配：降低运动强度，推荐室内无器械方案。
     */
    private void handleTravelScenario(Long userId, UserProfile profile) {
        log.info("用户进入出差场景 userId={}", userId);

        // 场景切换通知
        try {
            SysNotification notification = new SysNotification();
            notification.setUserId(userId);
            notification.setTitle("出差场景已激活");
            notification.setContent("检测到你近期活动减少，已自动切换为「出差模式」。运动计划将调整为酒店室内无器械方案，无需健身房也能保持锻炼。");
            notification.setType("SCENARIO_SWITCH");
            notification.setTargetType("plan");
            notification.setIsRead(0);
            notification.setCreateTime(LocalDateTime.now());
            sysNotificationMapper.insert(notification);
        } catch (Exception e) {
            log.warn("出差通知推送失败 userId={}", userId, e);
        }

        // TODO: 触发计划调整——生成室内无器械方案
        // 可在后续 Phase 中接入 autoPlanAdjustService 针对 "travel" 场景生成特殊计划
    }

    /**
     * 获取用户的当前场景标签（默认工作日）。
     */
    public String getCurrentScenario(Long userId) {
        UserProfile profile = userProfileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserProfile>()
                        .eq(UserProfile::getUserId, userId));
        if (profile == null || profile.getCurrentScenario() == null) {
            return "workday";
        }
        return profile.getCurrentScenario();
    }
}