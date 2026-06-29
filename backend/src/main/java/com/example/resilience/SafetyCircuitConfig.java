package com.example.resilience;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 安全熔断器配置。
 * 可通过 application.yml 中的 safety.circuit 前缀覆盖默认值。
 */
@Data
@Component
@ConfigurationProperties(prefix = "safety.circuit")
public class SafetyCircuitConfig {

    /** 安全分低于此值触发预警（默认9.0） */
    private double safetyThreshold = 9.0;

    /** 安全分低于此值立即熔断（默认7.5） */
    private double meltdownThreshold = 7.5;

    /** 连续失败次数达到此值触发熔断（默认5） */
    private int consecutiveFailsToOpen = 5;

    /** 滑动窗口时长（分钟，默认30） */
    private int slidingWindowMinutes = 30;

    /** 熔断后探测间隔（分钟，默认10） */
    private int halfOpenTestInterval = 10;

    /** 每次探测允许通过的请求数（默认3） */
    private int halfOpenTestCount = 3;
}