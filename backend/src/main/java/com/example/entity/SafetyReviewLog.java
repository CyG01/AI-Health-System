package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 安全审查审计日志。
 * 记录每次安全审查的判定结果，用于追踪审查有效性和问题回溯。
 */
@Data
@TableName("safety_review_log")
public class SafetyReviewLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 审查判定：PASS / MODIFY / BLOCK */
    private String verdict;

    /** 风险等级：high / medium / low / none */
    private String riskLevel;

    /** 发现的问题列表（JSON数组） */
    private String issues;

    /** 修改建议（JSON数组） */
    private String suggestions;

    /** 是否由规则引擎兜底执行（LLM不可用时） */
    private Boolean fallbackMode;

    /** 待审查内容摘要（前500字符） */
    private String contentDigest;

    /** 审查耗时（毫秒） */
    private Long latencyMs;

    /** 记录创建时间 */
    private LocalDateTime createdAt;
}