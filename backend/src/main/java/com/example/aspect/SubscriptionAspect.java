package com.example.aspect;

import com.example.annotation.RequiresSubscription;
import com.example.billing.SubscriptionService;
import com.example.common.BusinessException;
import com.example.service.FamilyService;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

/**
 * 订阅权限校验切面。
 * 拦截 @RequiresSubscription 注解的方法，校验用户订阅等级。
 * 支持级联鉴权：个人订阅 → 家庭订阅 → 企业订阅
 *
 * 切面执行顺序：限流(1) → 防重(2) → 订阅校验(3) → 管理员校验 → 业务逻辑
 */
@Aspect
@Component
@Order(3)
public class SubscriptionAspect {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionAspect.class);

    private static final String FAMILY_SUBSCRIPTION_CACHE_PREFIX = "cache:family:sub:";
    private static final long FAMILY_SUB_CACHE_MINUTES = 30;

    private final SubscriptionService subscriptionService;
    private final FamilyService familyService;
    private final StringRedisTemplate stringRedisTemplate;

    public SubscriptionAspect(SubscriptionService subscriptionService,
                               FamilyService familyService,
                               StringRedisTemplate stringRedisTemplate) {
        this.subscriptionService = subscriptionService;
        this.familyService = familyService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Around("@annotation(requiresSubscription)")
    public Object checkSubscription(ProceedingJoinPoint pjp,
                                      RequiresSubscription requiresSubscription) throws Throwable {
        Long userId = extractUserIdFromRequest();
        if (userId == null) {
            log.warn("无法从请求中提取userId，拒绝订阅校验 method={}", pjp.getSignature().getName());
            throw new BusinessException(401, "用户身份验证失败");
        }

        String requiredTier = requiresSubscription.value();
        String feature = requiresSubscription.feature();

        // 1. 先检查个人订阅
        boolean hasAccess = subscriptionService.hasAccess(userId, requiredTier);

        // 2. 个人订阅不满足时，检查家庭/企业级联订阅（家庭版、企业版功能支持共享）
        if (!hasAccess && isFamilyShareableTier(requiredTier)) {
            hasAccess = hasFamilySubscriptionAccessWithCache(userId, requiredTier);
        }

        if (!hasAccess) {
            String msg = feature != null && !feature.isBlank()
                    ? "「" + feature + "」需要升级到" + getTierDisplay(requiredTier) + "版"
                    : "该功能需要升级到" + getTierDisplay(requiredTier) + "版";
            log.info("订阅权限不足 userId={} required={} feature={}", userId, requiredTier, feature);
            throw new BusinessException(402, msg);
        }

        return pjp.proceed();
    }

    /**
     * 检查家庭订阅权限（带Redis缓存，提升切面性能）
     */
    private boolean hasFamilySubscriptionAccessWithCache(Long userId, String requiredTier) {
        String cacheKey = FAMILY_SUBSCRIPTION_CACHE_PREFIX + userId + ":" + requiredTier;

        // 尝试从缓存获取
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return "1".equals(cached);
        }

        // 查询家庭订阅权限
        boolean hasAccess = familyService.hasFamilySubscriptionAccess(userId, requiredTier);

        // 写入缓存（30分钟过期）
        stringRedisTemplate.opsForValue().set(
                cacheKey,
                hasAccess ? "1" : "0",
                FAMILY_SUB_CACHE_MINUTES,
                TimeUnit.MINUTES
        );

        return hasAccess;
    }

    /**
     * 判断该订阅等级是否支持家庭共享
     * 家庭版(family)和企业版(enterprise)支持共享，Pro及以下不支持
     */
    private boolean isFamilyShareableTier(String tier) {
        return "family".equalsIgnoreCase(tier) || "enterprise".equalsIgnoreCase(tier);
    }

    /**
     * 从 HttpServletRequest attribute 中提取 userId。
     * 与项目统一的 @RequestAttribute("userId") 注入方式一致。
     */
    private Long extractUserIdFromRequest() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attributes.getRequest();
            Object userIdAttr = request.getAttribute("userId");
            if (userIdAttr instanceof Long) {
                return (Long) userIdAttr;
            }
        } catch (Exception e) {
            log.debug("无法获取 HttpServletRequest", e);
        }
        return null;
    }

    private String getTierDisplay(String tier) {
        return switch (tier) {
            case "free" -> "免费";
            case "pro" -> "Pro";
            case "family" -> "家庭";
            case "enterprise" -> "企业";
            default -> "付费";
        };
    }
}
