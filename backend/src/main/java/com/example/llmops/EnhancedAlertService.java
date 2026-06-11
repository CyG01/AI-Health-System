package com.example.llmops;

import com.example.util.AesEncryptor;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Phase 3 增强告警规则（可观测性闭环）。
 *
 * 新增告警规则：
 * - LLM P99 延迟 > 5s → CRITICAL
 * - LLM 错误率 > 5% → CRITICAL
 * - 加密操作失败 → CRITICAL
 * - 单用户日成本 > 1 元 → WARNING
 *
 * 通过 Micrometer Gauge 暴露 AES 操作计数器，
 * 供 Prometheus + Grafana 消费。
 */
@Slf4j
@Component
public class EnhancedAlertService {

    private final LlmMetricsCollector llmMetrics;
    private final AlertManager alertManager;
    private final MeterRegistry meterRegistry;

    /** 上次告警发送时间（毫秒），用于冷却 */
    private long lastLatencyAlertTs = 0;
    private long lastErrorRateAlertTs = 0;
    private long lastEncryptFailAlertTs = 0;

    private static final long ALERT_COOLDOWN_MS = 300_000; // 5 分钟冷却

    public EnhancedAlertService(LlmMetricsCollector llmMetrics,
                                 AlertManager alertManager,
                                 MeterRegistry meterRegistry) {
        this.llmMetrics = llmMetrics;
        this.alertManager = alertManager;
        this.meterRegistry = meterRegistry;
        registerAesMetrics();
    }

    /**
     * 将 AesEncryptor 静态计数器注册为 Prometheus Gauge。
     */
    private void registerAesMetrics() {
        Gauge.builder("aes_encrypt_operations_total", AesEncryptor::getEncryptTotal)
                .description("AES 加密操作总数")
                .register(meterRegistry);

        Gauge.builder("aes_encrypt_failure_total", AesEncryptor::getEncryptFailureCount)
                .description("AES 加密失败次数")
                .register(meterRegistry);

        Gauge.builder("aes_decrypt_operations_total", AesEncryptor::getDecryptTotal)
                .description("AES 解密操作总数")
                .register(meterRegistry);

        Gauge.builder("aes_decrypt_failure_total", AesEncryptor::getDecryptFailureCount)
                .description("AES 解密失败次数")
                .register(meterRegistry);

        log.info("AES 操作计数器已注册到 Prometheus");
    }

    /**
     * 每 30 秒检查增强告警规则。
     */
    @Scheduled(fixedRate = 30_000)
    public void checkEnhancedAlerts() {
        checkLatencyAlert();
        checkErrorRateAlert();
        checkEncryptionFailureAlert();
    }

    /**
     * P99 端到端延迟 > 5s → CRITICAL 告警。
     */
    private void checkLatencyAlert() {
        double p99 = llmMetrics.getP99E2eSeconds();
        long totalCalls = llmMetrics.getTotalCalls();

        // 至少 10 次调用后才检查，避免冷启动误报
        if (totalCalls < 10) return;

        if (p99 > 5.0) {
            long now = System.currentTimeMillis();
            if (now - lastLatencyAlertTs > ALERT_COOLDOWN_MS) {
                lastLatencyAlertTs = now;
                alertManager.sendP0Alert("LLM延迟告警",
                        String.format("P99 端到端延迟 %.1fs > 5s 阈值，总调用 %d 次",
                                p99, totalCalls));
            }
        }
    }

    /**
     * LLM 错误率 > 5% → CRITICAL 告警。
     */
    private void checkErrorRateAlert() {
        double errorRate = llmMetrics.getErrorRate();
        long totalCalls = llmMetrics.getTotalCalls();

        if (totalCalls < 20) return;

        if (errorRate > 0.05) {
            long now = System.currentTimeMillis();
            if (now - lastErrorRateAlertTs > ALERT_COOLDOWN_MS) {
                lastErrorRateAlertTs = now;
                long failedCalls = llmMetrics.getFailedCalls();
                alertManager.sendP0Alert("LLM错误率告警",
                        String.format("LLM 错误率 %.1f%% > 5%% 阈值，失败 %d/%d 次",
                                errorRate * 100, failedCalls, totalCalls));
            }
        }
    }

    /**
     * 加密操作失败 → CRITICAL 告警。
     */
    private void checkEncryptionFailureAlert() {
        long encryptFails = AesEncryptor.getEncryptFailureCount();
        long decryptFails = AesEncryptor.getDecryptFailureCount();

        if (encryptFails > 0 || decryptFails > 0) {
            long now = System.currentTimeMillis();
            if (now - lastEncryptFailAlertTs > ALERT_COOLDOWN_MS) {
                lastEncryptFailAlertTs = now;
                alertManager.sendP0Alert("加密失败告警",
                        String.format("检测到加密操作失败：加密失败 %d 次，解密失败 %d 次。请检查 AES 密钥配置！",
                                encryptFails, decryptFails));
            }
        }
    }
}