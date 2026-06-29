package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * AI 调用全链路审计日志
 */
@Data
@TableName("ai_call_audit_log")
public class AiCallAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 调用类型：plan_generate / food_recognize / chat / plan_adjust */
    private String callType;

    /** 使用的模型名称 */
    private String modelName;

    /** prompt 模板版本 */
    private Integer promptVersion;

    /** 请求参数（脱敏后） */
    private String requestParams;

    /** 实际使用的 prompt */
    private String promptUsed;

    /** AI 原始响应 */
    private String aiRawResponse;

    /** 解析后结果 */
    private String parsedResult;

    /** 输入 token 数 */
    private Integer inputTokens;

    /** 输出 token 数 */
    private Integer outputTokens;

    /** 响应耗时（毫秒） */
    private Integer latencyMs;

    /** 是否成功 */
    private Boolean success;

    /** 错误信息 */
    private String errorMessage;

    /** 记录创建时间 */
    private LocalDateTime createdAt;
}