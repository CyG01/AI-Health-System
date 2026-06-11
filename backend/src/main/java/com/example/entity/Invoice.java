package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 发票记录。
 */
@Data
@TableName("invoice")
public class Invoice {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 发票编号 */
    private String invoiceNo;

    /** 关联订单号 */
    private String orderNo;

    /** 发票金额（元） */
    private BigDecimal amount;

    /** 发票类型：personal（个人）/ enterprise（企业） */
    private String invoiceType;

    /** 发票抬头 */
    private String invoiceTitle;

    /** 税号（企业发票必填） */
    private String taxNumber;

    /** 发票状态：PENDING / ISSUED / CANCELLED */
    private String status;

    /** 开票日期 */
    private LocalDateTime issueDate;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}