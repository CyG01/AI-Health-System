package com.example.service.impl;

import com.example.common.BusinessException;
import com.example.entity.FoodItem;
import com.example.mapper.FoodItemMapper;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.properties.DeepSeekProperties;
import com.example.service.FoodRecognitionService;
import com.example.vo.FoodRecognizeVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class FoodRecognitionServiceImpl implements FoodRecognitionService {

    private final DeepSeekCostMonitor costMonitor;
    private final DeepSeekProperties deepSeekProperties;
    private final FoodItemMapper foodItemMapper;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public FoodRecognitionServiceImpl(DeepSeekCostMonitor costMonitor,
                                       DeepSeekProperties deepSeekProperties,
                                       FoodItemMapper foodItemMapper,
                                       ObjectMapper objectMapper) {
        this.costMonitor = costMonitor;
        this.deepSeekProperties = deepSeekProperties;
        this.foodItemMapper = foodItemMapper;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(deepSeekProperties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + deepSeekProperties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public FoodRecognizeVO recognize(MultipartFile image) {
        if (costMonitor.isGlobalCostExceeded()) {
            throw new BusinessException("今日AI调用额度已用尽，请明天再试");
        }

        try {
            // 图片转Base64
            byte[] imageBytes = image.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(imageBytes);

            // 使用视觉模型识别食物
            String prompt = "请识别图片中的食物，并估算每100克的热量（kcal）、蛋白质、碳水、脂肪。" +
                    "严格按照JSON格式输出：{\"foodName\":\"名称\",\"caloriePer100g\":数字,\"proteinPer100g\":数字,\"carbsPer100g\":数字,\"fatPer100g\":数字,\"category\":\"分类\",\"confidence\":置信度0-100}。" +
                    "如果无法识别，返回{\"error\":\"无法识别图片内容\"}。只输出JSON不要其他内容。";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-vision");
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", List.of(
                            Map.of("type", "text", "text", prompt),
                            Map.of("type", "image_url", "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image))
                    ))
            ));
            requestBody.put("response_format", Map.of("type", "json_object"));

            String response = webClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            // 解析token使用量
            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                int inputTokens = root.path("usage").path("prompt_tokens").asInt();
                int outputTokens = root.path("usage").path("completion_tokens").asInt();
                costMonitor.recordCall(inputTokens, outputTokens);

                String content = root.path("choices").get(0).path("message").path("content").asText();
                if (content == null || content.isBlank()) {
                    throw new BusinessException("AI返回内容为空");
                }

                // 检查是否有错误
                JsonNode contentJson = objectMapper.readTree(content);
                if (contentJson.has("error")) {
                    throw new BusinessException(contentJson.get("error").asText());
                }

                FoodRecognizeVO vo = new FoodRecognizeVO();
                vo.setFoodName(contentJson.path("foodName").asText());
                vo.setCaloriePer100g(contentJson.path("caloriePer100g").asInt());
                vo.setProteinPer100g(BigDecimal.valueOf(contentJson.path("proteinPer100g").asDouble()));
                vo.setCarbsPer100g(BigDecimal.valueOf(contentJson.path("carbsPer100g").asDouble()));
                vo.setFatPer100g(BigDecimal.valueOf(contentJson.path("fatPer100g").asDouble()));
                vo.setCategory(contentJson.path("category").asText("主食"));
                vo.setConfidence(contentJson.path("confidence").asInt(80));
                vo.setRecommendedGrams(calculateRecommended(vo));

                // 尝试匹配本地食物数据库（如果匹配上用本地更精准的数据）
                matchLocalFood(vo);

                return vo;
            }

            throw new BusinessException("AI识别失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("食物识别异常", e);
            throw new BusinessException("食物识别失败，请重试");
        }
    }

    private int calculateRecommended(FoodRecognizeVO vo) {
        // 根据食物类型给出合理建议量
        String category = vo.getCategory();
        if ("主食".equals(category)) return 100;
        if ("蛋白质".equals(category)) return 150;
        if ("蔬菜".equals(category)) return 200;
        if ("水果".equals(category)) return 150;
        if ("油脂".equals(category)) return 15;
        return 100;
    }

    private void matchLocalFood(FoodRecognizeVO vo) {
        String name = vo.getFoodName().trim().toLowerCase();
        List<FoodItem> allFood = foodItemMapper.selectList(null);
        for (FoodItem item : allFood) {
            if (item.getName().toLowerCase().contains(name) || name.contains(item.getName().toLowerCase())) {
                // 匹配成功，使用本地更精准的数据
                vo.setCaloriePer100g(item.getCaloriePer100g());
                vo.setProteinPer100g(item.getProteinPer100g());
                vo.setCarbsPer100g(item.getCarbsPer100g());
                vo.setFatPer100g(item.getFatPer100g());
                vo.setCategory(item.getCategory());
                break;
            }
        }
    }
}