package com.example.safety;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Layer2 医疗红线判别客户端 — 调用 Python BERT 微服务。
 *
 * 三层安全防线：
 *   Layer1: Regex 正则快速拦截（毫秒级）
 *   Layer2: BERT 模型语义判别（本服务）→ 目标召回率 ≥99.9%
 *   Layer3: SafetyReviewAgent 最终审查（LLM Agent）
 */
@Slf4j
@Service
public class BertRedlineClient {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor;

    @Value("${safety.bert.service-url:http://localhost:5001}")
    private String bertServiceUrl;

    @Value("${safety.bert.enabled:true}")
    private boolean enabled;

    @Value("${safety.bert.timeout-ms:2000}")
    private int timeoutMs;

    public BertRedlineClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        this.objectMapper = new ObjectMapper();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * 对单条文本进行红线判别。
     *
     * @param text 用户输入文本
     * @return 判别结果
     */
    public CompletableFuture<BertClassifyResponse> classify(String text) {
        if (!enabled) {
            return CompletableFuture.completedFuture(
                    BertClassifyResponse.passThrough()
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String requestBody = objectMapper.writeValueAsString(
                        new BertClassifyRequest(text)
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(bertServiceUrl + "/v1/classify"))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofMillis(timeoutMs))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    BertClassifyResponse result = objectMapper.readValue(
                            response.body(), BertClassifyResponse.class
                    );
                    log.debug("[BERT] 判别完成: safe={}, confidence={}, elapsed={}ms",
                            result.isSafe(), result.getConfidence(), result.getElapsedMs());
                    return result;
                } else {
                    log.warn("[BERT] 服务返回非200: {} {}", response.statusCode(), response.body());
                    // 降级：返回安全（误放优于误拦截）
                    return BertClassifyResponse.passThrough();
                }
            } catch (Exception e) {
                log.error("[BERT] 调用失败，降级放行: {}", e.getMessage());
                return BertClassifyResponse.passThrough();
            }
        }, executor);
    }

    /**
     * 批量判别。
     *
     * @param texts 文本列表
     * @return 判别结果列表
     */
    public CompletableFuture<BertBatchClassifyResponse> classifyBatch(List<String> texts) {
        if (!enabled || texts == null || texts.isEmpty()) {
            return CompletableFuture.completedFuture(
                    BertBatchClassifyResponse.empty()
            );
        }

        return CompletableFuture.supplyAsync(() -> {
            try {
                String requestBody = objectMapper.writeValueAsString(
                        new BertBatchClassifyRequest(texts)
                );

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(bertServiceUrl + "/v1/batch_classify"))
                        .header("Content-Type", "application/json")
                        .timeout(Duration.ofMillis(timeoutMs * 2))
                        .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                        .build();

                HttpResponse<String> response = httpClient.send(request,
                        HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    return objectMapper.readValue(response.body(), BertBatchClassifyResponse.class);
                } else {
                    log.warn("[BERT] 批量判别失败: {} {}", response.statusCode(), response.body());
                    return BertBatchClassifyResponse.empty();
                }
            } catch (Exception e) {
                log.error("[BERT] 批量判别异常: {}", e.getMessage());
                return BertBatchClassifyResponse.empty();
            }
        }, executor);
    }

    /**
     * 健康检查。
     */
    public boolean isHealthy() {
        if (!enabled) return true;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(bertServiceUrl + "/health"))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    // ===================== 内部 DTO =====================

    @Data
    private static class BertClassifyRequest {
        private String text;
        public BertClassifyRequest(String text) { this.text = text; }
    }

    @Data
    public static class BertClassifyResponse {
        private boolean safe;
        private String label;
        private double confidence;
        @JsonProperty("elapsed_ms")
        private double elapsedMs;

        /** 服务不可用时的降级结果：标记为安全 */
        public static BertClassifyResponse passThrough() {
            BertClassifyResponse r = new BertClassifyResponse();
            r.safe = true;
            r.label = "safe";
            r.confidence = 0.5;
            r.elapsedMs = 0;
            return r;
        }
    }

    @Data
    private static class BertBatchClassifyRequest {
        private List<String> texts;
        public BertBatchClassifyRequest(List<String> texts) { this.texts = texts; }
    }

    @Data
    public static class BertBatchClassifyResponse {
        private int total;
        @JsonProperty("unsafe_count")
        private int unsafeCount;
        private List<BertClassifyResponse> results;

        public static BertBatchClassifyResponse empty() {
            BertBatchClassifyResponse r = new BertBatchClassifyResponse();
            r.total = 0;
            r.unsafeCount = 0;
            r.results = List.of();
            return r;
        }
    }
}