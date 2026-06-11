package com.example.billing;

import com.example.entity.Subscription;
import com.example.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 企业版定制化服务。
 * 支持为企业客户配置团队人数、自定义Token额度和价格。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EnterprisePlanService {

    private final SubscriptionMapper subscriptionMapper;
    private final SubscriptionService subscriptionService;

    /**
     * 激活企业版定制化订阅。
     *
     * @param userId           用户ID
     * @param teamSize         团队人数
     * @param customTokenQuotaM 自定义月度Token额度（百万）
     * @param customPrice      自定义价格（元/月）
     * @param months           订阅月数
     * @param orderNo          订单号
     * @param channel          支付渠道
     * @return 创建的订阅记录
     */
    @Transactional
    public Subscription activateEnterprisePlan(Long userId, int teamSize, int customTokenQuotaM,
                                                BigDecimal customPrice, int months,
                                                String orderNo, String channel) {
        // 取消旧订阅
        Subscription old = subscriptionMapper.findActiveByUserId(userId);
        if (old != null) {
            old.setStatus("cancelled");
            subscriptionMapper.updateById(old);
        }

        Subscription sub = new Subscription();
        sub.setUserId(userId);
        sub.setTier("enterprise");
        sub.setStatus("active");
        sub.setStartTime(LocalDateTime.now());
        sub.setEndTime(LocalDateTime.now().plusMonths(months));
        sub.setAutoRenew(true);
        sub.setOrderNo(orderNo);
        sub.setPaymentChannel(channel);

        // 企业版定制化配置
        sub.setTeamSize(teamSize);
        sub.setCustomTokenQuotaM(customTokenQuotaM);
        sub.setCustomPrice(customPrice);

        sub.setCreatedAt(LocalDateTime.now());
        subscriptionMapper.insert(sub);
        subscriptionService.evictTierCache(userId);

        log.info("企业版定制化订阅激活 userId={} teamSize={} tokenQuota={}M price={} months={}",
                userId, teamSize, customTokenQuotaM, customPrice, months);
        return sub;
    }

    /**
     * 更新企业版配置（团队人数、Token额度、价格）。
     * 次月生效。
     */
    @Transactional
    public Subscription updateEnterpriseConfig(Long userId, Integer teamSize,
                                                Integer customTokenQuotaM, BigDecimal customPrice) {
        Subscription sub = subscriptionMapper.findActiveByUserId(userId);
        if (sub == null || !"enterprise".equals(sub.getTier())) {
            throw new IllegalArgumentException("用户无生效中的企业版订阅");
        }

        if (teamSize != null) sub.setTeamSize(teamSize);
        if (customTokenQuotaM != null) sub.setCustomTokenQuotaM(customTokenQuotaM);
        if (customPrice != null) sub.setCustomPrice(customPrice);

        subscriptionMapper.updateById(sub);
        subscriptionService.evictTierCache(userId);

        log.info("企业版配置已更新 userId={} teamSize={} tokenQuota={}M price={}",
                userId, teamSize, customTokenQuotaM, customPrice);
        return sub;
    }

    /**
     * 获取企业版当前配置。
     */
    public Subscription getEnterpriseConfig(Long userId) {
        Subscription sub = subscriptionMapper.findActiveByUserId(userId);
        if (sub == null || !"enterprise".equals(sub.getTier())) {
            return null;
        }
        return sub;
    }
}