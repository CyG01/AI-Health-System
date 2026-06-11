package com.example.agent.model;

import com.example.sdui.AiAgentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个 Agent 执行结果。
 * 由 Orchestrator 收集并聚合。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentResult {

    /** 来源 Agent 名称 */
    private String agentName;

    /** AI 原始输出文本 */
    private String rawOutput;

    /** 结构化响应（SDUI 协议） */
    private AiAgentResponse response;

    /** 消耗的 input tokens */
    @Builder.Default
    private int inputTokens = 0;

    /** 消耗的 output tokens */
    @Builder.Default
    private int outputTokens = 0;

    /** 响应耗时（毫秒） */
    @Builder.Default
    private long latencyMs = 0;

    /** 是否成功 */
    @Builder.Default
    private boolean success = true;

    /** 错误信息 */
    private String errorMessage;

    /** 是否通过了安全检查 */
    @Builder.Default
    private boolean safetyPassed = true;

    public static AgentResult error(String agentName, String errorMessage) {
        return AgentResult.builder()
                .agentName(agentName)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}