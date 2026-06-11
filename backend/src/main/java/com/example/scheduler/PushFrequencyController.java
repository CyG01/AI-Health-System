package com.example.scheduler;

import com.example.entity.UserMemory;
import com.example.entity.UserProfile;
import com.example.mapper.*;
import com.example.service.MemoryService;
import com.example.service.OnboardingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 推送频率控制器（防骚扰）。
 * 基于用户活跃度动态调整推送上限，支持冷却期和DND时段。
 */
@Slf4j
@Component
public class PushFrequencyController {

    /** 默认免打扰时段：22:00 - 次日 7:00 */
    private static final LocalTime DND_START_DEFAULT = LocalTime.of(22, 0);
    private static final LocalTime DND_END_DEFAULT = LocalTime.of(7, 0);

    /** 同类型推送冷却期：3小时 */
    private static final Duration COOLDOWN_SAME_TYPE = Duration.ofHours(3);

    /** 用户活跃度 → 每日最大推送数 */
    private static final Map<String, Integer> ACTIVITY_MAX_PUSH = Map.of(
            "HIGH", 5,
            "MEDIUM", 3,
            "LOW", 1
    );

    /** 推送类型冷却记录 (userId:pushType → 上次推送时间) */
    private final Map<String, LocalDateTime> lastPushTime = new ConcurrentHashMap<>();

    /** 用户当日已推送计数 (userId → count) */
    private final Map<Long, Integer> todayPushCount = new ConcurrentHashMap<>();

    private final DailyCheckinMapper dailyCheckinMapper;

    public PushFrequencyController(DailyCheckinMapper dailyCheckinMapper) {
        this.dailyCheckinMapper = dailyCheckinMapper;
    }

    /**
     * 判断是否可以推送
     * @return true = 允许推送
     */
    public boolean canPush(Long userId, String pushType, String priority) {
        // P0 安全告警始终允许
        if ("P0".equals(priority)) {
            return true;
        }

        // 免打扰时段检查
        if (isInDndPeriod()) {
            log.debug("当前在免打扰时段，跳过推送 userId={}", userId);
            return false;
        }

        // 冷却期检查
        String cooldownKey = userId + ":" + pushType;
        LocalDateTime lastTime = lastPushTime.get(cooldownKey);
        if (lastTime != null && Duration.between(lastTime, LocalDateTime.now()).compareTo(COOLDOWN_SAME_TYPE) < 0) {
            log.debug("推送类型 {} 在冷却期内，跳过 userId={}", pushType, userId);
            return false;
        }

        // 每日上限检查
        int maxDaily = getDailyMaxPush(userId);
        int current = todayPushCount.getOrDefault(userId, 0);
        if (current >= maxDaily) {
            log.debug("已超过每日推送上限 userId={} max={} current={}", userId, maxDaily, current);
            return false;
        }

        return true;
    }

    /**
     * 标记推送已发送
     */
    public void markPushed(Long userId, String pushType) {
        String cooldownKey = userId + ":" + pushType;
        lastPushTime.put(cooldownKey, LocalDateTime.now());
        todayPushCount.merge(userId, 1, Integer::sum);
    }

    /**
     * 根据用户活跃度计算每日最大推送数
     */
    private int getDailyMaxPush(Long userId) {
        String activity = getUserActivityLevel(userId);
        return ACTIVITY_MAX_PUSH.getOrDefault(activity, 3);
    }

    /**
     * 评估用户活跃度
     * HIGH: 每周≥6天打卡
     * MEDIUM: 每周3-5天打卡
     * LOW: 每周<3天打卡
     */
    private String getUserActivityLevel(Long userId) {
        try {
            LocalDate today = LocalDate.now();
            LocalDate weekAgo = today.minusDays(7);
            var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.DailyCheckin>()
                    .eq(com.example.entity.DailyCheckin::getUserId, userId)
                    .ge(com.example.entity.DailyCheckin::getCheckDate, weekAgo);
            long checkinDays = dailyCheckinMapper.selectCount(wrapper);
            if (checkinDays >= 6) return "HIGH";
            if (checkinDays >= 3) return "MEDIUM";
            return "LOW";
        } catch (Exception e) {
            return "MEDIUM";
        }
    }

    /**
     * 检查是否在免打扰时段
     */
    private boolean isInDndPeriod() {
        LocalTime now = LocalTime.now();
        return now.isAfter(DND_START_DEFAULT) || now.isBefore(DND_END_DEFAULT);
    }

    /**
     * 每日0点重置推送计数（由调度器调用）
     */
    public void resetDailyCount() {
        todayPushCount.clear();
        log.debug("推送计数器已重置");
    }
}