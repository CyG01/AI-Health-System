package com.example.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.Subscription;
import com.example.entity.SysNotification;
import com.example.mapper.SubscriptionMapper;
import com.example.mapper.SysNotificationMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订阅续费提醒定时调度器。
 * 在用户订阅到期前 7天/3天/1天 发送站内通知提醒续费。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionReminderScheduler {

    private static final String RENEWAL_LOCK_KEY = "lock:subscription:renewal";

    private final SubscriptionMapper subscriptionMapper;
    private final SysNotificationMapper sysNotificationMapper;
    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 每天上午10点检查即将到期的订阅并发送续费提醒。
     */
    @Scheduled(cron = "0 0 10 * * ?")
    public void sendSubscriptionRenewalReminders() {
        if (!acquireLock(RENEWAL_LOCK_KEY)) return;

        try {
            log.info("开始执行订阅续费提醒检查");
            LocalDateTime now = LocalDateTime.now();
            int[] sent = new int[3]; // [0]=7天, [1]=3天, [2]=1天

            // 查询所有付费的有效订阅
            var wrapper = new LambdaQueryWrapper<Subscription>()
                    .eq(Subscription::getStatus, "active")
                    .ne(Subscription::getTier, "free")
                    .isNotNull(Subscription::getEndTime);
            List<Subscription> activeSubs = subscriptionMapper.selectList(wrapper);

            for (Subscription sub : activeSubs) {
                if (sub.getEndTime() == null) continue;

                long daysUntilExpiry = Duration.between(now, sub.getEndTime()).toDays();
                if (daysUntilExpiry < 0) continue; // 已过期，不发送

                String notificationTitle = null;
                String notificationContent = null;

                if (daysUntilExpiry == 7) {
                    notificationTitle = "订阅即将到期提醒";
                    notificationContent = "您的" + tierLabel(sub.getTier()) + "订阅将在7天后到期，请及时续费以继续享受会员权益。";
                    sent[0]++;
                } else if (daysUntilExpiry == 3) {
                    notificationTitle = "订阅到期紧急提醒";
                    notificationContent = "您的" + tierLabel(sub.getTier()) + "订阅将在3天后到期，续费可享不间断服务。";
                    sent[1]++;
                } else if (daysUntilExpiry == 1) {
                    notificationTitle = "订阅明天到期";
                    notificationContent = "您的" + tierLabel(sub.getTier()) + "订阅明天到期！到期后将降级为免费版，请立即续费。";
                    sent[2]++;
                }

                if (notificationTitle != null) {
                    sendNotification(sub.getUserId(), notificationTitle, notificationContent);
                }
            }
            log.info("续费提醒完成 - 7天:{}条 3天:{}条 1天:{}条", sent[0], sent[1], sent[2]);
        } finally {
            stringRedisTemplate.delete(RENEWAL_LOCK_KEY);
        }
    }

    private void sendNotification(Long userId, String title, String content) {
        try {
            SysNotification notification = new SysNotification();
            notification.setUserId(userId);
            notification.setTitle(title);
            notification.setContent(content);
            notification.setType("subscription");
            notification.setTargetType("subscription");
            notification.setIsRead(0);
            sysNotificationMapper.insert(notification);
            log.info("续费通知已发送 userId={} title={}", userId, title);
        } catch (Exception e) {
            log.error("发送续费通知失败 userId={}", userId, e);
        }
    }

    private boolean acquireLock(String lockKey) {
        Boolean success = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "1", Duration.ofMinutes(30));
        return Boolean.TRUE.equals(success);
    }

    private String tierLabel(String tier) {
        return switch (tier) {
            case "pro" -> "Pro版";
            case "enterprise" -> "企业版";
            default -> tier;
        };
    }
}