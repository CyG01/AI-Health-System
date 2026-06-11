package com.example.aspect;

import com.example.annotation.RequiresSubscription;
import com.example.billing.SubscriptionService;
import com.example.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 订阅权限校验切面。
 * 拦截 @RequiresSubscription 注解的方法，校验用户订阅等级。
 *
 * 切面执行顺序：限流(1) → 防重(2) → 订阅校验(3) → 管理员校验 → 业务逻辑
 */
@Aspect
@Component
@Order(3)
public class SubscriptionAspect {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionAspect.class);

    private final SubscriptionService subscriptionService;

    public SubscriptionAspect(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @Around("@annotation(requiresSubscription)")
    public Object checkSubscription(ProceedingJoinPoint pjp,
                                      RequiresSubscription requiresSubscription) throws Throwable {
        Long userId = extractUserIdFromRequest();
        if (userId == null) {
            log.warn("无法从请求中提取userId，跳过订阅校验 method={}", pjp.getSignature().getName());
            return pjp.proceed();
        }

        String requiredTier = requiresSubscription.value();
        String feature = requiresSubscription.feature();

        if (!subscriptionService.hasAccess(userId, requiredTier)) {
            String msg = feature != null && !feature.isBlank()
                    ? "\u300c" + feature + "\u300d需要升级到" + getTierDisplay(requiredTier) + "版"
                    : "该功能需要升级到" + getTierDisplay(requiredTier) + "版";
            log.info("订阅权限不足 userId={} required={} feature={}", userId, requiredTier, feature);
            throw new BusinessException(402, msg);
        }

        return pjp.proceed();
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
            case "enterprise" -> "企业";
            default -> "付费";
        };
    }
}