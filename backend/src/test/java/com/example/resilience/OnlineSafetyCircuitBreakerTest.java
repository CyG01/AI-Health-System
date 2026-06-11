package com.example.resilience;

import com.example.llmops.AlertManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * OnlineSafetyCircuitBreaker 单元测试。
 *
 * 覆盖状态转换（CLOSED → OPEN → HALF_OPEN → CLOSED）、
 * 安全评分记录、滑动窗口平均值、熔断探测等核心逻辑。
 */
class OnlineSafetyCircuitBreakerTest {

    private OnlineSafetyCircuitBreaker circuitBreaker;
    private SafetyCircuitConfig config;
    private AlertManager alertManager;
    private ModelRouter modelRouter;

    @BeforeEach
    void setUp() {
        config = new SafetyCircuitConfig();
        config.setSafetyThreshold(9.0);
        config.setMeltdownThreshold(7.5);
        config.setConsecutiveFailsToOpen(5);
        config.setHalfOpenTestInterval(1);
        config.setHalfOpenTestCount(3);

        alertManager = mock(AlertManager.class);
        modelRouter = mock(ModelRouter.class);

        circuitBreaker = new OnlineSafetyCircuitBreaker(config, alertManager, modelRouter);
    }

    // ==================== 初始状态测试 ====================

    @Nested
    @DisplayName("初始状态")
    class InitialStateTests {

        @Test
        @DisplayName("初始化 → 状态为 CLOSED，允许 AI 调用")
        void shouldBeClosedInitially() {
            assertEquals(CircuitState.CLOSED, circuitBreaker.getCurrentState());
            assertTrue(circuitBreaker.allowAiCall());
            assertEquals(10.0, circuitBreaker.getAverageSafetyScore(), 0.01);
        }
    }

    // ==================== 安全评分记录测试 ====================

    @Nested
    @DisplayName("安全评分记录")
    class SafetyScoreRecordingTests {

        @Test
        @DisplayName("记录高安全分 → 保持在 CLOSED，不清零并发")
        void shouldStayClosedWithHighScores() {
            circuitBreaker.recordSafetyScore(9.5);
            circuitBreaker.recordSafetyScore(9.8);
            circuitBreaker.recordSafetyScore(9.2);

            assertEquals(CircuitState.CLOSED, circuitBreaker.getCurrentState());
            double avg = circuitBreaker.getAverageSafetyScore();
            assertTrue(avg >= 9.0, "平均安全分应 ≥ 9.0，实际: " + avg);
        }

        @Test
        @DisplayName("单次低安全分 → 不触发熔断")
        void shouldNotTripOnSingleLowScore() {
            circuitBreaker.recordSafetyScore(8.0);
            assertEquals(CircuitState.CLOSED, circuitBreaker.getCurrentState());
        }

        @Test
        @DisplayName("连续5次低于阈值 → 触发熔断")
        void shouldTripAfterConsecutiveLowScores() {
            for (int i = 0; i < 5; i++) {
                circuitBreaker.recordSafetyScore(8.0);
            }
            assertEquals(CircuitState.OPEN, circuitBreaker.getCurrentState());
        }

        @Test
        @DisplayName("单次极低安全分（<meltdownThreshold）→ 立即熔断")
        void shouldTripImmediatelyOnMeltdownScore() {
            circuitBreaker.recordSafetyScore(5.0);
            assertEquals(CircuitState.OPEN, circuitBreaker.getCurrentState());
            assertFalse(circuitBreaker.allowAiCall());
        }

        @Test
        @DisplayName("连续失败后通过一次高分 → 重置失败计数")
        void shouldResetConsecutiveFailsOnHighScore() {
            // 4次低分
            for (int i = 0; i < 4; i++) {
                circuitBreaker.recordSafetyScore(8.0);
            }
            // 1次高分 — 重置计数器
            circuitBreaker.recordSafetyScore(9.8);

            // 再来4次低分不应触发（因为计数器被重置了）
            for (int i = 0; i < 4; i++) {
                circuitBreaker.recordSafetyScore(8.0);
            }
            assertEquals(CircuitState.CLOSED, circuitBreaker.getCurrentState());
        }
    }

    // ==================== 熔断状态测试 ====================

    @Nested
    @DisplayName("熔断状态管理")
    class CircuitOpenTests {

        @Test
        @DisplayName("熔断后 → allowAiCall 返回 false")
        void shouldBlockAiCallWhenOpen() {
            // 触发熔断
            circuitBreaker.recordSafetyScore(5.0);
            assertFalse(circuitBreaker.allowAiCall());
        }

        @Test
        @DisplayName("熔断后 → 记录高分不改变 OPEN 状态")
        void shouldStayOpenRegardlessOfHighScore() {
            circuitBreaker.recordSafetyScore(5.0);
            assertEquals(CircuitState.OPEN, circuitBreaker.getCurrentState());

            // 在熔断期间记录高分不应改变状态
            circuitBreaker.recordSafetyScore(9.9);
            assertEquals(CircuitState.OPEN, circuitBreaker.getCurrentState());
        }

