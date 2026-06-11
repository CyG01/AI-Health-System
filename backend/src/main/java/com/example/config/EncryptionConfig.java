package com.example.config;

import com.example.llmops.WebhookNotifier;
import com.example.util.AesEncryptor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * 字段加密配置。
 *
 * 在应用启动时从环境变量加载 AES 密钥并初始化 AesEncryptor。
 * 密钥通过环境变量注入，禁止硬编码。
 * 注入 WebhookNotifier 作为加密失败告警通道。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EncryptionConfig {

    private final WebhookNotifier webhookNotifier;

    @Value("${aes.encryption.key:}")
    private String currentKeyBase64;

    @Value("${aes.encryption.legacy-key:}")
    private String legacyKeyBase64;

    @PostConstruct
    public void init() {
        // 设置加密失败告警回调（钉钉/飞书/企微通知）
        AesEncryptor.setAlertCallback((severity, ruleName, message) -> {
            try {
                webhookNotifier.sendAlert(severity, ruleName, message);
            } catch (Exception e) {
                log.warn("加密告警Webhook发送失败", e);
            }
        });

        AesEncryptor.init(currentKeyBase64, legacyKeyBase64);
        log.info("字段级加密模块已就绪（含加密失败告警）");
    }
}