package com.example.llmops;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;

/**
 * LLM 专用度量收集器（Phase 3 全链路追踪补充）。
 *
 * 采集 LLM 调用特有的延迟指标，支撑 Grafana 监控面板：
 * - 首 Token 延迟（TTFT — Time To First Token）
 * - 端到端延迟（E2E Latency）
 * - Token 消耗（Input / Output / Total）
 * - 成本（按模型 × 用户细分）
 */
@Slf4j
@Component
public class LlmMetricsCollector {

    /** 首 Token 延迟滑动窗口 */
    private static final int LATENCY_WINDOW_SIZE = 500;

    private final MeterRegistry meterRegistry;
    private final PrometheusMetricsExporter prometheusExporter;

    // ===== 首 Token 延迟 =====
    private final ConcurrentLinkedDeque<Long> ttftWindow = new ConcurrentLinkedDeque<>();

    // ===== 端到端延迟 =====
    private final ConcurrentLinkedDeque<Long> e2eWindow = new ConcurrentLinkedDeque<>();

    // ===== Token 消耗计数 =====
    private final AtomicLong totalInputTokens = new AtomicLong(0);
    private final AtomicLong totalOutputTokens = new AtomicLong(0);

    // ===== 成本累计 =====
    private final DoubleAdder totalCost = new DoubleAdder();

    // ===== 调用次数 =====
    private final AtomicLong totalCalls = new AtomicLong(0);
    private final AtomicLong failedCalls = new AtomicLong(0);

    public LlmMetricsCollector(MeterRegistry meterRegistry,
                                PrometheusMetricsExporter prometheusExporter) {
        this.meterRegistry = meterRegistry;
        this.prometheusExporter = prometheusExporter;

        registerGauges();
    }

    /**
     * 注册 Prometheus Gauge 指标。
     */
    private void registerGauges() {
        // 首 Token 延迟 P50/P95/P99
        Gauge.builder("llm_ttft_p50_seconds", this, LlmMetricsCollector::getTtftP50Seconds)
                .description("LLM 首 Token 延迟 P50（秒）")
                .register(meterRegistry);

        Gauge.builder("llm_ttft_p95_seconds", this, LlmMetricsCollector::getTtftP95Seconds)
                .description("LLM 首 Token 延迟 P95（秒）")
                .register(meterRegistry);

        Gauge.builder("llm_ttft_p99_seconds", this, LlmMetricsCollector::getTtftP99Seconds)
                .description("LLM 首 Token 延迟 P99（秒）")
                .register(meterRegistry);

        // 端到端延迟 P50/P95/P99
        Gauge.builder("llm_e2e_p50_seconds", this, LlmMetricsCollector::getE2eP50Seconds)
                .description("LLM 端到端延迟 P50（秒）")
                .register(meterRegistry);

        Gauge.builder("llm_e2e_p95_seconds", this, LlmMetricsCollector::getE2eP95Seconds)
                .description("LLM 端到端延迟 P95（秒）")
                .register(meterRegistry);

        Gauge.builder("llm_e2e_p99_seconds", this, LlmMetricsCollector::getE2eP99Seconds)
                .description("LLM 端到端延迟 P99（秒）")
                .register(meterRegistry);

        // Token 总量
        Gauge.builder("llm_input_tokens_total", totalInputTokens, AtomicLong::get)
                .description("LLM 输入 Token 累计")
                .register(meterRegistry);

        Gauge.builder("llm_output_tokens_total", totalOutputTokens, AtomicLong::get)
                .description("LLM 输出 Token 累计")
                .register(meterRegistry);

        Gauge.builder("llm_total_tokens", this,
                        m -> m.totalInputTokens.get() + m.totalOutputTokens.get())
                .description("LLM Token 消耗总量")
                .register(meterRegistry);

        // 成本指标
        Gauge.builder("llm_cost_total", totalCost, DoubleAdder::sum)
                .description("LLM 调用累计成本（元）")
                .register(meterRegistry);

        // 调用次数 & 成功率
        Gauge.builder("llm_call_total", totalCalls, AtomicLong::get)
                .description("LLM 调用总次数")
                .register(meterRegistry);

        Gauge.builder("llm_error_rate", this, m -> {
            long total = m.totalCalls.get();
            return total > 0 ? (double) m.failedCalls.get() / total : 0;
        }).description("LLM 错误率（0~1）")
                .register(meterRegistry);
    }

    // ==================== 数据记录 API ====================

