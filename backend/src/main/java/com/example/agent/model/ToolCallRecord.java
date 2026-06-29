package com.example.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 单次 Tool 调用记录。
 * 由 ToolCallAspect 拦截 @Tool 方法时自动采集。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallRecord {

    /** Tool 方法名（如 getActivePlan） */
    private String toolName;

    /** Tool 描述（@Tool 注解的 value） */
    private String description;

    /** 调用参数（参数名 → 值，userId 自动脱敏） */
    private Map<String, Object> parameters;

    /** Tool 返回结果摘要（截断至 500 字符） */
    private String resultSummary;

    /** 调用是否成功 */
    @Builder.Default
    private Boolean success = true;

    /** 异常信息（失败时） */
    private String errorMessage;

    /** 调用耗时（毫秒） */
    @Builder.Default
    private long latencyMs = 0;

    /** 调用时间 */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}