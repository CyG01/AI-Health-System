package com.example.service;

import com.example.common.BusinessException;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.properties.DeepSeekProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    private static final String PROMPT_TEMPLATE = "H:%.1f,W:%.1f,G:%s,D:%d,I:%s.仅输出JSON:{\"days\":[{\"d\":1,\"items\":[\"\"]}]}";

    private final WebClient webClient;
    private final DeepSeekProperties properties;
    private final ObjectMapper objectMapper;

    @Autowired
    private DeepSeekCostMonitor costMonitor;

    public DeepSeekService(DeepSeekProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Retry(name = "deepseek", fallbackMethod = "deepSeekFallback")
    @CircuitBreaker(name = "deepseek", fallbackMethod = "deepSeekFallback")
    public String callApi(BigDecimal height, BigDecimal weight, String goal,
                          Integer durationDays, String preference) {
        return callApi(height, weight, goal, durationDays, preference, properties.getModel());
    }

    @Retry(name = "deepseek", fallbackMethod = "deepSeekFallback")
    @CircuitBreaker(name = "deepseek", fallbackMethod = "deepSeekFallback")
    public String callApi(BigDecimal height, BigDecimal weight, String goal,
                          Integer durationDays, String preference, String model) {
        if (costMonitor.isGlobalCostExceeded()) {
            throw new BusinessException("今日AI调用额度已用尽，请明天再试");
        }

        String prompt = String.format(PROMPT_TEMPLATE, height, weight, goal, durationDays, preference);

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a health plan generator. Always respond with valid JSON."),
                        Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of("type", "json_object")
        );

        try {
            String response = webClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(properties.getTimeout()))
                    .block();

            JsonNode root = objectMapper.readTree(response);
            int inputTokens = root.path("usage").path("prompt_tokens").asInt();
            int outputTokens = root.path("usage").path("completion_tokens").asInt();
            costMonitor.recordCall(inputTokens, outputTokens);

            String content = root.path("choices").get(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new BusinessException("AI返回内容为空");
            }
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek API调用异常", e);
            throw new BusinessException("AI服务调用失败");
        }
    }

    private String deepSeekFallback(BigDecimal height, BigDecimal weight, String goal,
                                    Integer durationDays, String preference, Throwable t) {
        log.error("DeepSeek API熔断/重试失败", t);
        throw new BusinessException("AI服务暂时不可用");
    }
}
