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
 * 支持钉钉、飞书、企业微信等平台的 Webhook 通知。
 */
@Slf4j
@Component
public class WebhookNotifier {

    @Value("${llmops.webhook.dingtalk:}")
    private String dingtalkWebhook;

    @Value("${llmops.webhook.feishu:}")
    private String feishuWebhook;

    @Value("${llmops.webhook.wecom:}")
    private String wecomWebhook;

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    /**
     * 异步发送告警到已配置的所有渠道。
     */
    @Async
    public void sendAlert(String severity, String ruleName, String message) {
        sendDingtalk(severity, ruleName, message);
        sendFeishu(severity, ruleName, message);
        sendWecom(severity, ruleName, message);
    }

    /**
     * 钉钉 Webhook 通知。
     */
    private void sendDingtalk(String severity, String ruleName, String message) {
        if (dingtalkWebhook == null || dingtalkWebhook.isBlank()) return;

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("msgtype", "markdown");

            Map<String, String> title;
            String emoji;
            switch (severity) {
                case "CRITICAL" -> {
                    title = Map.of("title", "【紧急】AI健康系统告警");
                    emoji = "\uD83D\uDD34";
                }
                case "WARNING" -> {
                    title = Map.of("title", "【警告】AI健康系统告警");
                    emoji = "\uD83D\uDFE1";
                }
                default -> {
                    title = Map.of("title", "【通知】AI健康系统消息");
                    emoji = "\uD83D\uDD35";
                }
            }

            String markdown = String.format(
                    "%s **%s**\n\n- **级别**: %s\n- **规则**: %s\n- **内容**: %s\n- **时间**: %s\n\n---\n*LLMOps 运维告警*",
                    emoji, title.get("title"), severity, ruleName, message,
                    LocalDateTime.now().toString().replace("T", " "));

            Map<String, Object> markdownObj = new LinkedHashMap<>();
            markdownObj.put("title", title.get("title"));
            markdownObj.put("text", markdown);
            body.put("markdown", markdownObj);

            WebClient.create().post()
                    .uri(dingtalkWebhook)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(TIMEOUT)
                    .onErrorResume(e -> {
                        log.warn("钉钉告警发送失败: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            log.debug("钉钉告警已发送: {}", ruleName);
        } catch (Exception e) {
            log.warn("钉钉告警发送异常: {}", e.getMessage());
        }
    }

    /**
     * 飞书 Webhook 通知。
     */
    private void sendFeishu(String severity, String ruleName, String message) {
        if (feishuWebhook == null || feishuWebhook.isBlank()) return;

        try {
            String color;
            switch (severity) {
                case "CRITICAL" -> color = "red";
                case "WARNING" -> color = "yellow";
                default -> color = "blue";
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("msg_type", "interactive");

            Map<String, Object> card = new LinkedHashMap<>();
            Map<String, Object> header = new LinkedHashMap<>();
            header.put("title", Map.of("content", "[LLMOps] " + severity, "tag", "plain_text"));
            header.put("template", color);
            card.put("header", header);

            java.util.List<Map<String, Object>> elements = new java.util.ArrayList<>();
            Map<String, Object> textElement = new LinkedHashMap<>();
            textElement.put("tag", "div");
            textElement.put("text", Map.of("tag", "lark_md",
                    "content", String.format("**规则**: %s\n**内容**: %s\n**时间**: %s",
                            ruleName, message, LocalDateTime.now().toString().replace("T", " "))));
            elements.add(textElement);

            Map<String, Object> hrElement = new LinkedHashMap<>();
            hrElement.put("tag", "hr");
            elements.add(hrElement);

            Map<String, Object> noteElement = new LinkedHashMap<>();
            noteElement.put("tag", "note");
            noteElement.put("elements", java.util.List.of(
                    Map.of("tag", "plain_text", "content", "LLMOps 运维告警")));
            elements.add(noteElement);

            card.put("elements", elements);
            body.put("card", card);

            WebClient.create().post()
                    .uri(feishuWebhook)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(TIMEOUT)
                    .onErrorResume(e -> {
                        log.warn("飞书告警发送失败: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            log.debug("飞书告警已发送: {}", ruleName);
        } catch (Exception e) {
            log.warn("飞书告警发送异常: {}", e.getMessage());
        }
    }

    /**
     * 企业微信 Webhook 通知。
     */
    private void sendWecom(String severity, String ruleName, String message) {
        if (wecomWebhook == null || wecomWebhook.isBlank()) return;

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("msgtype", "markdown");

            String color;
            switch (severity) {
                case "CRITICAL" -> color = "warning";
                case "WARNING" -> color = "comment";
                default -> color = "info";
            }

            Map<String, Object> markdown = new LinkedHashMap<>();
            markdown.put("content", String.format(
                    "# [LLMOps] %s\n> 规则: <font color=\"%s\">%s</font>\n> 内容: %s\n> 时间: %s",
                    severity, color, ruleName, message,
                    LocalDateTime.now().toString().replace("T", " ")));
            body.put("markdown", markdown);

            WebClient.create().post()
                    .uri(wecomWebhook)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .toBodilessEntity()
                    .timeout(TIMEOUT)
                    .onErrorResume(e -> {
                        log.warn("企微告警发送失败: {}", e.getMessage());
                        return Mono.empty();
                    })
                    .block();

            log.debug("企微告警已发送: {}", ruleName);
        } catch (Exception e) {
            log.warn("企微告警发送异常: {}", e.getMessage());
        }
    }
}