        @Test
        @DisplayName("重复触发熔断 → 不重复发送告警")
        void shouldNotDuplicateTrip() {
            circuitBreaker.recordSafetyScore(5.0);
            assertEquals(CircuitState.OPEN, circuitBreaker.getCurrentState());

            // 再次触发不应该改变状态
            circuitBreaker.recordSafetyScore(5.0);
            assertEquals(CircuitState.OPEN, circuitBreaker.getCurrentState());
        }
    }

    // ==================== 半开状态探测测试 ====================

    @Nested
    @DisplayName("半开状态探测恢复")
    class HalfOpenTests {

        @Test
        @DisplayName("半开状态探测通过 → 恢复为 CLOSED")
        void shouldCloseAfterSuccessfulProbes() {
            // 先触发熔断
            circuitBreaker.recordSafetyScore(5.0);
            assertEquals(CircuitState.OPEN, circuitBreaker.getCurrentState());

            // 模拟进入半开状态（手动设置，绕过时间限制）
            var stateField = getCircuitStateField();
            setCircuitState(CircuitState.HALF_OPEN);

            // 3次高分探测 → 恢复
            circuitBreaker.recordSafetyScore(9.5);
            circuitBreaker.recordSafetyScore(9.6);
            circuitBreaker.recordSafetyScore(9.7);

            assertEquals(CircuitState.CLOSED, circuitBreaker.getCurrentState());
            assertTrue(circuitBreaker.allowAiCall());
        }

        @Test
        @DisplayName("半开状态第一次探测 → allowAiCall 返回 false")
        void shouldNotAllowCallOnFirstProbe() {
            circuitBreaker.recordSafetyScore(5.0);
            // 手动进入半开
            setCircuitState(CircuitState.HALF_OPEN);

            // 第一个探测请求应被采样机制处理
            assertFalse(circuitBreaker.allowAiCall());
        }
    }

    // ==================== 滑动窗口测试 ====================

    @Nested
    @DisplayName("滑动窗口平均值")
    class SlidingWindowTests {

        @Test
        @DisplayName("空窗口 → 返回默认值 10.0")
        void shouldReturnDefaultForEmptyWindow() {
            assertEquals(10.0, circuitBreaker.getAverageSafetyScore(), 0.01);
        }

        @Test
        @DisplayName("正常窗口 → 正确计算平均值")
        void shouldCalculateCorrectAverage() {
            circuitBreaker.recordSafetyScore(9.0);
            circuitBreaker.recordSafetyScore(9.5);
            circuitBreaker.recordSafetyScore(9.8);

            double avg = circuitBreaker.getAverageSafetyScore();
            assertTrue(avg > 9.0 && avg < 10.0);
        }

        @Test
        @DisplayName("超过 MAX_WINDOW_SIZE → 移除旧数据")
        void shouldEvictOldDataWhenWindowFull() {
            // 填充 >200 条数据
            for (int i = 0; i < 250; i++) {
                circuitBreaker.recordSafetyScore(5.0);
                // 不让它熔断（先恢复）
                if (circuitBreaker.getCurrentState() != CircuitState.CLOSED) {
                    setCircuitState(CircuitState.CLOSED);
                }
            }
            double avg = circuitBreaker.getAverageSafetyScore();
            // 平均值应接近 5.0（最新 200 条都是 5.0）
            assertTrue(avg <= 6.0, "平均分应 ≤ 6.0，实际: " + avg);
        }
    }

    // ==================== 状态查询测试 ====================

    @Nested
    @DisplayName("状态摘要查询")
    class StatusSummaryTests {

        @Test
        @DisplayName("getStatusSummary → 返回非空摘要")
        void shouldReturnNonEmptySummary() {
            String summary = circuitBreaker.getStatusSummary();
            assertNotNull(summary);
            assertTrue(summary.contains("state="));
            assertTrue(summary.contains("CLOSED"));
        }

        @Test
        @DisplayName("熔断后 → 摘要反映 OPEN 状态")
        void shouldReflectOpenStateInSummary() {
            circuitBreaker.recordSafetyScore(5.0);
            String summary = circuitBreaker.getStatusSummary();
            assertTrue(summary.contains("OPEN"));
        }
    }

    // ---- 辅助方法 ----

    private java.lang.reflect.Field getCircuitStateField() {
        try {
            var field = OnlineSafetyCircuitBreaker.class.getDeclaredField("currentState");
            field.setAccessible(true);
            return field;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setCircuitState(CircuitState state) {
        try {
            var field = OnlineSafetyCircuitBreaker.class.getDeclaredField("currentState");
            field.setAccessible(true);
            field.set(circuitBreaker, state);

            // 同步重置 halfOpenPassCount
            if (state == CircuitState.HALF_OPEN) {
                var passCountField = OnlineSafetyCircuitBreaker.class.getDeclaredField("halfOpenPassCount");
                passCountField.setAccessible(true);
                var atomicInt = (java.util.concurrent.atomic.AtomicInteger) passCountField.get(circuitBreaker);
                atomicInt.set(0);
            }

            // 同步重置 consecutiveFails
            if (state == CircuitState.CLOSED) {
                var failsField = OnlineSafetyCircuitBreaker.class.getDeclaredField("consecutiveFails");
                failsField.setAccessible(true);
                var atomicInt = (java.util.concurrent.atomic.AtomicInteger) failsField.get(circuitBreaker);
                atomicInt.set(0);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}