package com.example.agent.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 路由决策结果。
 * Router Agent 分析用户输入后，决定分派给哪些专家 Agent。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoutingDecision {

    /** 用户原始输入 */
    private String userInput;

    /** 用户情绪标签 */
    @Builder.Default
    private String emotionLabel = "neutral";

    /** 意图分类：coach / nutrition / psychology / mixed */
    private String intent;

    /** 需要调用的 Agent 列表 */
    @Builder.Default
    private List<String> targetAgents = new ArrayList<>();

    /** 是否需要并行调用（多领域时并行，单一领域串行即可） */
    @Builder.Default
    private boolean parallel = false;

    /** 优先级排序（数字越小优先级越高） */
    @Builder.Default
    private List<AgentPriority> priorities = new ArrayList<>();

    /** 是否需要安全审查 */
    @Builder.Default
    private boolean requireSafetyReview = true;

    /** 路由置信度 0.0-1.0 */
    @Builder.Default
    private double confidence = 0.0;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AgentPriority {
        private String agentName;
        private int priority;
    }

    public boolean needsCoach() {
        return targetAgents.contains("coach");
    }

    public boolean needsNutrition() {
        return targetAgents.contains("nutrition");
    }

    public boolean needsPsychology() {
        return targetAgents.contains("psychology");
    }

    public boolean isSingleAgent() {
        return targetAgents.size() == 1;
    }
}