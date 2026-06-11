package com.example.resilience;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 模型配置。
 * 定义每个可用模型的参数和权重。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ModelConfig {

    /** 模型唯一标识 */
    private String modelId;

    /** 模型显示名称 */
    private String displayName;

    /** API 端点 */
    private String baseUrl;

    /** API Key */
    private String apiKey;

    /** 模型名称 */
    private String modelName;

    /** 默认权重（0-1，越大越优先） */
    @Builder.Default
    private double weight = 1.0;

    /** 成本系数（相对 deepseek=1.0） */
    @Builder.Default
    private double costFactor = 0.5;

    /** 延迟系数 */
    @Builder.Default
    private double latencyFactor = 2.0;

    /** 最大并发数 */
    @Builder.Default
    private int maxConcurrency = 10;

    /** 是否为主力模型 */
    @Builder.Default
    private boolean primary = false;

    /** 健康状态：healthy / degraded / unhealthy */
    @Builder.Default
    private String healthStatus = "healthy";

    /** 最近 N 次调用的成功率 */
    @Builder.Default
    private double recentSuccessRate = 1.0;

    /** 最近 N 次调用的平均延迟（ms） */
    @Builder.Default
    private long avgLatencyMs = 0;

    /** 适配的场景列表 */
    @Builder.Default
    private List<String> suitableScenarios = List.of();

    /** 动态权重（运行时调整） */
    @Builder.Default
    private double dynamicWeight = 1.0;

    /**
     * 计算动态权重：成功率 × 基础权重 × 成本因子倒数。
     */
    public double calculateDynamicWeight() {
        double successWeight = Math.max(0.1, recentSuccessRate);
        double costWeight = 1.0 / Math.max(0.1, costFactor);
        this.dynamicWeight = successWeight * weight * costWeight;
        return this.dynamicWeight;
    }

    public boolean isHealthy() {
        return "healthy".equals(healthStatus);
    }

    public boolean isDegraded() {
        return "degraded".equals(healthStatus);
    }
}