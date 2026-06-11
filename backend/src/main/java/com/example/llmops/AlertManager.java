package com.example.llmops;

import com.example.monitor.DeepSeekCostMonitor;
import com.example.evaluation.EvalMetricsCollector;
import com.example.resilience.ModelHealthChecker;
import com.example.resilience.ModelRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 监控告警管理器。
 *
 * 告警规则：
 * - AI API P99 延迟 > 10s → 高优先级
 * - 熔断器打开 > 5分钟 → 高优先级
 * - 幻觉率 > 10% → 中优先级
 * - 每日 Token 消耗 > 预算 80% → 低优先级
 * - 安全分连续下降 → 高优先级
 */
@Slf4j
@Component
public class AlertManager {

    private static final String ALERT_PREFIX = "alert:";
    private static final String ALERT_HISTORY = "alert:history:";

    /** 告警冷却时间（同一规则5分钟内不重复发送） */
    private static final long COOLDOWN_MS = 300_000;

    // ===== 可配置告警阈值 =====
    @Value("${llmops.alert.cost-warning-threshold:0.80}")
    private double costWarningThreshold;

    @Value("${llmops.alert.cost-critical-threshold:0.95}")
    private double costCriticalThreshold;

    @Value("${llmops.alert.daily-budget:10.00}")
    private String dailyBudget;

    @Value("${llmops.alert.safety-warning-threshold:9.0}")
    private double safetyWarningThreshold;

    @Value("${llmops.alert.safety-critical-threshold:7.5}")
    private double safetyCriticalThreshold;

    private final RedisTemplate<String, Object> redisTemplate;
    private final DeepSeekCostMonitor costMonitor;
    private final EvalMetricsCollector metricsCollector;
    private final ModelHealthChecker healthChecker;
    private final ModelRouter modelRouter;
    private final WebhookNotifier webhookNotifier;

    public AlertManager(RedisTemplate<String, Object> redisTemplate,
                         DeepSeekCostMonitor costMonitor,
                         EvalMetricsCollector metricsCollector,
                         ModelHealthChecker healthChecker,
                         ModelRouter modelRouter,
                         WebhookNotifier webhookNotifier) {
        this.redisTemplate = redisTemplate;
        this.costMonitor = costMonitor;
        this.metricsCollector = metricsCollector;
        this.healthChecker = healthChecker;
        this.modelRouter = modelRouter;
        this.webhookNotifier = webhookNotifier;
    }

    /**
     * 每1分钟检查告警规则。
     */
    @Scheduled(fixedRate = 60_000)
    public void checkAlerts() {
        checkCostAlert();
        checkCircuitBreakerAlert();
        checkSafetyScoreAlert();
        checkModelHealthAlert();
        checkHallucinationAlert();
    }

    /**
     * 成本告警：日预算消耗比例。
     */
    private void checkCostAlert() {
        BigDecimal currentCost = costMonitor.getCurrentDailyCost();
        BigDecimal budget = new BigDecimal(dailyBudget);
        double ratio = currentCost.divide(budget, 2, java.math.RoundingMode.HALF_UP).doubleValue();

        if (ratio >= costCriticalThreshold) {
            fireAlert("cost_critical", "CRITICAL",
                    String.format("日预算已消耗 %.0f%%（%.2f/%s元）", ratio * 100, currentCost, dailyBudget));
        } else if (ratio >= costWarningThreshold) {
            fireAlert("cost_warning", "WARNING",
                    String.format("日预算已消耗 %.0f%%（%.2f/%s元）", ratio * 100, currentCost, dailyBudget));
        }
    }

    /**
     * 熔断器告警。
     */
    private void checkCircuitBreakerAlert() {
        Map<String, String> status = healthChecker.getModelStatusSummary();
        long openCount = status.values().stream()
                .filter(s -> s.contains("circuit=OPEN"))
                .count();

        if (openCount > 0) {
            fireAlert("circuit_breaker", "CRITICAL",
                    String.format("%d个模型处于熔断状态: %s", openCount, status));
        }
    }

    /**
     * 安全分告警。
     */
    private void checkSafetyScoreAlert() {
        double avgSafety = metricsCollector.getAverageSafetyScores();
        if (avgSafety < safetyCriticalThreshold) {
            fireAlert("safety_critical", "CRITICAL",
                    String.format("AI输出安全分过低: %.1f/10", avgSafety));
        } else if (avgSafety < safetyWarningThreshold) {
            fireAlert("safety_warning", "WARNING",
                    String.format("AI输出安全分下降: %.1f/10", avgSafety));
        }
    }

