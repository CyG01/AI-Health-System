package com.example.billing;

import com.example.entity.Subscription;
import com.example.mapper.SubscriptionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/**
 * 退款服务。
 *
 * 退款规则：
 * - 7天无理由退款：购买后7天内且未使用超量额度 → 全额退款
 * - 按比例退款：已使用超量额度 → 按比例扣除已消费金额后退款
 * - 退款金额 = 实付金额 - 已消费金额（超量部分按超量价格计算）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefundService {

    private final SubscriptionMapper subscriptionMapper;
    private final BillingService billingService;

    /**
     * 申请退款。
     *
     * @param userId   用户ID
     * @param reason   退款原因
     * @return 退款申请结果
     */
    @Transactional
    public Subscription applyRefund(Long userId, String reason) {
        Subscription sub = subscriptionMapper.findActiveByUserId(userId);
        if (sub == null) {
            throw new IllegalArgumentException("无生效中的订阅");
        }
        if ("free".equals(sub.getTier())) {
            throw new IllegalArgumentException("免费版订阅不支持退款");
        }
        if (sub.getRefundStatus() != null && !"NONE".equals(sub.getRefundStatus())) {
            throw new IllegalArgumentException("已有退款申请在处理中，请勿重复提交");
        }

        // 计算应退金额
        BigDecimal refundAmount = calculateRefundAmount(sub);

        sub.setRefundStatus("PENDING");
        sub.setRefundAmount(refundAmount);
        sub.setRefundReason(reason);
        subscriptionMapper.updateById(sub);

        log.info("退款申请已提交 userId={} orderNo={} refundAmount={}", userId, sub.getOrderNo(), refundAmount);
        return sub;
    }

    /**
     * 处理退款（管理员审批）。
     */
    @Transactional
    public Subscription approveRefund(Long userId, boolean approved, String adminRemark) {
        Subscription sub = subscriptionMapper.findActiveByUserId(userId);
        if (sub == null || !"PENDING".equals(sub.getRefundStatus())) {
            throw new IllegalArgumentException("无待处理的退款申请");
        }

        if (approved) {
            sub.setRefundStatus("COMPLETED");
            sub.setRefundTime(LocalDateTime.now());
            sub.setRefundReason((sub.getRefundReason() != null ? sub.getRefundReason() : "")
                    + (adminRemark != null ? " [管理员备注: " + adminRemark + "]" : ""));

            // 将订阅降级为免费版
            sub.setStatus("cancelled");
            subscriptionMapper.updateById(sub);

            // 创建免费版订阅
            Subscription free = new Subscription();
            free.setUserId(userId);
            free.setTier("free");
            free.setStatus("active");
            free.setStartTime(LocalDateTime.now());
            free.setEndTime(LocalDateTime.now().plusYears(100));
            free.setPaymentChannel("free");
            free.setCreatedAt(LocalDateTime.now());
            subscriptionMapper.insert(free);

            log.info("退款已审批通过 userId={} amount={}", userId, sub.getRefundAmount());
        } else {
            sub.setRefundStatus("REJECTED");
            sub.setRefundReason((sub.getRefundReason() != null ? sub.getRefundReason() : "")
                    + (adminRemark != null ? " [拒绝原因: " + adminRemark + "]" : ""));
            subscriptionMapper.updateById(sub);

            log.info("退款申请已驳回 userId={}", userId);
        }

        return sub;
    }

    /**
     * 获取退款状态。
     */
    public Subscription getRefundStatus(Long userId) {
        return subscriptionMapper.findActiveByUserId(userId);
    }

    /**
     * 计算退款金额。
     *
     * 规则：
     * 1. 7天内无超量使用 → 全额退款
     * 2. 超过7天或有超量使用 → 按比例退款
     *    - 退款金额 = 实付金额 × 剩余天数比例 - 已消费超量费用
     */
    private BigDecimal calculateRefundAmount(Subscription sub) {
        BigDecimal paidAmount = getPaidAmount(sub);

        if (paidAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        // 计算从购买到现在的天数
        long daysSincePurchase = ChronoUnit.DAYS.between(sub.getStartTime(), LocalDateTime.now());
        long totalDays = ChronoUnit.DAYS.between(sub.getStartTime(), sub.getEndTime());
        if (totalDays <= 0) totalDays = 30; // 默认30天

        long remainingDays = Math.max(0, totalDays - daysSincePurchase);
        BigDecimal remainingRatio = BigDecimal.valueOf(remainingDays)
                .divide(BigDecimal.valueOf(totalDays), 4, RoundingMode.HALF_UP);

        // 基础退款 = 实付金额 × 剩余天数比例
        BigDecimal baseRefund = paidAmount.multiply(remainingRatio);

        // 扣除已消费的超量费用
        BigDecimal overageCost = billingService.getMonthlyCost(sub.getUserId());

        BigDecimal refund = baseRefund.subtract(overageCost);
        if (refund.compareTo(BigDecimal.ZERO) < 0) {
            refund = BigDecimal.ZERO;
        }

        log.info("退款计算 userId={} paidAmount={} remainingRatio={} overageCost={} refund={}",
                sub.getUserId(), paidAmount, remainingRatio, overageCost, refund);
        return refund.setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 获取实付金额（优先使用自定义价格，否则按标准定价）。
     */
    private BigDecimal getPaidAmount(Subscription sub) {
        if (sub.getCustomPrice() != null && sub.getCustomPrice().compareTo(BigDecimal.ZERO) > 0) {
            return sub.getCustomPrice();
        }
        // 标准定价
        return switch (sub.getTier()) {
            case "pro" -> new BigDecimal("19.00");
            case "enterprise" -> new BigDecimal("99.00");
            default -> BigDecimal.ZERO;
        };
    }
}