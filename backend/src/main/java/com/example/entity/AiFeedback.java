package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户对AI建议的反馈实体。
 */
@Data
@TableName("ai_feedback")
public class AiFeedback {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 关联的AI响应ID */
    private String aiResponseId;

    /** 评价：useful/useless/incorrect */
    private String rating;

    /** 用户详细反馈 */
    private String comment;

    /** 是否已人工审核 0=未审核 1=已审核 */
    private Integer manualReviewed;

    /** 审核人ID */
    private Long reviewerId;

    /** 审核结果：valid/invalid/duplicate */
    private String reviewResult;

    /** 问题解决时间 */
    private LocalDateTime resolvedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}