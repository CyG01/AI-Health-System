package com.example.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.*;
import com.example.mapper.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 智能提醒定时调度器
 * 支持：打卡提醒、饮水提醒、运动提醒、睡眠提醒
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CheckinReminderScheduler {

    private static final String REMINDER_LOCK_PREFIX = "lock:reminder:";

    private final SysUserMapper sysUserMapper;
    private final DailyCheckinMapper dailyCheckinMapper;
    private final SysNotificationMapper sysNotificationMapper;
    private final WaterRecordMapper waterRecordMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final SleepRecordMapper sleepRecordMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 每天早上8点：打卡提醒 + 饮水提醒
     */
    @Scheduled(cron = "0 0 8 * * ?")
    public void sendMorningReminders() {
        String lockKey = REMINDER_LOCK_PREFIX + "morning";
        if (!acquireLock(lockKey)) return;

        try {
            log.info("开始执行早晨智能提醒");
            LocalDate today = LocalDate.now();
            List<SysUser> users = getActiveUsers();
            int checkinSent = 0, waterSent = 0;

            for (SysUser user : users) {
                try {
                    // 1. 打卡提醒
                    checkinSent += sendCheckinReminder(user, today) ? 1 : 0;
                    // 2. 饮水提醒
                    waterSent += sendWaterReminder(user, today) ? 1 : 0;
                } catch (Exception e) {
                    log.warn("发送早晨提醒失败 userId={}", user.getId(), e);
                }
            }
            log.info("早晨提醒完成 - 打卡:{}条 饮水:{}条", checkinSent, waterSent);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 上午10:30 / 下午3:00 / 晚上7:00：饮水补充提醒
     */
    @Scheduled(cron = "0 30 10 * * ?")
    public void sendWaterReminder10() { sendWaterRemindersIfNeeded("10:30"); }

    @Scheduled(cron = "0 0 15 * * ?")
    public void sendWaterReminder15() { sendWaterRemindersIfNeeded("15:00"); }

    @Scheduled(cron = "0 0 19 * * ?")
    public void sendWaterReminder19() { sendWaterRemindersIfNeeded("19:00"); }

    private void sendWaterRemindersIfNeeded(String timeSlot) {
        String lockKey = REMINDER_LOCK_PREFIX + "water:" + timeSlot;
        if (!acquireLock(lockKey)) return;

        try {
            LocalDate today = LocalDate.now();
            List<SysUser> users = getActiveUsers();
            int sent = 0;

            for (SysUser user : users) {
                try {
                    if (sendWaterReminder(user, today)) sent++;
                } catch (Exception e) {
                    log.warn("发送饮水提醒失败 userId={}", user.getId(), e);
                }
            }
            log.info("饮水提醒({})完成 发送:{}条", timeSlot, sent);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 每天晚上8点：运动提醒 + 睡眠提醒
     */
    @Scheduled(cron = "0 0 20 * * ?")
    public void sendEveningReminders() {
        String lockKey = REMINDER_LOCK_PREFIX + "evening";
        if (!acquireLock(lockKey)) return;

        try {
            log.info("开始执行晚间智能提醒");
            LocalDate today = LocalDate.now();
            List<SysUser> users = getActiveUsers();
            int exerciseSent = 0, sleepSent = 0;

            for (SysUser user : users) {
                try {
                    exerciseSent += sendExerciseReminder(user, today) ? 1 : 0;
                    sleepSent += sendSleepReminder(user, today) ? 1 : 0;
                } catch (Exception e) {
                    log.warn("发送晚间提醒失败 userId={}", user.getId(), e);
                }
            }
            log.info("晚间提醒完成 - 运动:{}条 睡眠:{}条", exerciseSent, sleepSent);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    // ====================== 各类型提醒逻辑 ======================

    private boolean sendCheckinReminder(SysUser user, LocalDate today) {
        // 已打卡则跳过
        DailyCheckin checkin = dailyCheckinMapper.selectOne(
                new LambdaQueryWrapper<DailyCheckin>()
                        .eq(DailyCheckin::getUserId, user.getId())
                        .eq(DailyCheckin::getCheckDate, today));
        if (checkin != null) return false;

        // 尊重用户设置的提醒时间偏好
        String reminderTime = user.getReminderTime();
        if (reminderTime != null && !reminderTime.isBlank()) {
            LocalTime userTime = LocalTime.parse(reminderTime);
            LocalTime now = LocalTime.now();
            if (Math.abs(now.toSecondOfDay() - userTime.toSecondOfDay()) > 300) {
                return false;
            }
        }

        SysNotification notification = new SysNotification();
        notification.setUserId(user.getId());
        notification.setTitle("今日健康打卡提醒");
        notification.setContent("早上好！别忘了完成今天的健康打卡，记录你的身体状况和运动饮食数据。");
        notification.setType("reminder");
        notification.setTargetType("checkin");
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        sysNotificationMapper.insert(notification);
        return true;
    }

    private boolean sendWaterReminder(SysUser user, LocalDate today) {
        // 检查当日饮水量
        WaterRecord waterRecord = waterRecordMapper.selectOne(
                new LambdaQueryWrapper<WaterRecord>()
                        .eq(WaterRecord::getUserId, user.getId())
                        .eq(WaterRecord::getRecordDate, today));
        int currentAmount = waterRecord != null ? waterRecord.getAmountMl() : 0;

        // 已达标不提醒
        if (currentAmount >= 2000) return false;

        String content;
        if (currentAmount < 500) {
            content = "今天喝水好像还不太够哦，记得多喝水！当前已饮 " + currentAmount + "ml / 建议 2000ml";
        } else if (currentAmount < 1000) {
            content = "喝水量还差一点，再加把劲吧！当前 " + currentAmount + "ml / 建议 2000ml";
        } else {
            content = "不错！还差一点就达标了。当前 " + currentAmount + "ml / 建议 2000ml";
        }

        SysNotification notification = new SysNotification();
        notification.setUserId(user.getId());
        notification.setTitle("饮水提醒");
        notification.setContent(content);
        notification.setType("reminder");
        notification.setTargetType("water");
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        sysNotificationMapper.insert(notification);
        return true;
    }

    private boolean sendExerciseReminder(SysUser user, LocalDate today) {
        // 检查今日是否有运动记录
        Long exerciseCount = exerciseRecordMapper.selectCount(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, user.getId())
                        .ge(ExerciseRecord::getCreateTime, today.atStartOfDay()));
        if (exerciseCount > 0) return false;

        SysNotification notification = new SysNotification();
        notification.setUserId(user.getId());
        notification.setTitle("运动提醒");
        notification.setContent("今天还没有运动记录哦！哪怕是散步30分钟也对健康有益，动起来吧！");
        notification.setType("reminder");
        notification.setTargetType("exercise");
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        sysNotificationMapper.insert(notification);
        return true;
    }

    private boolean sendSleepReminder(SysUser user, LocalDate today) {
        // 检查今日是否已记录睡眠
        SleepRecord sleepRecord = sleepRecordMapper.selectOne(
                new LambdaQueryWrapper<SleepRecord>()
                        .eq(SleepRecord::getUserId, user.getId())
                        .eq(SleepRecord::getRecordDate, today));
        if (sleepRecord != null) return false;

        SysNotification notification = new SysNotification();
        notification.setUserId(user.getId());
        notification.setTitle("睡眠提醒");
        notification.setContent("夜深了，记得按时休息哦！建议23:00前入睡，保证7-8小时充足睡眠。");
        notification.setType("reminder");
        notification.setTargetType("sleep");
        notification.setIsRead(0);
        notification.setCreateTime(LocalDateTime.now());
        sysNotificationMapper.insert(notification);
        return true;
    }

    // ====================== 辅助方法 ======================

    private List<SysUser> getActiveUsers() {
        return sysUserMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getStatus, 1)
                .eq(SysUser::getNotificationEnabled, 1));
    }

    private boolean acquireLock(String key) {
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMinutes(10));
        if (!Boolean.TRUE.equals(locked)) {
            log.info("其他实例正在执行提醒调度 [{}]，本实例跳过", key);
            return false;
        }
        return true;
    }
}