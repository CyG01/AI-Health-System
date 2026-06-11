package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理员审批表。敏感操作需经审批后方可执行。
 */
@Data
@TableName("admin_approval")
public class AdminApproval {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 操作发起人ID */
    private Long operatorId;

    /** 操作发起人名称 */
    private String operatorName;

    /** 操作类型 */
    private String actionType;

    /** 操作目标描述 */
    private String targetDescription;

    /** 请求参数JSON */
    private String requestPayload;

    /** 状态: pending / approved / rejected */
    private String status;

    /** 审批人ID */
    private Long approverId;

    /** 审批人名称 */
    private String approverName;

    /** 审批意见 */
    private String approveReason;

    /** 申请时间 */
    private LocalDateTime requestedAt;

    /** 审批时间 */
    private LocalDateTime approvedAt;

    /** 是否已执行: 0未执行 1已执行 */
    private Integer executed;
}