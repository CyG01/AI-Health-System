package com.example.billing;

import com.example.entity.Subscription;
import com.example.mapper.SubscriptionMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 订阅管理服务。
 * 处理订阅激活、续费、取消、等级校验。
 */
@Slf4j
@Service
public class SubscriptionService {

    private static final String TIER_CACHE_PREFIX = "subscription:tier:";
    private static final Duration TIER_CACHE_TTL = Duration.ofMinutes(10);

    private final SubscriptionMapper subscriptionMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public SubscriptionService(SubscriptionMapper subscriptionMapper,
                                RedisTemplate<String, Object> redisTemplate) {
        this.subscriptionMapper = subscriptionMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取用户当前活跃订阅。
     */
    public Subscription getActiveSubscription(Long userId) {
        return subscriptionMapper.findActiveByUserId(userId);
    }

    /**
     * 获取用户订阅等级，带 Redis 缓存，未订阅返回 free。
     */
    public String getUserTier(Long userId) {
        String cacheKey = TIER_CACHE_PREFIX + userId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached.toString();
        }

        Subscription sub = getActiveSubscription(userId);
        String tier = sub != null && sub.isActive() ? sub.getTier() : "free";

        redisTemplate.opsForValue().set(cacheKey, tier, TIER_CACHE_TTL);
        return tier;
    }

    /**
     * 清除用户订阅等级缓存（订阅变更时调用）。
     */
    public void evictTierCache(Long userId) {
        redisTemplate.delete(TIER_CACHE_PREFIX + userId);
    }

    /**
     * 检查用户是否有权限使用某等级的功能。
     */
    public boolean hasAccess(Long userId, String requiredTier) {
        String userTier = getUserTier(userId);
        return isTierSufficient(userTier, requiredTier);
    }

    /**
     * 激活免费试用（30天）。
     */
    public Subscription activateFreeTrial(Long userId) {
        Subscription existing = getActiveSubscription(userId);
        if (existing != null && !existing.isFree()) {
            log.info("用户已有付费订阅，跳过免费试用 userId={}", userId);
            return existing;
        }

        Subscription sub = new Subscription();
        sub.setUserId(userId);
        sub.setTier("pro");
        sub.setStatus("active");
        sub.setStartTime(LocalDateTime.now());
        sub.setEndTime(LocalDateTime.now().plusDays(30));
        sub.setAutoRenew(false);
        sub.setOrderNo("TRIAL-" + userId + "-" + System.currentTimeMillis());
        sub.setPaymentChannel("free");
        sub.setCreatedAt(LocalDateTime.now());
        subscriptionMapper.insert(sub);
        evictTierCache(userId);

        log.info("免费试用已激活 userId={} expireAt={}", userId, sub.getEndTime());
        return sub;
    }

    /**
     * 激活/续费订阅。
     */
    public Subscription activateSubscription(Long userId, String tier, int days, String orderNo, String channel) {
        // 取消旧订阅
        Subscription old = getActiveSubscription(userId);
        if (old != null) {
            old.setStatus("cancelled");
            subscriptionMapper.updateById(old);
        }

        Subscription sub = new Subscription();
        sub.setUserId(userId);
        sub.setTier(tier);
        sub.setStatus("active");
        sub.setStartTime(LocalDateTime.now());
        sub.setEndTime(LocalDateTime.now().plusDays(days));
        sub.setAutoRenew(true);
        sub.setOrderNo(orderNo);
        sub.setPaymentChannel(channel);
        sub.setCreatedAt(LocalDateTime.now());
        subscriptionMapper.insert(sub);
        evictTierCache(userId);

        log.info("订阅激活成功 userId={} tier={} days={} orderNo={}", userId, tier, days, orderNo);
        return sub;
    }

    /**
     * 取消订阅。
     */
    public void cancelSubscription(Long userId) {
        Subscription sub = getActiveSubscription(userId);
        if (sub != null) {
            sub.setStatus("cancelled");
            sub.setAutoRenew(false);
            subscriptionMapper.updateById(sub);
            evictTierCache(userId);
            log.info("订阅已取消 userId={}", userId);
        }
    }

    /**
     * 检查过期订阅并降级为免费版。
     */
    public void processExpiredSubscriptions() {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getStatus, "active")
                .lt(Subscription::getEndTime, LocalDateTime.now());
        var expired = subscriptionMapper.selectList(wrapper);

        for (Subscription sub : expired) {
            sub.setStatus("expired");
            subscriptionMapper.updateById(sub);

            // 创建免费版订阅
            Subscription free = new Subscription();
            free.setUserId(sub.getUserId());
            free.setTier("free");
            free.setStatus("active");
            free.setStartTime(LocalDateTime.now());
            free.setEndTime(LocalDateTime.now().plusYears(100));
            free.setPaymentChannel("free");
            free.setCreatedAt(LocalDateTime.now());
            subscriptionMapper.insert(free);
            evictTierCache(sub.getUserId());

            log.info("订阅已过期并降级 userId={} oldTier={}", sub.getUserId(), sub.getTier());
        }
    }

    /**
     * 等级比较：enterprise > pro > free
     */
    private boolean isTierSufficient(String userTier, String requiredTier) {
        if ("free".equals(requiredTier)) return true;
        if ("pro".equals(requiredTier)) {
            return "pro".equals(userTier) || "enterprise".equals(userTier);
        }
        if ("enterprise".equals(requiredTier)) {
            return "enterprise".equals(userTier);
        }
        return false;
    }
}