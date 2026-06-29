package com.example.llmops;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Webhook 告警通知器。
 * 通过 HTTP POST 向配置的 Webhook 地址发送告警通知。
 */
@Slf4j
@Component
public class WebhookNotifier {

    @Value("${llmops.webhook.url:}")
    private String webhookUrl;

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    /**
     * 异步发送告警到已配置的 Webhook 地址。
     */
    @Async
    public void sendAlert(String severity, String ruleName, String message) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("severity", severity);
            body.put("ruleName", ruleName);
            body.put("message", message);
            body.put("timestamp", LocalDateTime.now().toString());

            WebClient.create().post()
                    .uri(webhookUrl)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(TIMEOUT)
                    .onErrorResume(e -> {
                        log.warn("Webhook告警发送失败: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            log.debug("Webhook告警已发送: {}", ruleName);
        } catch (Exception e) {
            log.warn("Webhook告警发送异常: {}", e.getMessage());
        }
    }
}