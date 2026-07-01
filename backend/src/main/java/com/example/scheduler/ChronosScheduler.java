package com.example.scheduler;

import com.example.entity.SysUser;
import com.example.entity.UserProfile;
import com.example.entity.UserReminderSchedule;
import com.example.mapper.SysUserMapper;
import com.example.mapper.UserProfileMapper;
import com.example.mapper.UserReminderScheduleMapper;
import com.example.mq.MqTopics;
import com.example.util.TimezoneUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 动态时区自适应调度器 (Chronos Scheduler)
 *
 * 核心原理：
 * 1. 每天凌晨计算所有活跃用户在其所在时区的目标提醒时间，转换为服务器时间戳
 * 2. 将提醒任务存入 Redis ZSet，Score 为触发时间戳
 * 3. 后台常驻线程每秒轮询 ZSet，取出到期的任务发送到 RocketMQ
 * 4. 消费端通过 WebSocket 推送给用户
 *
 * 彻底解决跨时区提醒错乱问题
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChronosScheduler {

    /** Redis ZSet Key 前缀 */
    private static final String REMINDER_ZSET_KEY = "chronos:reminder:zset";

    /** 任务数据前缀（用于存储任务详情） */
    private static final String REMINDER_TASK_PREFIX = "chronos:reminder:task:";

    /** 分布式锁 Key */
    private static final String SCHEDULER_LOCK_KEY = "chronos:scheduler:lock";

    /** 锁过期时间（秒） */
    private static final long LOCK_EXPIRE_SECONDS = 300;

    /** 轮询间隔（毫秒） */
    private static final long POLL_INTERVAL_MS = 1000;

    /** 每次批量处理数量 */
    private static final int BATCH_SIZE = 100;

    private final StringRedisTemplate stringRedisTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final UserReminderScheduleMapper reminderScheduleMapper;
    private final ObjectMapper objectMapper;

    /** 调度线程池 */
    private ExecutorService schedulerExecutor;

    /** 运行标志 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    @PostConstruct
    public void init() {
        schedulerExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "chronos-scheduler");
            t.setDaemon(true);
            return t;
        });

        running.set(true);
        schedulerExecutor.submit(this::reminderPollingLoop);

        log.info("Chronos 动态时区调度器已启动");
    }

    @PreDestroy
    public void destroy() {
        running.set(false);
        if (schedulerExecutor != null) {
            schedulerExecutor.shutdown();
            try {
                if (!schedulerExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    schedulerExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                schedulerExecutor.shutdownNow();
            }
        }
        log.info("Chronos 动态时区调度器已停止");
    }

    /**
     * 每天凌晨2点（服务器时间）：重新计算所有活跃用户的提醒任务
     * 考虑用户时区，将用户当地时间转换为服务器时间
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void rebuildDailyReminderSchedule() {
        String lockKey = SCHEDULER_LOCK_KEY + ":rebuild";
        Boolean locked = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);

        if (!Boolean.TRUE.equals(locked)) {
            log.info("其他实例正在重建提醒调度，本实例跳过");
            return;
        }

        try {
            log.info("开始重建每日提醒调度...");
            int totalScheduled = 0;
            int page = 0;
            int pageSize = 500;

            while (true) {
                // 分批获取活跃用户
                List<SysUser> users = sysUserMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SysUser>()
                                .eq(SysUser::getStatus, 1)
                                .eq(SysUser::getNotificationEnabled, 1)
                                .last("LIMIT " + (page * pageSize) + "," + pageSize)
                );

                if (users.isEmpty()) {
                    break;
                }

                for (SysUser user : users) {
                    try {
                        int scheduled = scheduleUserReminders(user);
                        totalScheduled += scheduled;
                    } catch (Exception e) {
                        log.warn("调度用户提醒失败 userId={}", user.getId(), e);
                    }
                }

                page++;
            }

            log.info("每日提醒调度重建完成，共调度 {} 个任务", totalScheduled);
        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    /**
     * 调度单个用户的所有提醒
     */
    public int scheduleUserReminders(SysUser user) {
        // 获取用户时区
        String userTimezone = getUserTimezone(user.getId());

        // 获取用户的提醒配置
        List<UserReminderSchedule> schedules = reminderScheduleMapper.selectByUserId(user.getId());

        // 如果没有自定义配置，使用默认配置
        if (schedules.isEmpty()) {
            schedules = getDefaultReminderSchedules(user);
        }

        int scheduled = 0;
        for (UserReminderSchedule schedule : schedules) {
            if (schedule.getIsEnabled() == null || schedule.getIsEnabled() != 1) {
                continue;
            }

            // 检查今天是否需要提醒（按星期）
            if (!shouldReminderToday(schedule.getRepeatDays(), userTimezone)) {
                continue;
            }

            // 计算服务器时间戳
            long triggerTimestamp = TimezoneUtils.calculateNextReminderTimestamp(
                    schedule.getReminderTime(), userTimezone);

            // 添加到 ZSet
            String taskId = generateTaskId(user.getId(), schedule.getReminderType(), schedule.getReminderTime());
            addReminderTask(taskId, triggerTimestamp, buildTaskPayload(user, schedule));

            scheduled++;
        }

        return scheduled;
    }

    /**
     * 立即添加一个提醒任务
     */
    public void addReminderTask(String taskId, long triggerTimestamp, String payload) {
        // 添加到 ZSet
        stringRedisTemplate.opsForZSet().add(REMINDER_ZSET_KEY, taskId, triggerTimestamp);

        // 存储任务详情
        stringRedisTemplate.opsForValue().set(
                REMINDER_TASK_PREFIX + taskId,
                payload,
                48, TimeUnit.HOURS
        );
    }

    /**
     * 取消提醒任务
     */
    public void cancelReminderTask(String taskId) {
        stringRedisTemplate.opsForZSet().remove(REMINDER_ZSET_KEY, taskId);
        stringRedisTemplate.delete(REMINDER_TASK_PREFIX + taskId);
    }

    /**
     * 提醒轮询主循环
     */
    private void reminderPollingLoop() {
        log.info("提醒轮询循环启动");

        while (running.get()) {
            try {
                processDueReminders();
                Thread.sleep(POLL_INTERVAL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("提醒轮询异常", e);
                try {
                    Thread.sleep(5000); // 出错后等待5秒再继续
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.info("提醒轮询循环已停止");
    }

    /**
     * 处理到期的提醒
     */
    private void processDueReminders() {
        long now = System.currentTimeMillis();

        // 从 ZSet 中取出所有已到期的任务
        Set<String> dueTasks = stringRedisTemplate.opsForZSet()
                .rangeByScore(REMINDER_ZSET_KEY, 0, now, 0, BATCH_SIZE);

        if (dueTasks == null || dueTasks.isEmpty()) {
            return;
        }

        for (String taskId : dueTasks) {
            try {
                // 获取任务详情
                String payload = stringRedisTemplate.opsForValue().get(REMINDER_TASK_PREFIX + taskId);

                if (payload != null) {
                    // 发送到 MQ
                    rocketMQTemplate.convertAndSend(MqTopics.HEALTH_REMINDER, payload);
                    log.debug("发送提醒任务 MQ: {}", taskId);
                }

                // 从 ZSet 中移除
                stringRedisTemplate.opsForZSet().remove(REMINDER_ZSET_KEY, taskId);
                stringRedisTemplate.delete(REMINDER_TASK_PREFIX + taskId);

            } catch (Exception e) {
                log.error("处理提醒任务失败 taskId={}", taskId, e);
            }
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 获取用户时区
     */
    private String getUserTimezone(Long userId) {
        try {
            UserProfile profile = userProfileMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserProfile>()
                            .eq(UserProfile::getUserId, userId)
            );
            if (profile != null && profile.getTimezoneId() != null) {
                return profile.getTimezoneId();
            }
        } catch (Exception e) {
            log.debug("获取用户时区失败 userId={}", userId, e);
        }
        return TimezoneUtils.DEFAULT_TIMEZONE;
    }

    /**
     * 获取默认提醒配置
     */
    private List<UserReminderSchedule> getDefaultReminderSchedules(SysUser user) {
        List<UserReminderSchedule> schedules = new ArrayList<>();

        // 早晨打卡提醒 08:00
        if (user.getNotifyCheckin() != null && user.getNotifyCheckin() == 1) {
            UserReminderSchedule s = new UserReminderSchedule();
            s.setUserId(user.getId());
            s.setReminderType("CHECKIN");
            s.setReminderTime("08:00");
            s.setIsEnabled(1);
            schedules.add(s);
        }

        // 饮水提醒 10:30, 15:00, 19:00
        for (String time : new String[]{"10:30", "15:00", "19:00"}) {
            UserReminderSchedule s = new UserReminderSchedule();
            s.setUserId(user.getId());
            s.setReminderType("WATER");
            s.setReminderTime(time);
            s.setIsEnabled(1);
            schedules.add(s);
        }

        // 晚间运动+睡眠提醒 20:00
        if (user.getNotifyExercise() != null && user.getNotifyExercise() == 1) {
            UserReminderSchedule s = new UserReminderSchedule();
            s.setUserId(user.getId());
            s.setReminderType("EXERCISE");
            s.setReminderTime("20:00");
            s.setIsEnabled(1);
            schedules.add(s);
        }

        return schedules;
    }

    /**
     * 判断今天是否需要提醒
     */
    private boolean shouldReminderToday(String repeatDays, String userTimezone) {
        if (repeatDays == null || repeatDays.isBlank()) {
            return true; // 默认每天
        }

        int userDayOfWeek = TimezoneUtils.getUserLocalDate(userTimezone).getDayOfWeek().getValue();
        String[] days = repeatDays.split(",");
        for (String day : days) {
            try {
                if (Integer.parseInt(day.trim()) == userDayOfWeek) {
                    return true;
                }
            } catch (NumberFormatException e) {
                // 忽略无效格式
            }
        }
        return false;
    }

    /**
     * 生成任务ID
     */
    private String generateTaskId(Long userId, String reminderType, String time) {
        return userId + ":" + reminderType + ":" + time + ":" + System.currentTimeMillis();
    }

    /**
     * 构建任务 Payload
     */
    private String buildTaskPayload(SysUser user, UserReminderSchedule schedule) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", user.getId());
            payload.put("reminderType", schedule.getReminderType());
            payload.put("reminderTime", schedule.getReminderTime());
            payload.put("customMessage", schedule.getCustomMessage());
            payload.put("scheduledAt", LocalDateTime.now().toString());
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("构建任务 payload 失败", e);
            return "{}";
        }
    }
}