    /**
     * 模型健康告警。
     */
    private void checkModelHealthAlert() {
        for (var entry : healthChecker.getModelStatusSummary().entrySet()) {
            if (entry.getValue().contains("health=unhealthy")) {
                fireAlert("model_unhealthy", "CRITICAL",
                        String.format("模型 %s 不健康: %s", entry.getKey(), entry.getValue()));
            } else if (entry.getValue().contains("health=degraded")) {
                fireAlert("model_degraded", "WARNING",
                        String.format("模型 %s 降级: %s", entry.getKey(), entry.getValue()));
            }
        }
    }

    /**
     * 幻觉率告警：幻觉率超标时自动触发模型降级。
     * 从 Prometheus Gauge 读取 RagasMonitor 实时更新的幻觉率指标。
     */
    private void checkHallucinationAlert() {
        try {
            double hallucinationRate = metricsCollector.getHallucinationRate();
            if (hallucinationRate > 0.05) {
                fireAlert("hallucination_high", "WARNING",
                        String.format("RAG幻觉率过高: %.2f%%，已触发模型自动降级", hallucinationRate * 100));

                // 自动降级当前主力模型权重，流量切换至更稳定的备选模型
                modelRouter.deprioritizeModel("deepseek-v3");
                log.warn("幻觉率超标(%.2f%%) → 已降级deepseek-v3，流量切换至备选模型", hallucinationRate * 100);
            }
        } catch (Exception e) {
            log.debug("检查幻觉率指标失败: {}", e.getMessage());
        }
    }

    /**
     * 触发告警（带冷却时间）。
     */
    private void fireAlert(String ruleName, String severity, String message) {
        String cooldownKey = ALERT_PREFIX + "cooldown:" + ruleName;
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(cooldownKey, "1", java.time.Duration.ofMillis(COOLDOWN_MS));

        if (Boolean.TRUE.equals(locked)) {
            // 记录告警历史
            String historyKey = ALERT_HISTORY + LocalDateTime.now().toString().substring(0, 10);
            Map<String, String> alertInfo = new LinkedHashMap<>();
            alertInfo.put("time", LocalDateTime.now().toString());
            alertInfo.put("rule", ruleName);
            alertInfo.put("severity", severity);
            alertInfo.put("message", message);
            redisTemplate.opsForList().leftPush(historyKey, alertInfo.toString());
            redisTemplate.expire(historyKey, java.time.Duration.ofDays(30));

            // 日志输出
            switch (severity) {
                case "CRITICAL" -> log.error("[LLMOps告警] {} | {} | {}", severity, ruleName, message);
                case "WARNING" -> log.warn("[LLMOps告警] {} | {} | {}", severity, ruleName, message);
                default -> log.info("[LLMOps告警] {} | {} | {}", severity, ruleName, message);
            }

            // 发送 Webhook 通知（异步）
            try {
                webhookNotifier.sendAlert(severity, ruleName, message);
            } catch (Exception e) {
                log.warn("Webhook通知发送失败: {}", e.getMessage());
            }
        }
    }

    /**
     * 获取最近的告警历史。
     */
    public java.util.List<String> getRecentAlerts(int limit) {
        String historyKey = ALERT_HISTORY + LocalDateTime.now().toString().substring(0, 10);
        java.util.List<Object> alerts = redisTemplate.opsForList().range(historyKey, 0, limit - 1);
        if (alerts == null) return java.util.List.of();
        return alerts.stream().map(Object::toString).toList();
    }

    /**
     * 手动触发告警（用于测试）。
     */
    public void testAlert(String message) {
        fireAlert("test", "INFO", message);
    }

    /**
     * 发送 P0 级别告警。
     */
    public void sendP0Alert(String title, String message) {
        fireAlert("p0_" + title.replaceAll("\\s+", "_"), "CRITICAL",
                title + " | " + message);
    }

    /**
     * 发送 Info 级别通知。
     */
    public void sendInfoAlert(String title, String message) {
        fireAlert("info_" + title.replaceAll("\\s+", "_"), "INFO",
                title + " | " + message);
    }

    /**
     * 发送 Warning 级别告警。
     */
    public void sendWarningAlert(String title, String message) {
        fireAlert("warn_" + title.replaceAll("\\s+", "_"), "WARNING",
                title + " | " + message);
    }
}