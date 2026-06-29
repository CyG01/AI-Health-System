package com.example.llmops;

import com.example.resilience.ModelHealthChecker;
import com.example.resilience.OnlineSafetyCircuitBreaker;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Prometheus 标准指标导出器。
 *
 * 导出指标：
 * - ai_call_total{call_type,model,status} — 调用次数
 * - ai_token_consumption_total{model,type} — Token消耗
 * - ai_call_latency_seconds{call_type} — 延迟分布
 * - ai_call_latency_p95_seconds — P95延迟
 * - ai_call_latency_p99_seconds — P99延迟
 * - ai_circuit_breaker_state — 熔断器状态
 * - ai_average_safety_score — 滑动窗口平均安全分
 * - ai_model_success_rate — 各模型成功率
 * - llm_total_cost_daily — 当日LLM总成本（Phase 4）
 * - llm_active_users_daily — 当日活跃用户数（Phase 4）
 * - llm_over_budget_users — 超预算用户数（Phase 4）
 */
@Component
public class PrometheusMetricsExporter {

    /** P95/P99 延迟滑动窗口大小 */
    private static final int LATENCY_WINDOW_SIZE = 200;

    private final MeterRegistry meterRegistry;
    private final ModelHealthChecker modelHealthChecker;

    /** AI调用总次数 */
    public final Counter aiCallCounter;

    /** Token消耗总量 */
    public final Counter tokenConsumptionCounter;

    /** 安全拦截次数 */
    public final Counter safetyBlockCounter;

    /** 熔断触发次数 */
    public final Counter circuitBreakerTripCounter;

    /** AI调用延迟分布 */
    public final Timer aiCallLatencyTimer;

    /** P95/P99 延迟滑动窗口（线程安全） */
    private final ConcurrentLinkedDeque<Long> latencyWindow = new ConcurrentLinkedDeque<>();

    /** 当日LLM总成本（元）— 供Grafana面板使用 */
    private final AtomicLong totalCostDailyNanos = new AtomicLong(0);

    /** 当日活跃用户数 */
    private final AtomicLong activeUsersDaily = new AtomicLong(0);

    /** 当日超预算用户数 */
    private final AtomicLong overBudgetUsers = new AtomicLong(0);

    public PrometheusMetricsExporter(MeterRegistry meterRegistry,
                                      OnlineSafetyCircuitBreaker circuitBreaker,
                                      ModelHealthChecker modelHealthChecker) {
        this.meterRegistry = meterRegistry;
        this.modelHealthChecker = modelHealthChecker;

        // Counter: AI调用次数
        this.aiCallCounter = Counter.builder("ai_call_total")
                .description("AI调用总次数")
                .register(meterRegistry);

        // Counter: Token消耗
        this.tokenConsumptionCounter = Counter.builder("ai_token_consumption_total")
                .description("Token消耗总量")
                .register(meterRegistry);

        // Counter: 安全拦截
        this.safetyBlockCounter = Counter.builder("ai_safety_block_total")
                .description("安全审查拦截次数")
                .register(meterRegistry);

        // Counter: 熔断触发
        this.circuitBreakerTripCounter = Counter.builder("ai_circuit_breaker_trip_total")
                .description("安全熔断器触发总次数")
                .register(meterRegistry);

        // Timer: AI调用延迟
        this.aiCallLatencyTimer = Timer.builder("ai_call_latency_seconds")
                .description("AI调用延迟分布")
                .publishPercentileHistogram(true)
                .register(meterRegistry);

        // Gauge: 熔断器状态
        Gauge.builder("ai_circuit_breaker_state", circuitBreaker,
                        cb -> switch (cb.getCurrentState()) {
                            case CLOSED -> 0;
                            case OPEN -> 1;
                            case HALF_OPEN -> 2;
                        })
                .description("安全熔断器状态：0=CLOSED,1=OPEN,2=HALF_OPEN")
                .register(meterRegistry);

        // Gauge: 平均安全分
        Gauge.builder("ai_average_safety_score", circuitBreaker,
                        OnlineSafetyCircuitBreaker::getAverageSafetyScore)
                .description("30分钟滑动窗口平均安全分")
                .register(meterRegistry);

        // Gauge: P95 延迟
        Gauge.builder("ai_call_latency_p95_seconds", this, PrometheusMetricsExporter::getP95LatencySeconds)
                .description("AI调用P95延迟（秒）")
                .register(meterRegistry);

        // Gauge: P99 延迟
        Gauge.builder("ai_call_latency_p99_seconds", this, PrometheusMetricsExporter::getP99LatencySeconds)
                .description("AI调用P99延迟（秒）")
                .register(meterRegistry);

        // Gauge: 模型成功率（通过回调查询 ModelHealthChecker 获取各模型成功率）
        Gauge.builder("ai_model_success_rate", modelHealthChecker,
                        hc -> {
                            double maxRate = 0;
                            for (String modelId : hc.getModelStatusSummary().keySet()) {
                                double rate = hc.getRecentSuccessRate(modelId);
                                if (rate > maxRate) maxRate = rate;
                            }
                            return maxRate;
                        })
                .description("模型成功率（取各模型最高值）")
                .register(meterRegistry);

        // ===== Phase 4: 成本监控指标 =====

        // Gauge: 当日LLM总成本（元）
        Gauge.builder("llm_total_cost_daily", this,
                        PrometheusMetricsExporter::getTotalCostDaily)
                .description("当日LLM调用总成本（元）")
                .register(meterRegistry);

        // Gauge: 当日活跃用户数
        Gauge.builder("llm_active_users_daily", this,
                        PrometheusMetricsExporter::getActiveUsersDaily)
                .description("当日LLM活跃用户数")
                .register(meterRegistry);

        // Gauge: 超预算用户数
        Gauge.builder("llm_over_budget_users", this,
                        PrometheusMetricsExporter::getOverBudgetUsers)
                .description("当日超预算用户数（>1元/天）")
                .register(meterRegistry);
    }

