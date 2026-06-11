package com.example.resilience;

import com.example.monitor.DeepSeekCostMonitor;
import com.example.monitor.ModelTier;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模型健康检查器。
 * 监控各模型的成功率、延迟、熔断状态，动态调整路由权重。
 */
@Slf4j
@Component
public class ModelHealthChecker {

    /** 滑动窗口大小 */
    private static final int WINDOW_SIZE = 20;

    /** 降级阈值 */
    private static final double DEGRADED_THRESHOLD = 0.85;
    private static final double UNHEALTHY_THRESHOLD = 0.5;

    /** 各模型的最近 N 次调用记录 */
    private final Map<String, List<CallRecord>> callHistory = new ConcurrentHashMap<>();

    /** 熔断状态 */
    private final Map<String, CircuitState> circuitStates = new ConcurrentHashMap<>();

    /** 熔断打开后的恢复等待时间（ms） */
    private static final long RECOVERY_WAIT_MS = 60_000;

    private final DeepSeekCostMonitor costMonitor;

    public ModelHealthChecker(DeepSeekCostMonitor costMonitor) {
        this.costMonitor = costMonitor;
    }

    /**
     * 模型调用的状态。
     */
    public enum CircuitState {
        CLOSED,      // 正常
        OPEN,        // 熔断
        HALF_OPEN    // 半开（试探恢复）
    }

    @Data
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    public static class CallRecord {
        private boolean success;
        private long latencyMs;
        private LocalDateTime timestamp;
    }

    /**
     * 记录一次模型调用结果。
     */
    public void recordCall(String modelId, boolean success, long latencyMs) {
        callHistory.computeIfAbsent(modelId, k -> new ArrayList<>());
        List<CallRecord> records = callHistory.get(modelId);
        synchronized (records) {
            records.add(new CallRecord(success, latencyMs, LocalDateTime.now()));
            if (records.size() > WINDOW_SIZE * 2) {
                records.subList(0, records.size() - WINDOW_SIZE).clear();
            }
        }
    }

    /**
     * 获取模型的健康状态。
     */
    public String getHealthStatus(String modelId) {
        CircuitState state = circuitStates.getOrDefault(modelId, CircuitState.CLOSED);
        if (state == CircuitState.OPEN) {
            return "unhealthy";
        }

        double successRate = getRecentSuccessRate(modelId);
        if (successRate < UNHEALTHY_THRESHOLD) return "unhealthy";
        if (successRate < DEGRADED_THRESHOLD) return "degraded";
        return "healthy";
    }

    /**
     * 获取最近调用成功率。
     */
    public double getRecentSuccessRate(String modelId) {
        List<CallRecord> records = callHistory.get(modelId);
        if (records == null || records.isEmpty()) return 1.0;

        synchronized (records) {
            int size = Math.min(WINDOW_SIZE, records.size());
            List<CallRecord> window = records.subList(
                    Math.max(0, records.size() - size), records.size());
            long success = window.stream().filter(CallRecord::isSuccess).count();
            return (double) success / size;
        }
    }

    /**
     * 获取平均延迟。
     */
    public long getAverageLatency(String modelId) {
        List<CallRecord> records = callHistory.get(modelId);
        if (records == null || records.isEmpty()) return 0;

        synchronized (records) {
            int size = Math.min(WINDOW_SIZE, records.size());
            List<CallRecord> window = records.subList(
                    Math.max(0, records.size() - size), records.size());
            return (long) window.stream()
                    .mapToLong(CallRecord::getLatencyMs)
                    .average().orElse(0);
        }
    }

    /**
     * 检查是否可以调用（熔断状态判断）。
     */
    public boolean canCall(String modelId) {
        CircuitState state = circuitStates.getOrDefault(modelId, CircuitState.CLOSED);

        return switch (state) {
            case CLOSED -> true;
            case OPEN -> {
                // 检查是否可以进入 HALF_OPEN
                CallRecord lastRecord = getLastRecord(modelId);
                if (lastRecord != null &&
                        System.currentTimeMillis() - lastRecord.getTimestamp()
                                .atZone(java.time.ZoneId.systemDefault())
                                .toInstant().toEpochMilli() > RECOVERY_WAIT_MS) {
                    circuitStates.put(modelId, CircuitState.HALF_OPEN);
                    log.info("模型 {} 进入半开状态，试探恢复", modelId);
                    yield true;
                }
                yield false;
            }
            case HALF_OPEN -> true; // 允许试探调用
        };
    }

    /**
     * 标记调用成功，关闭熔断。
     */
    public void markSuccess(String modelId) {
        CircuitState state = circuitStates.get(modelId);
        if (state == CircuitState.HALF_OPEN) {
            circuitStates.put(modelId, CircuitState.CLOSED);
            log.info("模型 {} 熔断已恢复", modelId);
        }
    }

    /**
     * 标记调用失败，可能触发熔断。
     */
    public void markFailure(String modelId) {
        double successRate = getRecentSuccessRate(modelId);
        if (successRate < UNHEALTHY_THRESHOLD) {
            circuitStates.put(modelId, CircuitState.OPEN);
            log.error("模型 {} 熔断触发！成功率={}", modelId, String.format("%.2f", successRate));
        }
    }

    /**
     * 每30秒刷新模型指标。
     */
    @Scheduled(fixedRate = 30_000)
    public void refreshMetrics() {
        for (String modelId : callHistory.keySet()) {
            double rate = getRecentSuccessRate(modelId);
            long latency = getAverageLatency(modelId);
            String health = getHealthStatus(modelId);
            CircuitState state = circuitStates.getOrDefault(modelId, CircuitState.CLOSED);

            if (rate < 1.0 || state != CircuitState.CLOSED) {
                log.info("模型健康: model={} health={} circuitState={} successRate={} avgLatencyMs={}",
                        modelId, health, state, String.format("%.2f", rate), latency);
            }
        }
    }

    /**
     * 选择最健康的模型。
     */
    public String selectHealthiest(List<String> modelIds) {
        return modelIds.stream()
                .filter(this::canCall)
                .max(Comparator.comparingDouble(this::getRecentSuccessRate))
                .orElse(null);
    }

    private CallRecord getLastRecord(String modelId) {
        List<CallRecord> records = callHistory.get(modelId);
        if (records == null || records.isEmpty()) return null;
        synchronized (records) {
            return records.get(records.size() - 1);
        }
    }

    /**
     * 获取各模型状态汇总。
     */
    public Map<String, String> getModelStatusSummary() {
        Map<String, String> summary = new ConcurrentHashMap<>();
        for (String modelId : callHistory.keySet()) {
            summary.put(modelId, String.format("health=%s circuit=%s rate=%.2f",
                    getHealthStatus(modelId),
                    circuitStates.getOrDefault(modelId, CircuitState.CLOSED),
                    getRecentSuccessRate(modelId)));
        }
        return summary;
    }
}