package com.example.service;

import com.example.monitor.ModelTier;
import com.example.monitor.MultiModelCostMonitor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 本地 Ollama 模型服务（Phase 4：本地模型部署）。
 *
 * 用 Ollama 部署 Llama3-8B / Qwen2-7B 本地模型，
 * 验证 LOW 级请求（闲聊 / 简单查询）走本地模型，替代 30% 云端调用。
 *
 * 特性：
 * - 并发控制（Semaphore 限制最大并发调用）
 * - 超时控制
 * - 模型切换（主模型不可用时自动切换到备选模型）
 * - 成本记录（本地模型成本≈0，仅记录 Token 消耗）
 */
@Service
@ConditionalOnProperty(name = "resilience.ollama.enabled", havingValue = "true")
public class OllamaService {

    private static final Logger log = LoggerFactory.getLogger(OllamaService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final MultiModelCostMonitor costMonitor;
    private final Semaphore concurrencySemaphore;
    private final int timeoutMs;

    private final String primaryModel;
    private final String backupModel;
    private final String baseUrl;

    public OllamaService(@Value("${resilience.ollama.base-url:http://localhost:11434}") String baseUrl,
                          @Value("${resilience.ollama.model:qwen2:7b}") String primaryModel,
                          @Value("${resilience.ollama.backup-model:llama3:8b}") String backupModel,
                          @Value("${resilience.ollama.timeout:30000}") int timeoutMs,
                          @Value("${resilience.ollama.max-concurrent:4}") int maxConcurrent,
                          MultiModelCostMonitor costMonitor,
                          ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.primaryModel = primaryModel;
        this.backupModel = backupModel;
        this.timeoutMs = timeoutMs;
        this.costMonitor = costMonitor;
        this.objectMapper = objectMapper;
        this.concurrencySemaphore = new Semaphore(maxConcurrent);
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        log.info("OllamaService 初始化 baseUrl={} primaryModel={} backupModel={} maxConcurrent={}",
                baseUrl, primaryModel, backupModel, maxConcurrent);
    }

    /**
     * 调用本地 Ollama 模型进行对话。
     *
     * @param messages 消息列表 [{"role":"user","content":"..."}]
     * @param intent   意图分类（用于成本记录）
     * @param userId   用户ID（用于成本记录）
     * @return AI 响应文本
     */
    public String chat(List<Map<String, String>> messages, String intent, Long userId) {
        long start = System.currentTimeMillis();
        boolean acquired = false;

        try {
            // 并发控制
            acquired = concurrencySemaphore.tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
            if (!acquired) {
                log.warn("Ollama 并发已满，拒绝请求 userId={} intent={}", userId, intent);
                throw new RuntimeException("本地模型繁忙，请稍后重试");
            }

            // 尝试主模型
            String result = callModel(primaryModel, messages);
            long latency = System.currentTimeMillis() - start;

            // 记录成本（本地模型成本≈0）
            int estimatedTokens = estimateTokens(result);
            costMonitor.recordCall(userId, intent, primaryModel, ModelTier.LOW,
                    estimatedTokens / 2, estimatedTokens, latency, true);

            log.info("Ollama 调用成功 model={} intent={} userId={} latencyMs={} tokens~={}",
                    primaryModel, intent, userId, latency, estimatedTokens);

            return result;

        } catch (Exception e) {
            log.warn("Ollama 主模型调用失败 model={} 尝试备选模型 model={}", primaryModel, backupModel, e);

            // 尝试备选模型
            try {
                String result = callModel(backupModel, messages);
                long latency = System.currentTimeMillis() - start;

                int estimatedTokens = estimateTokens(result);
                costMonitor.recordCall(userId, intent, backupModel, ModelTier.LOW,
                        estimatedTokens / 2, estimatedTokens, latency, true);

                log.info("Ollama 备选模型调用成功 model={} intent={} userId={} latencyMs={}",
                        backupModel, intent, userId, latency);

                return result;
            } catch (Exception ex) {
                long latency = System.currentTimeMillis() - start;
                costMonitor.recordCall(userId, intent, "ollama-failed", ModelTier.LOW,
                        0, 0, latency, false);
                log.error("Ollama 所有模型调用失败 userId={} intent={}", userId, intent, ex);
                throw new RuntimeException("本地模型服务不可用: " + ex.getMessage());
            }
        } finally {
            if (acquired) {
                concurrencySemaphore.release();
            }
        }
    }

    /**
     * 简化版单轮对话。
     */
    public String singleChat(String systemPrompt, String userMessage, String intent, Long userId) {
        List<Map<String, String>> messages = new java.util.ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userMessage));
        return chat(messages, intent, userId);
    }

    /**
     * 调用 Ollama API（OpenAI 兼容格式）。
     */
    private String callModel(String modelName, List<Map<String, String>> messages) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", modelName);
        body.put("stream", false);
        body.put("temperature", 0.7);
        body.put("max_tokens", 1024);

        ArrayNode msgs = body.putArray("messages");
        for (var msg : messages) {
            ObjectNode node = msgs.addObject();
            node.put("role", msg.get("role"));
            node.put("content", msg.get("content"));
        }

        String response = webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .onErrorResume(e -> {
                    log.error("Ollama HTTP 调用失败 model={} error={}", modelName, e.getMessage());
                    return Mono.just("{\"error\":\"" + e.getMessage() + "\"}");
                })
                .block();

        if (response == null || response.contains("\"error\"")) {
            throw new RuntimeException("Ollama 返回错误: " + response);
        }

        return extractContent(response);
    }

    /**
     * 从 OpenAI 兼容响应中提取 content。
     */
    private String extractContent(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }
        } catch (Exception e) {
            log.error("Ollama 响应解析失败 response={}", response, e);
        }
        return response;
    }

    /**
     * 粗略估算 Token 数。
     */
    private int estimateTokens(String text) {
        if (text == null || text.isBlank()) return 0;
        int chineseChars = 0;
        int otherChars = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                chineseChars++;
            } else {
                otherChars++;
            }
        }
        return (int) (chineseChars / 1.5 + otherChars / 4.0);
    }

    /**
     * 检查 Ollama 服务是否健康。
     */
    public boolean isHealthy() {
        try {
            String response = webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return response != null && response.contains("\"name\"");
        } catch (Exception e) {
            log.warn("Ollama 健康检查失败 baseUrl={}", baseUrl, e);
            return false;
        }
    }

    /**
     * 获取当前可用模型列表。
     */
    public String getAvailableModels() {
        try {
            return webClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
        } catch (Exception e) {
            log.error("获取 Ollama 模型列表失败", e);
            return "{\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}