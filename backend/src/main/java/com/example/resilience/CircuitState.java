package com.example.resilience;

/**
 * 安全熔断器状态枚举。
 * CLOSED: 正常运行
 * OPEN: 熔断中，所有请求走降级
 * HALF_OPEN: 探测恢复中，允许少量请求通过
 */
public enum CircuitState {
    CLOSED,
    OPEN,
    HALF_OPEN
}