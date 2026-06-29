package com.example.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.*;
import com.example.mapper.*;
import com.example.service.MemoryService;
import com.example.service.OnboardingService;
import com.example.service.impl.AutoPlanAdjustService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Phase 2.4 主动推送引擎。
 * 四大核心定时推送任务 + 推送频率管控 + 优先级系统。
 *
 * 优先级定义：
 *   P0 安全告警（最高）：检测到禁忌运动，立即替换
 *   P1 重要调整：睡眠不足→降强度，热量超标→调整建议
 *   P2 常规提醒：运动提醒、饮水提醒
 *   P3 鼓励/周报：完成率表扬、周末健康周报
 */
@Slf4j
@Component
public class HealthPushScheduler {

    private static final String LOCK_PREFIX = "lock:phase2:push:";
    private static final int SLEEP_DEEP_MIN_THRESHOLD = 60; // 深度睡眠最低分钟
    private static final double CALORIE_OVER_RATIO = 1.2;   // 热量超标阈值

    private final SysUserMapper sysUserMapper;
    private final DailyCheckinMapper dailyCheckinMapper;
    private final DietRecordMapper dietRecordMapper;
    private final SleepRecordMapper sleepRecordMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final SysNotificationMapper sysNotificationMapper;
    private final AiPlanMapper aiPlanMapper;
    private final MemoryService memoryService;
    private final OnboardingService onboardingService;
    private final PushFrequencyController pushController;
    private final StringRedisTemplate stringRedisTemplate;
    private final AutoPlanAdjustService autoPlanAdjustService;

    public HealthPushScheduler(SysUserMapper sysUserMapper,
                               DailyCheckinMapper dailyCheckinMapper,
                               DietRecordMapper dietRecordMapper,
                               SleepRecordMapper sleepRecordMapper,
                               ExerciseRecordMapper exerciseRecordMapper,
                               SysNotificationMapper sysNotificationMapper,
                               AiPlanMapper aiPlanMapper,
                               MemoryService memoryService,
                               OnboardingService onboardingService,
                               PushFrequencyController pushController,
                               StringRedisTemplate stringRedisTemplate,
                               AutoPlanAdjustService autoPlanAdjustService) {
        this.sysUserMapper = sysUserMapper;
        this.dailyCheckinMapper = dailyCheckinMapper;
        this.dietRecordMapper = dietRecordMapper;
        this.sleepRecordMapper = sleepRecordMapper;
        this.exerciseRecordMapper = exerciseRecordMapper;
        this.sysNotificationMapper = sysNotificationMapper;
        this.aiPlanMapper = aiPlanMapper;
        this.memoryService = memoryService;
        this.onboardingService = onboardingService;
        this.pushController = pushController;
        this.stringRedisTemplate = stringRedisTemplate;
        this.autoPlanAdjustService = autoPlanAdjustService;
    }

    // ============================================================
    // 每日0点：重置推送计数器
    // ============================================================
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetPushCounters() {
        pushController.resetDailyCount();
        log.debug("推送计数器已重置");
    }

