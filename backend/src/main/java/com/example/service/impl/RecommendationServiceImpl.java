package com.example.service.impl;

import com.example.entity.*;
import com.example.mapper.*;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.properties.DeepSeekProperties;
import com.example.service.HealthService;
import com.example.service.RecommendationService;
import com.example.vo.HealthRecordVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RecommendationServiceImpl implements RecommendationService {

    private static final String CACHE_KEY_PREFIX = "recommendation:";

    private final HealthService healthService;
    private final DailyCheckinMapper checkinMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final DietRecordMapper dietRecordMapper;
    private final ExerciseItemMapper exerciseItemMapper;
    private final FoodItemMapper foodItemMapper;
    private final DeepSeekCostMonitor costMonitor;
    private final DeepSeekProperties deepSeekProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public RecommendationServiceImpl(HealthService healthService,
                                      DailyCheckinMapper checkinMapper,
                                      ExerciseRecordMapper exerciseRecordMapper,
                                      DietRecordMapper dietRecordMapper,
                                      ExerciseItemMapper exerciseItemMapper,
                                      FoodItemMapper foodItemMapper,
                                      DeepSeekCostMonitor costMonitor,
                                      DeepSeekProperties deepSeekProperties,
                                      RedisTemplate<String, Object> redisTemplate,
                                      ObjectMapper objectMapper) {
        this.healthService = healthService;
        this.checkinMapper = checkinMapper;
        this.exerciseRecordMapper = exerciseRecordMapper;
        this.dietRecordMapper = dietRecordMapper;
        this.exerciseItemMapper = exerciseItemMapper;
        this.foodItemMapper = foodItemMapper;
        this.costMonitor = costMonitor;
        this.deepSeekProperties = deepSeekProperties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(deepSeekProperties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + deepSeekProperties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public Map<String, Object> getRecommendations(Long userId) {
        // 先查缓存
        String cacheKey = CACHE_KEY_PREFIX + userId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached.toString(), Map.class);
            } catch (Exception ignored) {}
        }

        if (costMonitor.isGlobalCostExceeded()) {
            return generateFallback(userId);
        }

        try {
            HealthRecordVO health = healthService.getLatestHealthRecord(userId);
            String aiSuggestions = callAIForRecommendations(health);

            Map<String, Object> result = new HashMap<>();
            result.put("exercises", getPopularExercises());
            result.put("foods", getHealthyFoods());
            result.put("aiSuggestions", aiSuggestions);
            result.put("healthTips", getHealthTips(health));

            // 缓存1小时
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(result), 1, TimeUnit.HOURS);

            return result;
        } catch (Exception e) {
            log.error("生成推荐失败", e);
            return generateFallback(userId);
        }
    }

    private String callAIForRecommendations(HealthRecordVO health) {
        String prompt = String.format(
                "你是专业健康顾问。请根据以下用户数据生成3条个性化健康建议（总字数控制在200字以内）。\n" +
                        "用户：身高%.1fcm，体重%.1fkg，BMI%.1f，目标：%s。\n" +
                        "请简洁回复，每条建议用编号开头。",
                health.getHeight(), health.getWeight(), health.getBmi(),
                health.getGoal() != null ? health.getGoal() : "未设定");

        Map<String, Object> requestBody = Map.of(
                "model", deepSeekProperties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你是专业健康顾问，回答要简短具体。"),
                        Map.of("role", "user", "content", prompt)
                )
        );

        String response = webClient.post()
                .uri("/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        try {
            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
            int inputTokens = root.path("usage").path("prompt_tokens").asInt();
            int outputTokens = root.path("usage").path("completion_tokens").asInt();
            costMonitor.recordCall(inputTokens, outputTokens);

            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            return "无法获取AI建议，请稍后重试";
        }
    }

    private List<Map<String, Object>> getPopularExercises() {
        List<ExerciseItem> items = exerciseItemMapper.selectList(null);
        return items.stream().limit(6).map(item -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", item.getId());
            m.put("name", item.getName());
            m.put("type", item.getType());
            m.put("caloriePerHour", (int)(item.getCalorieCoefficient().doubleValue() * 70));
            m.put("difficulty", item.getDifficulty() != null ? item.getDifficulty() : "初级");
            m.put("targetMuscle", item.getTargetMuscle());
            return m;
        }).collect(Collectors.toList());
    }

    private List<Map<String, Object>> getHealthyFoods() {
        List<FoodItem> items = foodItemMapper.selectList(null);
        return items.stream().filter(f -> f.getCaloriePer100g() < 200).limit(6).map(item -> {
            Map<String, Object> m = new HashMap<>();
            m.put("id", item.getId());
            m.put("name", item.getName());
            m.put("category", item.getCategory());
            m.put("caloriePer100g", item.getCaloriePer100g());
            m.put("proteinPer100g", item.getProteinPer100g());
            return m;
        }).collect(Collectors.toList());
    }

    private List<String> getHealthTips(HealthRecordVO health) {
        List<String> tips = new java.util.ArrayList<>();
        double bmi = health.getBmi() != null ? health.getBmi().doubleValue() : 22;
        if (bmi < 18.5) {
            tips.add("BMI偏低，建议增加蛋白质和碳水摄入");
            tips.add("配合力量训练增加肌肉量");
        } else if (bmi >= 25) {
            tips.add("当前BMI偏高，建议控制饮食热量摄入");
            tips.add("每周至少进行150分钟中等强度有氧运动");
        } else {
            tips.add("BMI处于正常范围，保持当前运动习惯");
            tips.add("注意均衡饮食，多样化摄入营养");
        }
        tips.add("每天保证2000ml饮水量");
        return tips;
    }

    private Map<String, Object> generateFallback(Long userId) {
        Map<String, Object> result = new HashMap<>();
        result.put("exercises", getPopularExercises());
        result.put("foods", getHealthyFoods());
        result.put("aiSuggestions", "AI服务暂时不可用，以下是常规建议：\n1.每天保持30分钟中强度运动\n2.多吃蔬菜水果，减少高糖高脂食物\n3.保证7-8小时睡眠");
        result.put("healthTips", List.of("每天饮水2000ml", "保持规律作息", "餐后散步15分钟"));
        return result;
    }
}