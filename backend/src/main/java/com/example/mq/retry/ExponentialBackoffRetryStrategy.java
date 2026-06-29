package com.example.mq.retry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 指数退避重试策略。
 * 重试间隔：1s -> 2s -> 4s -> 8s -> 16s，最大重试 5 次。
 */
@Slf4j
@Component
public class ExponentialBackoffRetryStrategy {

    @Value("${ai-task.retry.max-attempts:5}")
    private int maxAttempts;

    @Value("${ai-task.retry.initial-delay-ms:1000}")
    private long initialDelayMs;

    @Value("${ai-task.retry.max-delay-ms:16000}")
    private long maxDelayMs;

    @Value("${ai-task.retry.multiplier:2.0}")
    private double multiplier;

    /**
     * 判断是否应继续重试。
     */
    public boolean shouldRetry(int currentRetryCount) {
        return currentRetryCount < maxAttempts;
    }

    /**
     * 计算第 N 次重试的等待时间（毫秒）。
     * 公式：min(initialDelay * multiplier^(retryCount-1), maxDelay)
     */
    public long getDelayMs(int retryCount) {
        if (retryCount <= 0) {
            return 0;
        }
        long delay = (long) (initialDelayMs * Math.pow(multiplier, retryCount - 1));
        return Math.min(delay, maxDelayMs);
    }

    /**
     * 获取最大重试次数。
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }
}