    // ============================================================
    // 晨间健康检查（7:00）：睡眠数据检查 + 计划自动调整
    // ============================================================
    @Scheduled(cron = "0 0 7 * * ?")
    public void morningSleepCheck() {
        String lockKey = LOCK_PREFIX + "morning";
        if (!acquireLock(lockKey)) return;

        try {
            log.info("开始晨间睡眠健康检查");
            LocalDate today = LocalDate.now();
            LocalDate yesterday = today.minusDays(1);
            List<SysUser> users = getActiveUsers();
            int adjusted = 0;

            for (SysUser user : users) {
                try {
                    if (checkSleepAndAdjust(user.getId(), yesterday)) adjusted++;
                } catch (Exception e) {
                    log.warn("晨间睡眠检查失败 userId={}", user.getId(), e);
                }
            }
            log.info("晨间睡眠检查完成 - 调整计划:{}人", adjusted);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * P1 重要调整：睡眠不足→自动降强度
     */
    private boolean checkSleepAndAdjust(Long userId, LocalDate yesterday) {
        SleepRecord sleep = sleepRecordMapper.selectOne(
                new LambdaQueryWrapper<SleepRecord>()
                        .eq(SleepRecord::getUserId, userId)
                        .eq(SleepRecord::getRecordDate, yesterday));
        if (sleep == null) return false;

        Integer durationMin = sleep.getDurationMin();
        if (durationMin == null || durationMin >= SLEEP_DEEP_MIN_THRESHOLD * 5) return false;

        // 睡眠不足，推送调整通知
        if (!pushController.canPush(userId, "SLEEP_ADJUST", "P1")) return false;

        String content = "昨晚睡眠时长较短（" + durationMin + "分钟），今天已自动将运动计划调整为低强度拉伸。好好休息也是健康的一部分~";
        sendNotification(userId, "SLEEP_ADJUST", "P1",
                "睡眠不足，计划已调整", content, "plan");

        // 存入记忆
        memoryService.store(userId,
                "睡眠不足（" + durationMin + "分钟），" + yesterday + "自动降低了运动强度",
                "HEALTH", 5, "SYSTEM_RECORD");

        // 实际调整计划（非仅通知）
        autoPlanAdjustService.adjustForInsufficientSleep(userId, durationMin);

        pushController.markPushed(userId, "SLEEP_ADJUST");
        return true;
    }

    // ============================================================
    // 午间热量检查（12:30）：热量超标提醒
    // ============================================================
    @Scheduled(cron = "0 30 12 * * ?")
    public void noonCalorieCheck() {
        String lockKey = LOCK_PREFIX + "noon";
        if (!acquireLock(lockKey)) return;

        try {
            log.info("开始午间热量检查");
            LocalDate today = LocalDate.now();
            List<SysUser> users = getActiveUsers();
            int alerts = 0;

            for (SysUser user : users) {
                try {
                    if (checkCalorieOverflow(user.getId(), today)) alerts++;
                } catch (Exception e) {
                    log.warn("午间热量检查失败 userId={}", user.getId(), e);
                }
            }
            log.info("午间热量检查完成 - 超标提醒:{}人", alerts);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * P1 重要调整：午间热量超标检查
     */
    private boolean checkCalorieOverflow(Long userId, LocalDate today) {
        // 查询今日上午饮食总热量
        var wrapper = new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getUserId, userId)
                .ge(DietRecord::getCreateTime, today.atStartOfDay())
                .le(DietRecord::getCreateTime, today.atTime(12, 30));
        var records = dietRecordMapper.selectList(wrapper);

        int totalCal = records.stream()
                .mapToInt(r -> r.getCaloriesConsumed() != null ? r.getCaloriesConsumed() : 0)
                .sum();
        // 以每日2000大卡为基准，半日应 ≤ 1000
        if (totalCal <= 1000) return false;

        if (!pushController.canPush(userId, "CALORIE_ALERT", "P1")) return false;

        String content = "上午已摄入 " + totalCal + " 大卡，有点多了哦。晚上可以多吃蔬菜，或增加15分钟散步消耗多余热量~";
        sendNotification(userId, "CALORIE_ALERT", "P1",
                "热量摄入提醒", content, "diet");

        memoryService.store(userId,
                "今天上午摄入热量" + totalCal + "大卡，已超过半日预算",
                "HEALTH", 4, "SYSTEM_RECORD");

        // 严重超标（>2000大卡）则触发自动饮食计划调整
        if (totalCal > 2000) {
            autoPlanAdjustService.adjustForCalorieOverflow(userId, totalCal);
        }

        pushController.markPushed(userId, "CALORIE_ALERT");
        return true;
    }

    // ============================================================
    // 晚间运动兜底方案（20:00）：未运动则推荐15分钟居家轻运动
    // ============================================================
    @Scheduled(cron = "0 0 20 * * ?")
    public void eveningExerciseFallback() {
        String lockKey = LOCK_PREFIX + "evening";
        if (!acquireLock(lockKey)) return;

        try {
            log.info("开始晚间运动兜底检查");
            LocalDate today = LocalDate.now();
            List<SysUser> users = getActiveUsers();
            int reminded = 0;

            for (SysUser user : users) {
                try {
                    if (remindEveningExercise(user.getId(), today)) reminded++;
                } catch (Exception e) {
                    log.warn("晚间运动检查失败 userId={}", user.getId(), e);
                }
            }
            log.info("晚间运动检查完成 - 提醒:{}人", reminded);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * P2 常规提醒：当日未完成运动，推送15分钟居家方案
     */
    private boolean remindEveningExercise(Long userId, LocalDate today) {
        // 检查今日是否已有运动记录
        Long count = exerciseRecordMapper.selectCount(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, userId)
                        .ge(ExerciseRecord::getCreateTime, today.atStartOfDay()));
        if (count > 0) return false;

        // 检查今日是否已打卡（打卡视为已运动）
        DailyCheckin checkin = dailyCheckinMapper.selectOne(
                new LambdaQueryWrapper<DailyCheckin>()
                        .eq(DailyCheckin::getUserId, userId)
                        .eq(DailyCheckin::getCheckDate, today));
        if (checkin != null) return false;

        if (!pushController.canPush(userId, "EXERCISE_FALLBACK", "P2")) return false;

        String content = "还来得及！为你准备了15分钟居家轻运动方案：开合跳2min + 深蹲15个✕3组 + 平板支撑30s✕3组 + 拉伸5min。动起来吧！";
        sendNotification(userId, "EXERCISE_FALLBACK", "P2",
                "晚间运动提醒", content, "exercise");

        pushController.markPushed(userId, "EXERCISE_FALLBACK");
        return true;
    }

    // ============================================================
    // 周末健康周报（周六 10:00）：生成上周健康总结
    // ============================================================
    @Scheduled(cron = "0 0 10 * * SAT")
    public void weekendHealthReport() {
        String lockKey = LOCK_PREFIX + "report";
        if (!acquireLock(lockKey)) return;

        try {
            log.info("开始生成周末健康周报");
            LocalDate today = LocalDate.now();
            LocalDate weekStart = today.minusDays(7);
            List<SysUser> users = getActiveUsers();
            int reports = 0;

            for (SysUser user : users) {
                try {
                    if (generateWeeklyReport(user.getId(), weekStart, today)) reports++;
                } catch (Exception e) {
                    log.warn("周报生成失败 userId={}", user.getId(), e);
                }
            }
            log.info("周末周报生成完成 - 发送:{}人", reports);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * P3 鼓励/周报：生成并推送上周健康总结
     */
    private boolean generateWeeklyReport(Long userId, LocalDate weekStart, LocalDate weekEnd) {
        if (!pushController.canPush(userId, "WEEKLY_REPORT", "P3")) return false;

        // 收集本周数据
        Long checkinDays = dailyCheckinMapper.selectCount(
                new LambdaQueryWrapper<DailyCheckin>()
                        .eq(DailyCheckin::getUserId, userId)
                        .between(DailyCheckin::getCheckDate, weekStart, weekEnd));
        Long exerciseCount = exerciseRecordMapper.selectCount(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, userId)
                        .between(ExerciseRecord::getCreateTime, weekStart.atStartOfDay(), weekEnd.plusDays(1).atStartOfDay()));
        Long dietRecords = dietRecordMapper.selectCount(
                new LambdaQueryWrapper<DietRecord>()
                        .eq(DietRecord::getUserId, userId)
                        .between(DietRecord::getCreateTime, weekStart.atStartOfDay(), weekEnd.plusDays(1).atStartOfDay()));

        StringBuilder content = new StringBuilder();
        content.append("本周健康总结来啦！\n");
        content.append("打卡天数: ").append(checkinDays).append("/7天\n");
        content.append("运动次数: ").append(exerciseCount).append("次\n");
        content.append("饮食记录: ").append(dietRecords).append("次\n\n");

        // 个性化评价
        if (checkinDays >= 6) {
            content.append("太棒了！你本周几乎每天都坚持了，继续保持这个节奏！");
        } else if (checkinDays >= 3) {
            content.append("不错的一周！下周试试能否打卡5天以上？");
        } else {
            content.append("这周有点松懈了哦，下周重新开始吧，AI教练会一直陪着你的！");
        }

        sendNotification(userId, "WEEKLY_REPORT", "P3",
                "本周健康周报", content.toString(), "report");

        pushController.markPushed(userId, "WEEKLY_REPORT");
        return true;
    }

    // ============================================================
    // 新用户激活策略定时任务
    // ============================================================
    @Scheduled(cron = "0 0 9 * * ?")
    public void onboardingActivation() {
        String lockKey = LOCK_PREFIX + "onboarding";
        if (!acquireLock(lockKey)) return;

        try {
            log.info("开始新用户激活检查");
            List<SysUser> users = getActiveUsers();
            int activated = 0;

            for (SysUser user : users) {
                try {
                    String message = onboardingService.getActivationMessage(user.getId());
                    if (message != null && pushController.canPush(user.getId(), "ONBOARDING", "P2")) {
                        sendNotification(user.getId(), "ONBOARDING", "P2",
                                "AI健康教练", message, "system");
                        pushController.markPushed(user.getId(), "ONBOARDING");
                        activated++;
                    }
                } catch (Exception e) {
                    log.warn("激活推送失败 userId={}", user.getId(), e);
                }
            }
            log.info("新用户激活完成 - 推送:{}人", activated);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    // ============================================================
    // 通用方法
    // ============================================================

    private void sendNotification(Long userId, String pushType, String priority,
                                   String title, String content, String targetType) {
        SysNotification notification = new SysNotification();
        notification.setUserId(userId);
        notification.setTitle("[" + priority + "] " + title);
        notification.setContent(content);
        notification.setType("push");
        notification.setTargetType(targetType);
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        sysNotificationMapper.insert(notification);
    }

    private List<SysUser> getActiveUsers() {
        return sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getStatus, 1));
    }

    private boolean acquireLock(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMinutes(10)));
    }
}