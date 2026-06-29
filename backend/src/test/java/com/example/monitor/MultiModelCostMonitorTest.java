package com.example.monitor;

import com.example.mapper.LlmCostLogMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MultiModelCostMonitorTest {

    @Mock private LlmCostLogMapper costLogMapper;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private HashOperations<String, Object, Object> hashOperations;
    @Mock private ValueOperations<String, Object> valueOperations;

    private MultiModelCostMonitor monitor;

    @BeforeEach
    void setUp() {
        monitor = new MultiModelCostMonitor(costLogMapper, redisTemplate);
        // Set @Value fields
        ReflectionTestUtils.setField(monitor, "dailyPerUserBudget", new BigDecimal("1.0"));
        ReflectionTestUtils.setField(monitor, "autoPauseThreshold", new BigDecimal("1.0"));
        ReflectionTestUtils.setField(monitor, "globalDailyBudget", new BigDecimal("10.00"));

        lenient().when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    // ==================== recordCall(int, int) ====================

    @Test
    @DisplayName("recordCall(int, int) delegates to Redis with HIGH tier pricing")
    void shouldRecordCallWithDefaultHighTier() {
        when(hashOperations.increment(anyString(), any(), anyLong())).thenReturn(0L);
        when(hashOperations.increment(anyString(), any(), anyDouble())).thenReturn(0.0);

        monitor.recordCall(100, 200);

        // Verify Redis hash increments were called (global stats updated)
        verify(hashOperations).increment(contains("llm:cost:daily:"), eq("inputTokens"), eq(100L));
        verify(hashOperations).increment(contains("llm:cost:daily:"), eq("outputTokens"), eq(200L));
        verify(hashOperations).increment(contains("llm:cost:daily:"), eq("totalCost"), anyDouble());
    }

    @Test
    @DisplayName("recordCall(int, int, ModelTier) updates Redis stats")
    void shouldRecordCallWithExplicitTier() {
        when(hashOperations.increment(anyString(), any(), anyLong())).thenReturn(0L);
        when(hashOperations.increment(anyString(), any(), anyDouble())).thenReturn(0.0);

        monitor.recordCall(500, 1000, ModelTier.MEDIUM);

        verify(hashOperations).increment(contains("llm:cost:daily:"), eq("inputTokens"), eq(500L));
        verify(hashOperations).increment(contains("llm:cost:daily:"), eq("outputTokens"), eq(1000L));
    }

    // ==================== isGlobalCostExceeded ====================

    @Test
    @DisplayName("isGlobalCostExceeded returns true when cost >= budget")
    void shouldReturnTrueWhenGlobalCostExceedsBudget() {
        when(costLogMapper.getGlobalDailyCost()).thenReturn(new BigDecimal("10.00"));

        assertTrue(monitor.isGlobalCostExceeded());
    }

    @Test
    @DisplayName("isGlobalCostExceeded returns false when under budget")
    void shouldReturnFalseWhenGlobalCostUnderBudget() {
        when(costLogMapper.getGlobalDailyCost()).thenReturn(new BigDecimal("5.00"));

        assertFalse(monitor.isGlobalCostExceeded());
    }

    @Test
    @DisplayName("isGlobalCostExceeded falls back to Redis when DB returns null")
    void shouldFallbackToRedisWhenDbReturnsNull() {
        when(costLogMapper.getGlobalDailyCost()).thenReturn(null);
        when(hashOperations.get(contains("llm:cost:daily:"), eq("totalCost"))).thenReturn("12.50");

        assertTrue(monitor.isGlobalCostExceeded());
    }

    @Test
    @DisplayName("isGlobalCostExceeded returns false when both DB and Redis have zero cost")
    void shouldReturnFalseWhenNoCostRecorded() {
        when(costLogMapper.getGlobalDailyCost()).thenReturn(BigDecimal.ZERO);

        assertFalse(monitor.isGlobalCostExceeded());
    }

    // ==================== isUserCostExceeded ====================

    @Test
    @DisplayName("isUserCostExceeded returns true when user cost >= budget")
    void shouldReturnTrueWhenUserCostExceedsBudget() {
        when(costLogMapper.getUserDailyCost(1L)).thenReturn(new BigDecimal("1.00"));

        assertTrue(monitor.isUserCostExceeded(1L));
    }

    @Test
    @DisplayName("isUserCostExceeded returns false when under budget")
    void shouldReturnFalseWhenUserCostUnderBudget() {
        when(costLogMapper.getUserDailyCost(1L)).thenReturn(new BigDecimal("0.50"));

        assertFalse(monitor.isUserCostExceeded(1L));
    }

    @Test
    @DisplayName("isUserCostExceeded returns false for null userId")
    void shouldReturnFalseForNullUserId() {
        assertFalse(monitor.isUserCostExceeded(null));
    }

    @Test
    @DisplayName("isUserCostExceeded falls back to Redis when DB returns null")
    void shouldFallbackToRedisForUserCost() {
        when(costLogMapper.getUserDailyCost(1L)).thenReturn(null);
        when(hashOperations.get(contains("llm:cost:user:"), eq("totalCost"))).thenReturn("1.50");

        assertTrue(monitor.isUserCostExceeded(1L));
    }

    // ==================== pauseUser / resumeUser ====================

    @Test
    @DisplayName("pauseUser and isUserPaused work correctly")
    void shouldPauseAndCheckUserStatus() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        assertFalse(monitor.isUserPaused(1L));

        monitor.pauseUser(1L);

        assertTrue(monitor.isUserPaused(1L));
        verify(valueOperations).set(eq("llm:cost:paused:1"), eq("1"));
    }

    @Test
    @DisplayName("resumeUser clears paused status")
    void shouldResumeUserAndClearStatus() {
        monitor.pauseUser(1L);
        assertTrue(monitor.isUserPaused(1L));

        monitor.resumeUser(1L);
        assertFalse(monitor.isUserPaused(1L));
        verify(redisTemplate).delete("llm:cost:paused:1");
    }
}
