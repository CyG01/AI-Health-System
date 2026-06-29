package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户订阅记录。
 * 三级定价：free / pro / enterprise
 */
@Data
@TableName("subscription")
public class Subscription {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 订阅等级：free / pro / enterprise */
    private String tier;

    /** 订阅状态：active / expired / cancelled */
    private String status;

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 是否自动续费 */
    private Boolean autoRenew;

    /** 订单号 */
    private String orderNo;

    /** 支付渠道：wechat / alipay / free */
    private String paymentChannel;

    // ===== 企业版定制化字段 =====

    /** 团队人数（企业版专用） */
    private Integer teamSize;

    /** 自定义月度Token额度（百万token，企业版专用） */
    private Integer customTokenQuotaM;

    /** 自定义价格（元，企业版专用） */
    private java.math.BigDecimal customPrice;

    // ===== 退款相关字段 =====

    /** 退款状态：NONE / PENDING / APPROVED / REJECTED / COMPLETED */
    private String refundStatus;

    /** 退款金额（元） */
    private java.math.BigDecimal refundAmount;

    /** 退款原因 */
    private String refundReason;

    /** 退款完成时间 */
    private LocalDateTime refundTime;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    public boolean isActive() {
        return "active".equals(status)
                && endTime != null
                && endTime.isAfter(LocalDateTime.now());
    }

    public boolean isFree() {
        return "free".equals(tier);
    }

    public boolean isPro() {
        return "pro".equals(tier);
    }

    public boolean isEnterprise() {
        return "enterprise".equals(tier);
    }
}