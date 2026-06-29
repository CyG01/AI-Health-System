package com.example.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Function Calling 完整调用结果。
 * 包含模型返回的 tool_calls 以及最终文本响应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FunctionCallResult {

    /** 模型最终文本回复 */
    private String content;

    /** 模型要求调用的工具列表（中间步骤） */
    @Builder.Default
    private List<ToolCallRequest> toolCalls = new ArrayList<>();

    /** Token 用量 */
    @Builder.Default
    private int inputTokens = 0;

    @Builder.Default
    private int outputTokens = 0;

    /** 是否完成了工具调用（false=需要继续发送工具结果给模型） */
    @Builder.Default
    private boolean finished = true;

    /** 模型原始 JSON 响应（用于调试） */
    private String rawResponse;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ToolCallRequest {
        /** tool call id（用于关联请求与结果） */
        private String id;

        /** 函数名 */
        private String name;

        /** JSON 参数字符串 */
        private String arguments;

        /** 已解析的参数（JsonNode） */
        private JsonNode argumentsNode;
    }
}