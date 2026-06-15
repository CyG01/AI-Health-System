package com.example.service.impl;

import com.example.common.BusinessException;
import com.example.entity.ExerciseItem;
import com.example.mapper.ExerciseItemMapper;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.properties.DeepSeekProperties;
import com.example.service.ExerciseGuidanceService;
import com.example.vo.ExerciseGuidanceVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class ExerciseGuidanceServiceImpl implements ExerciseGuidanceService {

    private static final String CACHE_PREFIX = "exercise:guidance:";

    private final ExerciseItemMapper exerciseItemMapper;
    private final DeepSeekCostMonitor costMonitor;
    private final DeepSeekProperties deepSeekProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public ExerciseGuidanceServiceImpl(ExerciseItemMapper exerciseItemMapper,
                                        DeepSeekCostMonitor costMonitor,
                                        DeepSeekProperties deepSeekProperties,
                                        RedisTemplate<String, Object> redisTemplate,
                                        ObjectMapper objectMapper) {
        this.exerciseItemMapper = exerciseItemMapper;
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
    public ExerciseGuidanceVO getGuidance(Long exerciseId) {
        // 查缓存
        String cacheKey = CACHE_PREFIX + exerciseId;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached.toString(), ExerciseGuidanceVO.class);
            } catch (Exception ignored) {}
        }

        // DB已有AI指导则直接返回
        ExerciseItem item = exerciseItemMapper.selectById(exerciseId);
        if (item == null) {
            throw new BusinessException(404, "运动项目不存在");
        }
        if (item.getAiGuidance() != null && !item.getAiGuidance().isEmpty()) {
            try {
                ExerciseGuidanceVO vo = objectMapper.readValue(item.getAiGuidance(), ExerciseGuidanceVO.class);
                vo.setExerciseName(item.getName());
                return vo;
            } catch (Exception ignored) {}
        }

        // 调用AI生成指导
        if (costMonitor.isGlobalCostExceeded()) {
            return generateFallback(item);
        }

        try {
            String prompt = String.format(
                    "请为运动项目\"%s\"（类型：%s，难度：%s）生成专业的动作指导。" +
                            "严格输出JSON格式：{" +
                            "\"basicInfo\":{\"type\":\"%s\",\"targetMuscle\":\"目标肌群名称\",\"difficulty\":\"%s\"}," +
                            "\"breathing\":\"呼吸节奏说明\"," +
                            "\"steps\":[\"步骤1\",\"步骤2\",\"步骤3\",\"步骤4\"]," +
                            "\"commonMistakes\":[\"错误1\",\"错误2\",\"错误3\"]," +
                            "\"tips\":\"安全提示(2-3条)\"}。只输出JSON。",
                    item.getName(), item.getType(), item.getDifficulty() != null ? item.getDifficulty() : "初级",
                    item.getType() != null ? item.getType() : "综合",
                    item.getDifficulty() != null ? item.getDifficulty() : "初级");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", deepSeekProperties.getModel());
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", "你是专业健身教练。"),
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("response_format", Map.of("type", "json_object"));

            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(response);
            int inputTokens = root.path("usage").path("prompt_tokens").asInt();
            int outputTokens = root.path("usage").path("completion_tokens").asInt();
            costMonitor.recordCall(inputTokens, outputTokens);

            String content = root.path("choices").get(0).path("message").path("content").asText();
            JsonNode guidanceJson = objectMapper.readTree(content);

            ExerciseGuidanceVO vo = new ExerciseGuidanceVO();
            vo.setExerciseName(item.getName());

            // 解析 basicInfo
            JsonNode basicInfoNode = guidanceJson.path("basicInfo");
            if (!basicInfoNode.isMissingNode()) {
                ExerciseGuidanceVO.BasicInfo basicInfo = new ExerciseGuidanceVO.BasicInfo();
                basicInfo.setType(basicInfoNode.path("type").asText(item.getType()));
                basicInfo.setTargetMuscle(basicInfoNode.path("targetMuscle").asText("全身"));
                basicInfo.setDifficulty(basicInfoNode.path("difficulty").asText("初级"));
                vo.setBasicInfo(basicInfo);
            }

            vo.setBreathing(guidanceJson.path("breathing").asText("用力时呼气，放松时吸气"));

            // 解析 steps 数组
            JsonNode stepsNode = guidanceJson.path("steps");
            if (stepsNode.isArray()) {
                vo.setSteps(objectMapper.convertValue(stepsNode, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}));
            } else if (stepsNode.isTextual()) {
                vo.setSteps(List.of(stepsNode.asText().split("[;；\n]")));
            }

            // 解析 commonMistakes 数组
            JsonNode mistakesNode = guidanceJson.path("commonMistakes");
            if (mistakesNode.isArray()) {
                vo.setCommonMistakes(objectMapper.convertValue(mistakesNode, new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {}));
            } else if (mistakesNode.isTextual()) {
                vo.setCommonMistakes(List.of(mistakesNode.asText().split("[;；\n]")));
            }

            vo.setTips(guidanceJson.path("tips").asText("运动前充分热身，如有不适立即停止"));

            // 缓存到Redis
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(vo), 30, TimeUnit.DAYS);

            // 回写到DB
            item.setAiGuidance(objectMapper.writeValueAsString(vo));
            exerciseItemMapper.updateById(item);

            return vo;
        } catch (Exception e) {
            log.error("生成运动指导失败", e);
            return generateFallback(item);
        }
    }

    private ExerciseGuidanceVO generateFallback(ExerciseItem item) {
        ExerciseGuidanceVO vo = new ExerciseGuidanceVO();
        vo.setExerciseName(item.getName());
        vo.setBreathing("用力时呼气，放松时吸气，保持呼吸均匀。");
        vo.setCommonMistakes(List.of("动作过快", "姿势不正确", "呼吸不规律"));
        vo.setTips("运动前充分热身，如有不适立即停止");
        return vo;
    }
}