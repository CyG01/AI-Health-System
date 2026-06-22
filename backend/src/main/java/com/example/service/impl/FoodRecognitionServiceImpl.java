package com.example.service.impl;

import com.example.common.BusinessException;
import com.example.entity.AiCallAuditLog;
import com.example.entity.FoodItem;
import com.example.event.FoodRecognizedEvent;
import com.example.mapper.AiCallAuditLogMapper;
import com.example.mapper.FoodItemMapper;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.properties.DeepSeekProperties;
import com.example.service.FoodRecognitionService;
import com.example.util.AiResponseParser;
import com.example.util.ImageCompressor;
import com.example.util.MedicalDisclaimerFilter;
import com.example.util.PromptSanitizer;
import com.example.vo.FoodRecognizeVO;
import com.example.vo.FoodTextRecognizeVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
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
    private final AiCallAuditLogMapper auditLogMapper;
    private final MedicalDisclaimerFilter disclaimerFilter;
    private final ApplicationEventPublisher eventPublisher;

    public FoodRecognitionServiceImpl(DeepSeekCostMonitor costMonitor,
                                       DeepSeekProperties deepSeekProperties,
                                       FoodItemMapper foodItemMapper,
                                       ObjectMapper objectMapper,
                                       AiCallAuditLogMapper auditLogMapper,
                                       MedicalDisclaimerFilter disclaimerFilter,
                                       ApplicationEventPublisher eventPublisher) {
        this.costMonitor = costMonitor;
        this.deepSeekProperties = deepSeekProperties;
        this.foodItemMapper = foodItemMapper;
        this.objectMapper = objectMapper;
        this.auditLogMapper = auditLogMapper;
        this.disclaimerFilter = disclaimerFilter;
        this.eventPublisher = eventPublisher;
        this.webClient = WebClient.builder()
                .baseUrl(deepSeekProperties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + deepSeekProperties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(HttpClient.create()
                        .responseTimeout(Duration.ofSeconds(30))
                        .option(io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)))
                .build();
    }

    @Override
    public FoodRecognizeVO recognize(MultipartFile image, Long userId) {
        long startTime = System.currentTimeMillis();
        AiCallAuditLog auditLog = new AiCallAuditLog();
        auditLog.setCallType("food_recognize");
        auditLog.setModelName("deepseek-vision");
        auditLog.setCreatedAt(LocalDateTime.now());

        if (costMonitor.isGlobalCostExceeded()) {
            auditLog.setSuccess(false);
            auditLog.setErrorMessage("今日AI调用额度已用尽");
            auditLogMapper.insert(auditLog);
            throw new BusinessException("今日AI调用额度已用尽，请明天再试");
        }

        try {
            // 图片压缩+转Base64（防止OOM）
            byte[] compressedBytes = ImageCompressor.compress(image);
            String base64Image = Base64.getEncoder().encodeToString(compressedBytes);
            auditLog.setRequestParams("image_size_original=" + image.getSize() + ",compressed=" + compressedBytes.length);

            // 食物识别 prompt（注入防护）
            String prompt = "请识别图片中的食物，并估算每100克的热量（kcal）、蛋白质、碳水、脂肪。" +
                    "严格按照JSON格式输出：{\"foodName\":\"名称\",\"caloriePer100g\":数字,\"proteinPer100g\":数字,\"carbsPer100g\":数字,\"fatPer100g\":数字,\"category\":\"分类\",\"confidence\":置信度0-100}。" +
                    "如果无法识别，返回{\"error\":\"无法识别图片内容\"}。只输出JSON不要其他内容。";
            auditLog.setPromptUsed(prompt);

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
                    auditLog.setSuccess(false);
                    auditLog.setErrorMessage("AI返回内容为空");
                    auditLogMapper.insert(auditLog);
                    throw new BusinessException("AI返回内容为空");
                }

                // 使用 AiResponseParser 防御性解析 JSON（含失败重试）
                JsonNode contentJson;
                try {
                    contentJson = AiResponseParser.extractJson(content);
                } catch (BusinessException parseEx) {
                    log.warn("食物识别JSON解析失败，尝试纠正重试 error={}", parseEx.getMessage());
                    try {
                        String retryContent = retryLlmWithJsonCorrection(requestBody, content);
                        contentJson = AiResponseParser.extractJson(retryContent);
                    } catch (BusinessException retryEx) {
                        auditLog.setSuccess(false);
                        auditLog.setErrorMessage("JSON解析重试仍失败: " + retryEx.getMessage());
                        auditLogMapper.insert(auditLog);
                        throw new BusinessException("食物识别结果解析失败，请重试");
                    }
                }

                auditLog.setAiRawResponse(content.length() > 4000 ? content.substring(0, 4000) : content);
                auditLog.setInputTokens(inputTokens);
                auditLog.setOutputTokens(outputTokens);
                auditLog.setLatencyMs((int) (System.currentTimeMillis() - startTime));

                // 检查是否有错误
                if (contentJson.has("error")) {
                    auditLog.setSuccess(false);
                    auditLog.setErrorMessage(contentJson.get("error").asText());
                    auditLogMapper.insert(auditLog);
                    throw new BusinessException(contentJson.get("error").asText());
                }

                FoodRecognizeVO vo = new FoodRecognizeVO();
                // 食物名称做注入防护
                String foodName = contentJson.path("foodName").asText();
                vo.setFoodName(PromptSanitizer.sanitize(foodName));
                vo.setCaloriePer100g(contentJson.path("caloriePer100g").asInt());
                vo.setProteinPer100g(BigDecimal.valueOf(contentJson.path("proteinPer100g").asDouble()));
                vo.setCarbsPer100g(BigDecimal.valueOf(contentJson.path("carbsPer100g").asDouble()));
                vo.setFatPer100g(BigDecimal.valueOf(contentJson.path("fatPer100g").asDouble()));
                vo.setCategory(PromptSanitizer.sanitize(contentJson.path("category").asText("主食")));
                vo.setConfidence(contentJson.path("confidence").asInt(80));
                vo.setRecommendedGrams(calculateRecommended(vo));

                // 尝试匹配本地食物数据库
                matchLocalFood(vo);

                // 发布食物识别完成事件（异步触发饮食记录、热量超标检测等）
                publishRecognizedEvent(vo, userId);

                auditLog.setParsedResult("foodName=" + vo.getFoodName() + ",calorie=" + vo.getCaloriePer100g());
                auditLog.setSuccess(true);
                auditLogMapper.insert(auditLog);

                return vo;
            }

            auditLog.setSuccess(false);
            auditLog.setErrorMessage("AI识别失败");
            auditLogMapper.insert(auditLog);
            throw new BusinessException("AI识别失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("食物识别异常", e);
            auditLog.setSuccess(false);
            auditLog.setErrorMessage("食物识别失败: " + e.getMessage());
            auditLogMapper.insert(auditLog);
            throw new BusinessException("食物识别失败，请重试");
        }
    }

    /**
     * JSON 解析失败时，追加纠正消息重新调用 LLM，要求输出有效 JSON。
     * 最多重试 1 次。
     */
    private String retryLlmWithJsonCorrection(Map<String, Object> originalRequestBody,
                                               String failedContent) {
        // 构建纠正请求：在原始 messages 基础上追加 assistant 失败回复 + user 纠正指令
        List<Map<String, Object>> correctedMessages = new java.util.ArrayList<>();
        Object origMessages = originalRequestBody.get("messages");
        if (origMessages instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> msgs = (List<Map<String, Object>>) origMessages;
            correctedMessages.addAll(msgs);
        }
        // 模拟 assistant 的失败回复
        correctedMessages.add(Map.of("role", "assistant", "content", failedContent));
        // 追加纠正指令
        correctedMessages.add(Map.of("role", "user", "content",
                "Your previous response was not valid JSON. Please output ONLY valid JSON, no other text."));

        Map<String, Object> retryBody = new HashMap<>(originalRequestBody);
        retryBody.put("messages", correctedMessages);

        try {
            String response = webClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(retryBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                int inputTokens = root.path("usage").path("prompt_tokens").asInt();
                int outputTokens = root.path("usage").path("completion_tokens").asInt();
                costMonitor.recordCall(inputTokens, outputTokens);

                String content = root.path("choices").get(0).path("message").path("content").asText();
                if (content != null && !content.isBlank()) {
                    log.info("食物识别JSON纠正重试成功 contentLength={}", content.length());
                    return content;
                }
            }
            throw new BusinessException("纠正重试返回内容为空");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("食物识别纠正重试调用失败", e);
            throw new BusinessException("纠正重试调用失败: " + e.getMessage());
        }
    }

    private int calculateRecommended(FoodRecognizeVO vo) {
        String category = vo.getCategory();
        if ("主食".equals(category)) return 100;
        if ("蛋白质".equals(category)) return 150;
        if ("蔬菜".equals(category)) return 200;
        if ("水果".equals(category)) return 150;
        if ("油脂".equals(category)) return 15;
        return 100;
    }

    private void matchLocalFood(FoodRecognizeVO vo) {
        String name = vo.getFoodName().trim();
        List<FoodItem> matched = foodItemMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FoodItem>()
                        .like(FoodItem::getName, name)
                        .last("limit 5")
        );
        if (!matched.isEmpty()) {
            FoodItem item = matched.get(0);
            vo.setCaloriePer100g(item.getCaloriePer100g());
            vo.setProteinPer100g(item.getProteinPer100g());
            vo.setCarbsPer100g(item.getCarbsPer100g());
            vo.setFatPer100g(item.getFatPer100g());
            vo.setCategory(item.getCategory());
        }
    }

    private void publishRecognizedEvent(FoodRecognizeVO vo, Long userId) {
        if (userId == null) return;
        try {
            FoodRecognizedEvent event = new FoodRecognizedEvent(
                    this, userId,
                    vo.getFoodName(),
                    vo.getCaloriePer100g() != null ? vo.getCaloriePer100g() : 0,
                    vo.getProteinPer100g() != null ? vo.getProteinPer100g().intValue() : 0,
                    vo.getCarbsPer100g() != null ? vo.getCarbsPer100g().intValue() : 0,
                    vo.getFatPer100g() != null ? vo.getFatPer100g().intValue() : 0,
                    vo.getCategory(),
                    vo.getRecommendedGrams() != null ? vo.getRecommendedGrams() : 100
            );
            eventPublisher.publishEvent(event);
            log.info("发布食物识别事件 userId={} food={}", userId, vo.getFoodName());
        } catch (Exception e) {
            log.error("发布食物识别事件失败 userId={}", userId, e);
        }
    }

    @Override
    public FoodTextRecognizeVO recognizeByText(Long userId, String text) {
        long startTime = System.currentTimeMillis();
        AiCallAuditLog auditLog = new AiCallAuditLog();
        auditLog.setCallType("food_recognize_text");
        auditLog.setModelName("deepseek-chat");
        auditLog.setCreatedAt(LocalDateTime.now());

        if (costMonitor.isGlobalCostExceeded()) {
            auditLog.setSuccess(false);
            auditLog.setErrorMessage("今日AI调用额度已用尽");
            auditLogMapper.insert(auditLog);
            throw new BusinessException("今日AI调用额度已用尽，请明天再试");
        }

        try {
            // 注入防护
            String sanitizedText = PromptSanitizer.sanitize(text);

            String prompt = """
                    你是一个专业的营养分析助手。请解析用户描述的食物为JSON格式。
                    对于每种食物，估算其名称、重量（克）和热量（kcal）。
                    严格按照以下JSON格式输出：
                    {"items":[{"foodName":"食物名称","weightG":估算重量克数,"calories":估算总热量kcal}]}
                    用户描述：%s
                    只输出JSON，不要其他内容。
                    """.formatted(sanitizedText);

            auditLog.setPromptUsed(prompt);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "deepseek-chat");
            requestBody.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("response_format", Map.of("type", "json_object"));

            String response = webClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (response != null) {
                JsonNode root = objectMapper.readTree(response);
                int inputTokens = root.path("usage").path("prompt_tokens").asInt();
                int outputTokens = root.path("usage").path("completion_tokens").asInt();
                costMonitor.recordCall(inputTokens, outputTokens);

                String content = root.path("choices").get(0).path("message").path("content").asText();
                if (content == null || content.isBlank()) {
                    auditLog.setSuccess(false);
                    auditLog.setErrorMessage("AI返回内容为空");
                    auditLogMapper.insert(auditLog);
                    throw new BusinessException("AI返回内容为空");
                }

                // JSON 解析（含失败重试）
                JsonNode contentJson;
                try {
                    contentJson = AiResponseParser.extractJson(content);
                } catch (BusinessException parseEx) {
                    log.warn("文字食物识别JSON解析失败，尝试纠正重试 error={}", parseEx.getMessage());
                    try {
                        String retryContent = retryLlmWithJsonCorrection(requestBody, content);
                        contentJson = AiResponseParser.extractJson(retryContent);
                    } catch (BusinessException retryEx) {
                        auditLog.setSuccess(false);
                        auditLog.setErrorMessage("JSON解析重试仍失败: " + retryEx.getMessage());
                        auditLogMapper.insert(auditLog);
                        throw new BusinessException("食物识别结果解析失败，请重试");
                    }
                }
                JsonNode itemsNode = contentJson.path("items");

                List<FoodTextRecognizeVO.FoodItem> items = new java.util.ArrayList<>();
                if (itemsNode.isArray()) {
                    for (JsonNode itemNode : itemsNode) {
                        String foodName = PromptSanitizer.sanitize(itemNode.path("foodName").asText());
                        int weightG = itemNode.path("weightG").asInt(100);
                        int calories = itemNode.path("calories").asInt(0);

                        // 尝试匹配本地数据库校准热量
                        List<FoodItem> matched = foodItemMapper.selectList(
                                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FoodItem>()
                                        .like(FoodItem::getName, foodName)
                                        .last("limit 1"));
                        if (!matched.isEmpty()) {
                            FoodItem dbItem = matched.get(0);
                            calories = (int) Math.round(dbItem.getCaloriePer100g() * weightG / 100.0);
                        }

                        items.add(FoodTextRecognizeVO.FoodItem.builder()
                                .foodName(foodName)
                                .weightG(weightG)
                                .calories(calories)
                                .build());
                    }
                }

                auditLog.setInputTokens(inputTokens);
                auditLog.setOutputTokens(outputTokens);
                auditLog.setLatencyMs((int) (System.currentTimeMillis() - startTime));
                auditLog.setSuccess(true);
                auditLogMapper.insert(auditLog);

                return FoodTextRecognizeVO.builder().items(items).build();
            }

            auditLog.setSuccess(false);
            auditLog.setErrorMessage("AI识别失败");
            auditLogMapper.insert(auditLog);
            throw new BusinessException("AI识别失败");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("文字食物识别异常", e);
            auditLog.setSuccess(false);
            auditLog.setErrorMessage("文字食物识别失败: " + e.getMessage());
            auditLogMapper.insert(auditLog);
            throw new BusinessException("食物识别失败，请重试");
        }
    }
}