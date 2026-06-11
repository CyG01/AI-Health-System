package com.example.monitor;

/**
 * 模型分级策略。
 * 按场景选择不同成本层级的模型，控制 Token 消耗。
 */
public enum ModelTier {

    /** 重量级：计划生成、安全审查（使用 deepseek-chat） */
    HEAVY("deepseek-chat", "plan_generate,safety_check"),

    /** 轻量级：食物识别、简单问答（可降级为更便宜模型） */
    LIGHT("deepseek-chat", "food_recognize,simple_chat"),

    /** 本地级：情绪分析、关键词提取（本地模型，零 API 成本） */
    LOCAL("local-bert", "sentiment_analysis,keyword_extract");

    private final String modelName;
    private final String scenarios;

    ModelTier(String modelName, String scenarios) {
        this.modelName = modelName;
        this.scenarios = scenarios;
    }

    public String getModelName() {
        return modelName;
    }

    public String getScenarios() {
        return scenarios;
    }

    /**
     * 根据调用场景返回推荐的模型层级。
     */
    public static ModelTier forScenario(String scenario) {
        if (scenario == null) {
            return LIGHT;
        }
        for (ModelTier tier : values()) {
            if (tier.scenarios.contains(scenario)) {
                return tier;
            }
        }
        return LIGHT;
    }
}