package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 规则建议表。由采样分析自动生成，经管理员审核后可转为 SafetyRule 或 ComplianceRule。
 */
@Data
@TableName("rule_suggestion")
public class RuleSuggestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 建议类型: safety_rule / compliance_rule */
    private String suggestionType;

    /** 规则分类 */
    private String ruleCategory;

    /** 触发关键词或正则 */
    private String triggerPattern;

    /** 动作: block / warning / flag */
    private String action;

    /** 优先级 1-10 */
    private Integer priority;

    /** 建议原因（关联采样数据描述） */
    private String reason;

    /** 来源采样ID列表 */
    private String sourceSampleIds;

    /** 命中次数 */
    private Integer hitCount;

    /** 状态: pending / approved / rejected */
    private String status;

    /** 审核人 */
    private String reviewedBy;

    /** 审核时间 */
    private LocalDateTime reviewedAt;

    /** 创建时间 */
    private LocalDateTime createdAt;
}