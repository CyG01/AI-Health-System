package com.example.monitor;

/**
 * 模型分级策略（Phase 2b 升级版）。
 * 按意图复杂度和安全要求选择不同成本层级的模型，控制 Token 消耗。
 *
 * 降级链：LOW → MEDIUM → HIGH → 预设应答
 * 每个 Tier 独立熔断状态，不可用时自动升级到下一级。
 */
public enum ModelTier {

    /**
     * 低优先级：闲聊、简单问候、无风险查询。
     * 走本地 Ollama 模型（成本≈0），零 API 延迟。
     */
    LOW("local-ollama", "chitchat,greeting,simple_query", 0.0),

    /**
     * 中优先级：食物识别、运动记录、基础健康建议。
     * 走千问-Turbo 等低成本云端模型。
     */
    MEDIUM("qwen-turbo", "food_recognize,exercise_log,health_tips,water_record", 0.3),

    /**
     * 高优先级：医疗指标分析、计划生成、安全审查。
     * 走 DeepSeek-Chat 等高质量模型。
     */
    HIGH("deepseek-chat", "plan_generate,safety_check,medical_analysis,sentiment_analysis", 0.5),

    /**
     * 最高优先级：心理危机干预、自杀风险检测。
     * 走最高安全模型 + SafetyReviewAgent 强制审核。
     */
    CRITICAL("deepseek-chat", "crisis,suicide_risk,emergency", 1.0);

    private final String modelName;
    private final String scenarios;
    /** 成本因子（0~1），用于成本估算 */
    private final double costFactor;

    ModelTier(String modelName, String scenarios, double costFactor) {
        this.modelName = modelName;
        this.scenarios = scenarios;
        this.costFactor = costFactor;
    }

    public String getModelName() {
        return modelName;
    }

    public String getScenarios() {
        return scenarios;
    }

    public double getCostFactor() {
        return costFactor;
    }

    /**
     * 根据调用场景返回推荐的模型层级。
     */
    public static ModelTier forScenario(String scenario) {
        if (scenario == null) {
            return MEDIUM;
        }
        // 按优先级从高到低匹配（CRITICAL 优先）
        for (ModelTier tier : new ModelTier[]{CRITICAL, HIGH, MEDIUM, LOW}) {
            if (tier.scenarios.contains(scenario)) {
                return tier;
            }
        }
        return MEDIUM;
    }

    /**
     * 获取降级链中的下一级。
     * LOW → MEDIUM → HIGH → null（null 表示不可降级，应返回预设应答）
     */
    public ModelTier downgrade() {
        return switch (this) {
            case LOW -> MEDIUM;
            case MEDIUM -> HIGH;
            case HIGH -> null;
            case CRITICAL -> HIGH; // CRITICAL 降级到 HIGH 仍需要高质量模型
        };
    }
}