    /**
     * 记录一次完整的 LLM 调用。
     *
     * @param model        模型名称（如 deepseek-v3）
     * @param scenario     调用场景（plan_generate / chat / food_recognize）
     * @param ttftMs       首 Token 延迟（毫秒）
     * @param e2eMs        端到端延迟（毫秒）
     * @param inputTokens  输入 Token 数
     * @param outputTokens 输出 Token 数
     * @param cost         调用成本（元）
     * @param success      是否成功
     */
    public void recordCall(String model, String scenario,
                            long ttftMs, long e2eMs,
                            int inputTokens, int outputTokens,
                            double cost, boolean success) {
        // 首 Token 延迟
        ttftWindow.addLast(ttftMs);
        while (ttftWindow.size() > LATENCY_WINDOW_SIZE) {
            ttftWindow.pollFirst();
        }

        // 端到端延迟
        e2eWindow.addLast(e2eMs);
        while (e2eWindow.size() > LATENCY_WINDOW_SIZE) {
            e2eWindow.pollFirst();
        }

        // Token 累计
        totalInputTokens.addAndGet(inputTokens);
        totalOutputTokens.addAndGet(outputTokens);

        // 成本累计
        totalCost.add(cost);

        // 调用计数
        totalCalls.incrementAndGet();
        if (!success) {
            failedCalls.incrementAndGet();
        }

        // 同步更新 PrometheusExporter
        prometheusExporter.recordAiCall(scenario, model, success, e2eMs);
        prometheusExporter.recordTokenConsumption(model, inputTokens, outputTokens);

        // 按模型+场景的细分指标
        meterRegistry.counter("llm_calls_detail", "model", model,
                "scenario", scenario, "status", success ? "ok" : "fail").increment();
        meterRegistry.counter("llm_tokens_detail", "model", model, "type", "input")
                .increment(inputTokens);
        meterRegistry.counter("llm_tokens_detail", "model", model, "type", "output")
                .increment(outputTokens);

        log.debug("LLM metrics recorded model={} scenario={} ttftMs={} e2eMs={} " +
                        "tokens(in={}, out={}) cost={} success={}",
                model, scenario, ttftMs, e2eMs, inputTokens, outputTokens,
                String.format("%.6f", cost), success);
    }

    /**
     * 简化版记录（不区分首 Token 延迟时使用，TTFT = E2E / 2 估算）。
     */
    public void recordCallSimple(String model, String scenario,
                                  long e2eMs, int inputTokens, int outputTokens,
                                  double cost, boolean success) {
        long ttftEstimate = e2eMs / 2;
        recordCall(model, scenario, ttftEstimate, e2eMs,
                inputTokens, outputTokens, cost, success);
    }

    // ==================== 延迟百分位计算 ====================

    private double getTtftP50Seconds() { return computePercentile(ttftWindow, 0.50) / 1000.0; }
    private double getTtftP95Seconds() { return computePercentile(ttftWindow, 0.95) / 1000.0; }
    private double getTtftP99Seconds() { return computePercentile(ttftWindow, 0.99) / 1000.0; }

    private double getE2eP50Seconds() { return computePercentile(e2eWindow, 0.50) / 1000.0; }
    private double getE2eP95Seconds() { return computePercentile(e2eWindow, 0.95) / 1000.0; }
    private double getE2eP99Seconds() { return computePercentile(e2eWindow, 0.99) / 1000.0; }

    private double computePercentile(ConcurrentLinkedDeque<Long> window, double percentile) {
        if (window.isEmpty()) return 0;

        List<Long> sorted = new ArrayList<>(window);
        Collections.sort(sorted);

        int index = (int) Math.ceil(percentile * sorted.size()) - 1;
        if (index < 0) index = 0;
        if (index >= sorted.size()) index = sorted.size() - 1;

        return sorted.get(index);
    }

    // ==================== 查询 API（供告警规则使用） ====================

    /**
     * 获取 LLM 错误率（0~1）。
     */
    public double getErrorRate() {
        long total = totalCalls.get();
        return total > 0 ? (double) failedCalls.get() / total : 0;
    }

    /**
     * 获取 P99 端到端延迟（秒）。
     */
    public double getP99E2eSeconds() {
        return getE2eP99Seconds();
    }

    /**
     * 获取 P99 首 Token 延迟（秒）。
     */
    public double getP99TtftSeconds() {
        return getTtftP99Seconds();
    }

    /**
     * 获取累计成本。
     */
    public double getTotalCost() {
        return totalCost.sum();
    }

    /**
     * 获取总调用次数。
     */
    public long getTotalCalls() {
        return totalCalls.get();
    }

    /**
     * 获取失败调用次数。
     */
    public long getFailedCalls() {
        return failedCalls.get();
    }
}