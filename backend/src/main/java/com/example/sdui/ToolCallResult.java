package com.example.sdui;

import com.example.agent.model.ToolCallRecord;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tool 调用结果，记录 Function Calling 中 Tool 的执行情况。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ToolCallResult {

    /** Tool 名称 */
    private String toolName;

    /** Tool 描述 */
    private String description;

    /** 调用是否成功 */
    private Boolean success;

    /** 返回消息 */
    private String message;

    /** 调用耗时（毫秒） */
    @Builder.Default
    private long latencyMs = 0;

    /** 附加数据 */
    @Builder.Default
    private Map<String, Object> data = new HashMap<>();

    /**
     * 从 ToolCallRecord 转换（AOP 拦截采集的内部记录 → SDUI 协议）。
     */
    public static ToolCallResult from(ToolCallRecord record) {
        return ToolCallResult.builder()
                .toolName(record.getToolName())
                .description(record.getDescription())
                .success(record.getSuccess())
                .message(record.getSuccess()
                        ? record.getResultSummary()
                        : record.getErrorMessage())
                .latencyMs(record.getLatencyMs())
                .data(record.getParameters() != null
                        ? new HashMap<>(record.getParameters())
                        : new HashMap<>())
                .build();
    }

    /**
     * 批量转换。
     */
    public static List<ToolCallResult> from(List<ToolCallRecord> records) {
        if (records == null || records.isEmpty()) {
            return new ArrayList<>();
        }
        return records.stream().map(ToolCallResult::from).collect(Collectors.toList());
    }
}