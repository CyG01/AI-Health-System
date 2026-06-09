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
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    private static final String PROMPT_TEMPLATE = "H:%.1f,W:%.1f,G:%s,D:%d,I:%s.仅输出JSON:{\"days\":[{\"d\":1,\"items\":[\"\"]}]}";

    private static final String DONE_MARKER = "[DONE]";

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

    public Flux<String> callApiStream(BigDecimal height, BigDecimal weight, String goal,
                                      Integer durationDays, String preference, String model) {
        if (costMonitor.isGlobalCostExceeded()) {
            return Flux.error(new BusinessException("今日AI调用额度已用尽，请明天再试"));
        }

        String prompt = String.format(PROMPT_TEMPLATE, height, weight, goal, durationDays, preference);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are a health plan generator. Always respond with valid JSON."),
                Map.of("role", "user", "content", prompt)
        ));
        requestBody.put("stream", true);

        return webClient.post()
                .uri("/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .timeout(Duration.ofMillis(properties.getTimeout()))
                .filter(line -> line != null && !line.isBlank())
                .filter(line -> !DONE_MARKER.equals(line.trim()))
                .mapNotNull(this::extractDeltaContent)
                .onErrorMap(e -> {
                    if (e instanceof BusinessException) {
                        return e;
                    }
                    log.error("DeepSeek流式API调用异常", e);
                    return new BusinessException("AI流式服务调用失败");
                });
    }

    private String extractDeltaContent(String line) {
        try {
            String jsonStr = line;
            if (line.startsWith("data:")) {
                jsonStr = line.substring(5).trim();
            }
            if (jsonStr.isEmpty() || DONE_MARKER.equals(jsonStr)) {
                return null;
            }
            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode delta = choices.get(0).path("delta");
                JsonNode content = delta.path("content");
                if (!content.isNull()) {
                    String text = content.asText();
                    return text.isEmpty() ? null : text;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String deepSeekFallback(BigDecimal height, BigDecimal weight, String goal,
                                    Integer durationDays, String preference, Throwable t) {
        log.error("DeepSeek API熔断/重试失败，启用降级方案", t);
        return generateFallbackPlan(height, weight, goal, durationDays);
    }

    private String generateFallbackPlan(BigDecimal height, BigDecimal weight,
                                         String goal, int durationDays) {
        double w = weight.doubleValue();
        double h = height.doubleValue();
        double bmi = w / ((h / 100) * (h / 100));

        StringBuilder json = new StringBuilder("{\"days\":[");

        for (int d = 1; d <= durationDays; d++) {
            if (d > 1) json.append(",");
            json.append("{\"d\":").append(d).append(",\"items\":[");

            int exerciseMin = 30 + (d % 3) * 5;
            int calorieTarget = (int)(1500 + w * 5);
            String mealTip = bmi > 25 ? "控制碳水摄入，增加蔬菜比例" : "均衡饮食，保证蛋白质摄入";
            String exerciseTip = preference.contains("high")
                    ? "高强度间歇训练" + exerciseMin + "分钟"
                    : "快走或慢跑" + exerciseMin + "分钟";

            json.append("\"").append(mealTip).append("\",")
                .append("\"").append(exerciseTip).append("\",")
                .append("\"每日目标热量").append(calorieTarget).append("kcal\"");
            json.append("]}");
        }
        json.append("]}");
        return json.toString();
    }
}
