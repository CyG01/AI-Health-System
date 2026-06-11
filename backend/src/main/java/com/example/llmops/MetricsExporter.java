package com.example.llmops;

import com.example.evaluation.EvalMetricsCollector;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.resilience.ModelHealthChecker;
import com.example.resilience.ModelRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 度量导出器 — 供 Prometheus + Grafana 消费。
 *
 * 导出指标：
 * - ai_call_total{call_type,model,status} — 调用次数
 * - ai_call_latency_seconds{call_type} — 延迟分布
 * - ai_token_usage_total{type} — Token 消耗
 * - ai_cost_daily — 每日成本
 * - ai_circuit_breaker_state{model} — 熔断状态
 * - ai_safety_score — 安全评分
 * - ai_model_health{model} — 模型健康度
 */
@Slf4j
@Component
public class MetricsExporter {

    private static final String METRICS_SNAPSHOT_KEY = "llmops:metrics:snapshot";
    private static final long SNAPSHOT_TTL_SECONDS = 120;

    private final DeepSeekCostMonitor costMonitor;
    private final EvalMetricsCollector metricsCollector;
    private final ModelHealthChecker healthChecker;
    private final ModelRouter modelRouter;
    private final RedisTemplate<String, Object> redisTemplate;

    public MetricsExporter(DeepSeekCostMonitor costMonitor,
                            EvalMetricsCollector metricsCollector,
                            ModelHealthChecker healthChecker,
                            ModelRouter modelRouter,
                            RedisTemplate<String, Object> redisTemplate) {
        this.costMonitor = costMonitor;
        this.metricsCollector = metricsCollector;
        this.healthChecker = healthChecker;
        this.modelRouter = modelRouter;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 每30秒导出一次完整的指标快照到 Redis。
     * Prometheus 通过 Actuator endpoint 或 Redis exporter 采集。
     */
    @Scheduled(fixedRate = 30_000)
    public void exportMetrics() {
        Map<String, Object> snapshot = new HashMap<>();

        // 成本指标
        snapshot.put("ai_cost_daily", costMonitor.getCurrentDailyCost().doubleValue());
        snapshot.put("ai_cost_remaining", costMonitor.getRemainingBudget().doubleValue());
        snapshot.put("ai_cost_tier_costs", costMonitor.getAllTierCosts());

        // 评测指标
        snapshot.put("ai_eval_metrics", metricsCollector.getMetricsSummary());
        snapshot.put("ai_safety_score_avg", metricsCollector.getAverageSafetyScores());

        // 模型健康
        snapshot.put("ai_model_health", healthChecker.getModelStatusSummary());
        snapshot.put("ai_model_status", modelRouter.getModelStatus());

        // 写入 Redis 供 Prometheus Redis Exporter 或 Grafana 消费
        try {
            redisTemplate.opsForValue().set(METRICS_SNAPSHOT_KEY, snapshot, SNAPSHOT_TTL_SECONDS, TimeUnit.SECONDS);
            log.debug("Metrics snapshot written to Redis, keys={}", snapshot.size());
        } catch (Exception e) {
            log.warn("Failed to write metrics snapshot to Redis: {}", e.getMessage());
        }
    }

    /**
     * 获取运维面板摘要（供 API 消费）。
     */
    public Map<String, Object> getOpsDashboard() {
        Map<String, Object> dashboard = new HashMap<>();

        // 成本面板
        Map<String, Object> costPanel = new HashMap<>();
        costPanel.put("dailyCost", costMonitor.getCurrentDailyCost().doubleValue());
        costPanel.put("remainingBudget", costMonitor.getRemainingBudget().doubleValue());
        costPanel.put("tierCosts", costMonitor.getAllTierCosts());
        dashboard.put("cost", costPanel);

        // 质量面板
        Map<String, Object> qualityPanel = new HashMap<>();
        qualityPanel.put("metrics", metricsCollector.getMetricsSummary());
        qualityPanel.put("avgSafetyScore", metricsCollector.getAverageSafetyScores());
        qualityPanel.put("criticalFailures", metricsCollector.getCriticalFailureCount());
        dashboard.put("quality", qualityPanel);

        // 模型面板
        dashboard.put("models", modelRouter.getModelStatus());

        // 告警
        Map<String, String> modelHealth = new HashMap<>();
        for (var entry : healthChecker.getModelStatusSummary().entrySet()) {
            modelHealth.put(entry.getKey(), entry.getValue());
        }
        dashboard.put("modelHealth", modelHealth);

        return dashboard;
    }
}