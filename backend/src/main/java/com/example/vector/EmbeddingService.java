package com.example.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 向量嵌入服务。
 * 封装 DeepSeek Embedding API 调用，支持单条和批量嵌入，带重试机制。
 */
@Slf4j
@Service
public class EmbeddingService {

    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${deepseek.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${qdrant.retry.max-attempts:3}")
    private int maxRetries;

    public EmbeddingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    /**
     * 单条文本生成向量。
     */
    public float[] embed(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        List<float[]> results = batchEmbed(List.of(text));
        return results != null && !results.isEmpty() ? results.get(0) : null;
    }

    /**
     * 批量生成向量，带重试机制。
     * 失败重试 maxRetries 次，失败则跳过该文档并告警。
     */
    public List<float[]> batchEmbed(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return List.of();
        }

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                Map<String, Object> body = Map.of(
                        "model", "deepseek-chat",
                        "input", texts
                );
                String response = webClient.post()
                        .uri("/embeddings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();

                JsonNode node = objectMapper.readTree(response);
                JsonNode dataArray = node.get("data");

                List<float[]> result = new ArrayList<>();
                for (JsonNode item : dataArray) {
                    JsonNode embeddingNode = item.get("embedding");
                    float[] vec = new float[embeddingNode.size()];
                    for (int i = 0; i < embeddingNode.size(); i++) {
                        vec[i] = (float) embeddingNode.get(i).asDouble();
                    }
                    result.add(vec);
                }
                return result;
            } catch (Exception e) {
                log.warn("向量生成失败 attempt={}/{} texts={} err={}",
                        attempt, maxRetries, texts.size(), e.getMessage());
                if (attempt < maxRetries) {
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("向量生成全部重试失败 texts={} 跳过该批次", texts.size());
        return List.of();
    }
}