    /**
     * 记录AI调用（含延迟）。
     */
    public void recordAiCall(String callType, String model, boolean success, long latencyMs) {
        meterRegistry.counter("ai_call_total", "call_type", callType, "model", model,
                "status", success ? "success" : "fail").increment();
        aiCallLatencyTimer.record(latencyMs, TimeUnit.MILLISECONDS);

        // 维护 P95/P99 滑动窗口
        latencyWindow.addLast(latencyMs);
        while (latencyWindow.size() > LATENCY_WINDOW_SIZE) {
            latencyWindow.pollFirst();
        }
    }

    /**
     * 记录Token消耗。
     */
    public void recordTokenConsumption(String model, int inputTokens, int outputTokens) {
        meterRegistry.counter("ai_token_consumption_total", "model", model, "type", "input")
                .increment(inputTokens);
        meterRegistry.counter("ai_token_consumption_total", "model", model, "type", "output")
                .increment(outputTokens);
    }

    /**
     * 记录安全拦截。
     */
    public void recordSafetyBlock() {
        safetyBlockCounter.increment();
    }

    /**
     * 记录熔断触发。
     */
    public void recordCircuitBreakerTrip() {
        circuitBreakerTripCounter.increment();
    }

    /**
     * 获取 P95 延迟（秒）。
     */
    private double getP95LatencySeconds() {
        return computePercentile(0.95) / 1000.0;
    }

    /**
     * 获取 P99 延迟（秒）。
     */
    private double getP99LatencySeconds() {
        return computePercentile(0.99) / 1000.0;
    }

    /**
     * 计算指定百分位的延迟值（毫秒）。
     */
    private double computePercentile(double percentile) {
        if (latencyWindow.isEmpty()) return 0;

        List<Long> sorted = new ArrayList<>(latencyWindow);
        Collections.sort(sorted);

        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        if (index < 0) index = 0;
        if (index >= sorted.size()) index = sorted.size() - 1;

        return sorted.get(index);
    }

    // ===== Phase 4: 成本监控指标更新方法 =====

    private double getTotalCostDaily() {
        return totalCostDailyNanos.get() / 1_000_000_000.0;
    }

    private double getActiveUsersDaily() {
        return activeUsersDaily.get();
    }

    private double getOverBudgetUsers() {
        return overBudgetUsers.get();
    }

    /**
     * 更新当日总成本（由 MultiModelCostMonitor 调用）。
     * @param costNanos 成本（以纳元为单位，精确到小数点后9位）
     */
    public void updateTotalCostDaily(long costNanos) {
        totalCostDailyNanos.addAndGet(costNanos);
    }

    /**
     * 设置当日总成本（纳元）。
     */
    public void setTotalCostDaily(long costNanos) {
        totalCostDailyNanos.set(costNanos);
    }

    /**
     * 更新当日活跃用户数。
     */
    public void setActiveUsersDaily(long count) {
        activeUsersDaily.set(count);
    }

    /**
     * 更新超预算用户数。
     */
    public void setOverBudgetUsers(long count) {
        overBudgetUsers.set(count);
    }

    /**
     * 记录成本到 Prometheus Counter（累积指标，不会被Gauge重置覆盖）。
     */
    public void recordCost(String intent, String modelName, String modelTier, double cost) {
        meterRegistry.counter("llm_cost_total", "intent", intent,
                "model_name", modelName, "model_tier", modelTier).increment((long)(cost * 1_000_000));
    }

    /**
     * 获取 MeterRegistry（供外部组件注册自定义Gauge）。
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
}