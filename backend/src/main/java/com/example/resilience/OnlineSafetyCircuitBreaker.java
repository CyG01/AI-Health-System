package com.example.resilience;

import com.example.evaluation.EvalResult;
import com.example.evaluation.LLMEvaluator;
import com.example.llmops.AlertManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 独立安全熔断器。
 *
 * 职责：
 * 1. 每次 AI 调用后记录安全评分，判断是否触发熔断
 * 2. 熔断时所有 AI 调用走降级方案（规则引擎）
 * 3. 自动探测恢复
 */
@Slf4j
@Component
public class OnlineSafetyCircuitBreaker {

    private final SafetyCircuitConfig config;
    private final AlertManager alertManager;
    private final ModelRouter modelRouter;

    /** 状态机 */
    private volatile CircuitState currentState = CircuitState.CLOSED;

    /** 滑动窗口：存储最近的安全评分 */
    private final LinkedList<Double> safetyScoreWindow = new LinkedList<>();
    private static final int MAX_WINDOW_SIZE = 200;

    /** 连续失败计数器 */
    private final AtomicInteger consecutiveFails = new AtomicInteger(0);

    /** 熔断开始时间 */
    private volatile long openTime = 0;

    /** 半开状态探测通过计数 */
    private final AtomicInteger halfOpenPassCount = new AtomicInteger(0);

    public OnlineSafetyCircuitBreaker(SafetyCircuitConfig config,
                                       AlertManager alertManager,
                                       ModelRouter modelRouter) {
        this.config = config;
        this.alertManager = alertManager;
        this.modelRouter = modelRouter;
    }

    /**
     * 每次 AI 调用后记录安全评分，判断是否触发熔断。
     */
    public synchronized void recordSafetyScore(double score) {
        // 维护滑动窗口
        safetyScoreWindow.add(score);
        if (safetyScoreWindow.size() > MAX_WINDOW_SIZE) {
            safetyScoreWindow.removeFirst();
        }

        if (score < config.getSafetyThreshold()) {
            int fails = consecutiveFails.incrementAndGet();
            log.warn("安全评分低于阈值: {}, 连续失败次数: {}", score, fails);

            // 触发熔断条件：连续失败N次 或 单次安全分极低
            if (fails >= config.getConsecutiveFailsToOpen() || score < config.getMeltdownThreshold()) {
                tripCircuit(score);
            }
        } else {
            consecutiveFails.set(0);

            // 半开状态：探测通过计数
            if (currentState == CircuitState.HALF_OPEN) {
                int passCount = halfOpenPassCount.incrementAndGet();
                if (passCount >= config.getHalfOpenTestCount()) {
                    closeCircuit();
                }
            }
        }
    }

    /**
     * 检查是否允许 AI 调用（熔断时返回 false，走降级方案）。
     */
    public boolean allowAiCall() {
        if (currentState == CircuitState.CLOSED) {
            return true;
        }

        if (currentState == CircuitState.OPEN) {
            // 检查是否到了探测时间
            long elapsedMs = System.currentTimeMillis() - openTime;
            long intervalMs = (long) config.getHalfOpenTestInterval() * 60 * 1000L;
            if (elapsedMs > intervalMs) {
                currentState = CircuitState.HALF_OPEN;
                halfOpenPassCount.set(0);
                log.info("进入半开状态，开始探测恢复");
                return false; // 第一个探测请求由采样任务触发
            }
            return false;
        }

        // 半开状态：只允许配置数量的探测请求
        return halfOpenPassCount.get() < config.getHalfOpenTestCount();
    }

    /**
     * 触发熔断。
     */
    private void tripCircuit(double currentScore) {
        if (currentState == CircuitState.OPEN) {
            return;
        }

        currentState = CircuitState.OPEN;
        openTime = System.currentTimeMillis();
        consecutiveFails.set(0);
        safetyScoreWindow.clear();

        log.error("安全熔断器触发！当前安全分: {}, 已自动切换至规则引擎降级", currentScore);
        alertManager.sendP0Alert("AI安全熔断触发",
                String.format("近30分钟平均安全分: %.2f, 连续失败次数: %d, 已切换至规则引擎",
                        getAverageSafetyScore(), consecutiveFails.get()));
    }

    /**
     * 恢复正常。
     */
    private void closeCircuit() {
        currentState = CircuitState.CLOSED;
        openTime = 0;
        consecutiveFails.set(0);
        safetyScoreWindow.clear();
        halfOpenPassCount.set(0);

        log.info("安全熔断器恢复正常");
        alertManager.sendInfoAlert("AI安全熔断已恢复", "当前安全分已达标，恢复正常AI调用");
    }

    /**
     * 获取滑动窗口平均安全分。
     */
    public double getAverageSafetyScore() {
        if (safetyScoreWindow.isEmpty()) {
            return 10.0;
        }
        synchronized (safetyScoreWindow) {
            return safetyScoreWindow.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(10.0);
        }
    }

    public CircuitState getCurrentState() {
        return currentState;
    }

    /**
     * 获取当前状态摘要（供 API 查询）。
     */
    public String getStatusSummary() {
        return String.format("state=%s, avgSafety=%.2f, consecutiveFails=%d, openSince=%d",
                currentState, getAverageSafetyScore(), consecutiveFails.get(), openTime);
    }
}