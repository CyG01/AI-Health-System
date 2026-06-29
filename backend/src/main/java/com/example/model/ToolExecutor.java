package com.example.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 工具执行器函数式接口。
 * 由调用方注入具体的工具调度逻辑（将 toolName + arguments 本地分发到对应 Tool 方法）。
 */
@FunctionalInterface
public interface ToolExecutor {

    /**
     * 执行一个工具调用。
     *
     * @param toolCallId DeepSeek 返回的 tool_call_id（用于关联请求和结果）
     * @param toolName   DeepSeek 要求调用的函数名
     * @param arguments  JSON 参数字符串
     * @return 工具执行结果字符串（将被作为 tool role message 发送给 DeepSeek）
     */
    String execute(String toolCallId, String toolName, String arguments);
}