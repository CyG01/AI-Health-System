package com.example.service;

import com.example.common.BusinessException;
import com.example.entity.AiCallAuditLog;
import com.example.entity.ExerciseRule;
import com.example.llmops.PrometheusMetricsExporter;
import com.example.mapper.AiCallAuditLogMapper;
import com.example.mapper.ExerciseRuleMapper;
import com.example.model.FunctionCallResult;
import com.example.model.ToolDefinition;
import com.example.model.ToolExecutor;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.properties.DeepSeekProperties;
import com.example.util.AiResponseParser;
import com.example.util.DataMaskingService;
import com.example.util.MedicalDisclaimerFilter;
import com.example.util.PromptSanitizer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DeepSeekService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekService.class);

    private static final String PROMPT_TEMPLATE =
            "用户身高: %.1f cm, 体重: %.1f kg, 健康目标: %s, 计划持续: %d 天. 偏好: %s. " +
            "用户画像: %s. " +
            "请为该用户生成分天的个性化%s计划, 每天2-5个任务项, 格式为严格的JSON: " +
            "{\"days\":[{\"d\":天数, \"items\":[\"具体任务描述1\", \"具体任务描述2\"]}]}. " +
            "任务要具体、可执行, 包含具体数值(时长/组数/重量/食物克数等). " +
            "%s 仅输出JSON.";

    private static final String DONE_MARKER = "[DONE]";

    private final WebClient webClient;
    private final DeepSeekProperties properties;
    private final ObjectMapper objectMapper;
    private final DeepSeekCostMonitor costMonitor;
    private final PrometheusMetricsExporter prometheusMetrics;
    private final DataMaskingService dataMaskingService;
    private final MedicalDisclaimerFilter disclaimerFilter;
    private final AiCallAuditLogMapper auditLogMapper;
    private final ExerciseRuleMapper exerciseRuleMapper;

    public DeepSeekService(DeepSeekProperties properties, ObjectMapper objectMapper,
                           DeepSeekCostMonitor costMonitor,
                           PrometheusMetricsExporter prometheusMetrics,
                           DataMaskingService dataMaskingService,
                           MedicalDisclaimerFilter disclaimerFilter,
                           AiCallAuditLogMapper auditLogMapper,
                           ExerciseRuleMapper exerciseRuleMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.costMonitor = costMonitor;
        this.prometheusMetrics = prometheusMetrics;
        this.dataMaskingService = dataMaskingService;
        this.disclaimerFilter = disclaimerFilter;
        this.auditLogMapper = auditLogMapper;
        this.exerciseRuleMapper = exerciseRuleMapper;
        this.webClient = WebClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @RateLimiter(name = "ai-api")
    @Bulkhead(name = "ai-api")
    @Retry(name = "deepseek", fallbackMethod = "deepSeekFallback")
    @CircuitBreaker(name = "deepseek", fallbackMethod = "deepSeekFallback")
    public String callApi(BigDecimal height, BigDecimal weight, String goal,
                          Integer durationDays, String preference, String userProfile,
                          String planTypeLabel, String planTypeNote) {
        return callApi(height, weight, goal, durationDays, preference, userProfile,
                planTypeLabel, planTypeNote, properties.getModel(), null);
    }

    /**
     * 新增 userId 参数，用于审计日志记录
     */
    public String callApi(BigDecimal height, BigDecimal weight, String goal,
                          Integer durationDays, String preference, String userProfile,
                          String planTypeLabel, String planTypeNote, String model) {
        return callApi(height, weight, goal, durationDays, preference, userProfile,
                planTypeLabel, planTypeNote, model, null);
    }

    @RateLimiter(name = "ai-api")
    @Bulkhead(name = "ai-api")
    @Retry(name = "deepseek", fallbackMethod = "deepSeekFallback")
    @CircuitBreaker(name = "deepseek", fallbackMethod = "deepSeekFallback")
    public String callApi(BigDecimal height, BigDecimal weight, String goal,
                          Integer durationDays, String preference, String userProfile,
                          String planTypeLabel, String planTypeNote, String model, Long userId) {
        long startTime = System.currentTimeMillis();
        AiCallAuditLog auditLog = new AiCallAuditLog();
        auditLog.setUserId(userId);
        auditLog.setCallType("plan_generate");
        auditLog.setModelName(model != null ? model : properties.getModel());
        auditLog.setCreatedAt(LocalDateTime.now());

        if (costMonitor.isGlobalCostExceeded()) {
            auditLog.setSuccess(false);
            auditLog.setErrorMessage("今日AI调用额度已用尽");
            auditLogMapper.insert(auditLog);
            throw new BusinessException("今日AI调用额度已用尽，请明天再试");
        }

        // Prompt 注入防护 + 数据脱敏
        String sanitizedGoal = PromptSanitizer.sanitize(goal);
        String sanitizedPreference = PromptSanitizer.sanitize(preference);
        String maskedUserProfile = dataMaskingService.maskUserProfile(userProfile);
        String sanitizedLabel = PromptSanitizer.sanitize(planTypeLabel);
        String sanitizedNote = PromptSanitizer.sanitize(planTypeNote);

        String prompt = String.format(PROMPT_TEMPLATE, height, weight, sanitizedGoal, durationDays,
                sanitizedPreference, maskedUserProfile, sanitizedLabel, sanitizedNote);

        auditLog.setPromptUsed(prompt);
        auditLog.setRequestParams(String.format("height=%s,weight=%s,durationDays=%d", height, weight, durationDays));

        Map<String, Object> requestBody = Map.of(
                "model", model != null ? model : properties.getModel(),
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
                auditLog.setSuccess(false);
                auditLog.setErrorMessage("AI返回内容为空");
                auditLogMapper.insert(auditLog);
                throw new BusinessException("AI返回内容为空");
            }

            // 验证 JSON 可解析（防御性加固 + 失败重试）
            try {
                AiResponseParser.extractJson(content);
            } catch (BusinessException parseEx) {
                log.warn("JSON解析失败，尝试纠正重试 originalError={}", parseEx.getMessage());
                // 重试: 追加纠正指令让模型重新输出
                try {
                    String correctionPrompt = "Your previous response was not valid JSON. "
                            + "Please output ONLY valid JSON, no other text. "
                            + "Original request: " + prompt;
                    String retryContent = callLlmForJsonCorrection(correctionPrompt, model);
                    AiResponseParser.extractJson(retryContent); // 最终验证
                    content = retryContent; // 用修正后的内容替换
                } catch (BusinessException retryEx) {
                    auditLog.setAiRawResponse(content);
                    auditLog.setSuccess(false);
                    auditLog.setErrorMessage("JSON解析重试仍失败: " + retryEx.getMessage());
                    auditLogMapper.insert(auditLog);
                    throw retryEx;
                }
            }

            // 审计日志记录
            auditLog.setAiRawResponse(content.length() > 4000 ? content.substring(0, 4000) : content);
            auditLog.setInputTokens(inputTokens);
            auditLog.setOutputTokens(outputTokens);
            auditLog.setLatencyMs((int) (System.currentTimeMillis() - startTime));
            auditLog.setSuccess(true);
            auditLogMapper.insert(auditLog);

            // 追加医疗免责声明
            String result = disclaimerFilter.appendDisclaimer(content);

            // 记录 Prometheus 指标
            prometheusMetrics.recordAiCall(auditLog.getCallType(),
                    auditLog.getModelName(), true, System.currentTimeMillis() - startTime);
            prometheusMetrics.recordTokenConsumption(auditLog.getModelName(),
                    inputTokens, outputTokens);

            return result;
        } catch (BusinessException e) {
            prometheusMetrics.recordAiCall(auditLog.getCallType(),
                    auditLog.getModelName(), false, System.currentTimeMillis() - startTime);
            auditLog.setSuccess(false);
            auditLog.setErrorMessage(e.getMessage());
            auditLogMapper.insert(auditLog);
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek API调用异常", e);
            prometheusMetrics.recordAiCall(auditLog.getCallType(),
                    auditLog.getModelName(), false, System.currentTimeMillis() - startTime);
            auditLog.setSuccess(false);
            auditLog.setErrorMessage("AI服务调用失败: " + e.getMessage());
            auditLogMapper.insert(auditLog);
            throw new BusinessException("AI服务调用失败");
        }
    }

    public Flux<String> callApiStream(BigDecimal height, BigDecimal weight, String goal,
                                      Integer durationDays, String preference, String userProfile,
                                      String planTypeLabel, String planTypeNote, String model) {
        return callApiStream(height, weight, goal, durationDays, preference, userProfile,
                planTypeLabel, planTypeNote, model, null);
    }

    public Flux<String> callApiStream(BigDecimal height, BigDecimal weight, String goal,
                                      Integer durationDays, String preference, String userProfile,
                                      String planTypeLabel, String planTypeNote, String model, Long userId) {
        if (costMonitor.isGlobalCostExceeded()) {
            return Flux.error(new BusinessException("今日AI调用额度已用尽，请明天再试"));
        }

        // Prompt 注入防护 + 数据脱敏
        String sanitizedGoal = PromptSanitizer.sanitize(goal);
        String sanitizedPreference = PromptSanitizer.sanitize(preference);
        String maskedUserProfile = dataMaskingService.maskUserProfile(userProfile);
        String sanitizedLabel = PromptSanitizer.sanitize(planTypeLabel);
        String sanitizedNote = PromptSanitizer.sanitize(planTypeNote);

        String prompt = String.format(PROMPT_TEMPLATE, height, weight, sanitizedGoal, durationDays,
                sanitizedPreference, maskedUserProfile, sanitizedLabel, sanitizedNote);

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
        return generateFallbackPlan(height, weight, goal, durationDays, preference);
    }

    /**
     * 降级方案智能化 —— 基于规则表 + BMI 匹配
     */
    private String generateFallbackPlan(BigDecimal height, BigDecimal weight,
                                         String goal, int durationDays, String preference) {
        double w = weight.doubleValue();
        double h = height.doubleValue();
        double bmi = w / ((h / 100) * (h / 100));
        BigDecimal bmiDecimal = BigDecimal.valueOf(bmi).setScale(1, RoundingMode.HALF_UP);

        // 从规则表匹配推荐运动
        List<ExerciseRule> rules = exerciseRuleMapper.matchByGoalAndBmi(goal, bmiDecimal.doubleValue());

        StringBuilder json = new StringBuilder("{\"days\":[");

        for (int d = 1; d <= durationDays; d++) {
            if (d > 1) json.append(",");
            json.append("{\"d\":").append(d).append(",\"items\":[");

            if (rules != null && !rules.isEmpty()) {
                // 从规则中轮换选取运动
                ExerciseRule rule = rules.get(d % rules.size());
                json.append("\"").append(rule.getExerciseName())
                    .append(" ").append(rule.getDefaultDuration()).append("分钟（")
                    .append(rule.getDefaultIntensity()).append("强度）\"");

                // 如果规则 >1 条，追加第二条运动
                if (rules.size() > 1) {
                    ExerciseRule rule2 = rules.get((d + 1) % rules.size());
                    if (!rule2.getId().equals(rule.getId())) {
                        json.append(",\"").append(rule2.getExerciseName())
                            .append(" ").append(rule2.getDefaultDuration()).append("分钟（")
                            .append(rule2.getDefaultIntensity()).append("强度）\"");
                    }
                }
            } else {
                // 无匹配规则时回退到通用建议
                int exerciseMin = 30 + (d % 3) * 5;
                String exerciseTip = "快走或慢跑" + exerciseMin + "分钟（中等强度）";
                json.append("\"").append(exerciseTip).append("\"");
            }

            int calorieTarget = (int)(1500 + w * 5);
            String mealTip = bmi > 25 ? "控制碳水摄入，增加蔬菜比例" : "均衡饮食，保证蛋白质摄入";
            json.append(",\"").append(mealTip).append("\",")
                .append("\"每日目标热量").append(calorieTarget).append("kcal\"");
            json.append("]}");
        }
        json.append("]}");
        return json.toString();
    }

    /**
     * 通用对话接口（用于健康建议等非 JSON 场景）
     */
    @RateLimiter(name = "ai-api")
    @Bulkhead(name = "ai-api")
    @Retry(name = "deepseek", fallbackMethod = "chatFallback")
    @CircuitBreaker(name = "deepseek", fallbackMethod = "chatFallback")
    public String chat(String userPrompt) {
        if (costMonitor.isGlobalCostExceeded()) {
            throw new BusinessException("今日AI调用额度已用尽，请明天再试");
        }

        // 输入脱敏
        String sanitizedPrompt = PromptSanitizer.sanitize(userPrompt);

        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "你是一个专业的健康顾问，请根据用户提供的健康数据给出个性化的改善建议。回答简洁实用，不超过200字。"),
                        Map.of("role", "user", "content", sanitizedPrompt)
                )
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
            return disclaimerFilter.appendDisclaimer(content);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek API调用异常", e);
            throw new BusinessException("AI服务调用失败");
        }
    }

    @SuppressWarnings("unused")
    private String chatFallback(String userPrompt, Throwable t) {
        log.error("AI聊天降级 prompt={}", userPrompt, t);
        throw new BusinessException("AI服务暂不可用，请稍后再试");
    }

    /**
     * JSON 解析失败时，发送纠正 prompt 让 LLM 重新输出有效 JSON。
     * 最多重试 1 次，不做 Resilience4j 注解包装（内部调用）。
     */
    private String callLlmForJsonCorrection(String correctionPrompt, String model) {
        Map<String, Object> requestBody = Map.of(
                "model", model != null ? model : properties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "You are a JSON-only responder. Output ONLY valid JSON, no markdown, no explanation."),
                        Map.of("role", "user", "content", correctionPrompt)
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
                throw new BusinessException("纠正重试AI返回内容为空");
            }
            log.info("JSON纠正重试成功 contentLength={}", content.length());
            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("JSON纠正重试调用失败", e);
            throw new BusinessException("JSON纠正重试调用失败: " + e.getMessage());
        }
    }

    /**
     * 通用调用接口：传入自定义prompt，返回AI原始响应内容
     */
    @RateLimiter(name = "ai-api")
    @Bulkhead(name = "ai-api")
    @Retry(name = "deepseek", fallbackMethod = "callApiRawFallback")
    @CircuitBreaker(name = "deepseek", fallbackMethod = "callApiRawFallback")
    public String callApiRaw(String prompt) {
        if (costMonitor.isGlobalCostExceeded()) {
            throw new BusinessException("今日AI调用额度已用尽，请明天再试");
        }

        // 输入脱敏
        String sanitizedPrompt = PromptSanitizer.sanitize(prompt);

        Map<String, Object> requestBody = Map.of(
                "model", properties.getModel(),
                "messages", List.of(
                        Map.of("role", "user", "content", sanitizedPrompt)
                )
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
            return disclaimerFilter.appendDisclaimer(content);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("DeepSeek API调用异常", e);
            throw new BusinessException("AI服务调用失败");
        }
    }

    @SuppressWarnings("unused")
    private String callApiRawFallback(String prompt, Throwable t) {
        log.error("AI通用调用降级", t);
        throw new BusinessException("AI服务暂不可用，请稍后再试");
    }

    // ==================== Function Calling ====================

    /**
     * Function Calling 完整调用循环（非流式）。
     * <p>
     * 流程：
     * 1. 将 system/user messages + tool definitions 发送给 DeepSeek
     * 2. 如果模型返回 tool_calls（finish_reason=tool_calls），提取函数名+参数，
     *    通过 injectedExecutor 本地执行，将结果追加为 tool role message
     * 3. 再次调用获取最终文本回复
     * 4. 最多循环 maxToolCallRounds 次（默认 5），防止无限循环
     * <p>
     * DeepSeek 的 Function Calling 完全兼容 OpenAI 协议，包括：
     * - parallel_tool_calls（并行调用多个工具）
     * - strict mode（JSON Schema 严格模式）
     * - tool_choice（指定工具选择策略）
     *
     * @param systemPrompt   系统提示词
     * @param userMessage    用户消息
     * @param tools          工具定义列表
     * @param toolExecutor   本地工具执行器（由调用方注入调度逻辑）
     * @param maxToolCallRounds 最大工具调用轮数（防无限循环）
     * @return FunctionCallResult（含工具调用记录 + 最终文本）
     */
    public FunctionCallResult functionCall(String systemPrompt, String userMessage,
                                            List<ToolDefinition> tools,
                                            ToolExecutor toolExecutor,
                                            int maxToolCallRounds) {
        if (costMonitor.isGlobalCostExceeded()) {
            throw new BusinessException("今日AI调用额度已用尽，请明天再试");
        }

        int totalInputTokens = 0;
        int totalOutputTokens = 0;
        List<FunctionCallResult.ToolCallRequest> allToolCalls = new ArrayList<>();

        // 构建 messages 列表（可追加 tool 结果）
        List<Map<String, Object>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", PromptSanitizer.sanitize(systemPrompt)));
        }
        messages.add(Map.of("role", "user", "content", PromptSanitizer.sanitize(userMessage)));

        // 构建工具定义 JSON（序列化为 DeepSeek/OpenAI 兼容格式）
        List<Map<String, Object>> toolDefs = buildToolDefinitions(tools);

        int rounds = Math.max(1, Math.min(maxToolCallRounds, 10));

        for (int round = 0; round < rounds; round++) {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", properties.getModel());
            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.3);

            if (toolDefs != null && !toolDefs.isEmpty()) {
                requestBody.put("tools", toolDefs);
                // tool_choice: auto = 让模型自行决定是否调用工具
                requestBody.put("tool_choice", "auto");
            }

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
                totalInputTokens += inputTokens;
                totalOutputTokens += outputTokens;
                costMonitor.recordCall(inputTokens, outputTokens);

                JsonNode choice = root.path("choices").get(0);
                String finishReason = choice.path("finish_reason").asText();
                JsonNode message = choice.path("message");

                // 检查是否有 tool_calls
                JsonNode toolCallsNode = message.path("tool_calls");
                if (toolCallsNode.isArray() && toolCallsNode.size() > 0
                        && "tool_calls".equals(finishReason)) {
                    // 本轮需要执行工具
                    List<FunctionCallResult.ToolCallRequest> roundCalls = extractToolCalls(toolCallsNode);
                    allToolCalls.addAll(roundCalls);

                    // 将 assistant 消息（含 tool_calls）加入历史
                    messages.add(buildAssistantToolCallMessage(roundCalls));

                    // 执行每个工具调用，将结果追加为 tool role 消息
                    for (FunctionCallResult.ToolCallRequest tc : roundCalls) {
                        String toolResult;
                        try {
                            toolResult = toolExecutor.execute(tc.getId(), tc.getName(), tc.getArguments());
                        } catch (Exception e) {
                            log.error("Tool执行失败 tool={} id={}", tc.getName(), tc.getId(), e);
                            toolResult = "{\"error\":\"" + escapeJson(e.getMessage()) + "\"}";
                        }
                        messages.add(Map.of(
                                "role", "tool",
                                "tool_call_id", tc.getId(),
                                "content", toolResult
                        ));
                    }
                    // 继续下一轮，让模型根据工具结果生成最终回复
                    continue;
                }

                // 最终文本回复
                String content = message.path("content").asText();
                if (content == null || content.isBlank()) {
                    content = "模型未返回有效内容";
                }

                return FunctionCallResult.builder()
                        .content(disclaimerFilter.appendDisclaimer(content))
                        .toolCalls(allToolCalls)
                        .inputTokens(totalInputTokens)
                        .outputTokens(totalOutputTokens)
                        .finished(true)
                        .rawResponse(response)
                        .build();

            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                log.error("DeepSeek Function Calling 异常 round={}", round, e);
                throw new BusinessException("AI Function Calling 调用失败: " + e.getMessage());
            }
        }

        // 超过最大轮数未 finish
        throw new BusinessException("AI 工具调用超过最大轮数限制（" + rounds + "），请简化问题后重试");
    }

    /**
     * 简化版 Function Calling（单个 system prompt + user message）。
     */
    public FunctionCallResult functionCall(String systemPrompt, String userMessage,
                                            List<ToolDefinition> tools,
                                            ToolExecutor toolExecutor) {
        return functionCall(systemPrompt, userMessage, tools, toolExecutor, 5);
    }

    // ---- Function Calling 内部辅助方法 ----

    /**
     * 将 ToolDefinition 列表序列化为 DeepSeek/OpenAI 兼容的 tools 数组格式。
     */
    List<Map<String, Object>> buildToolDefinitions(List<ToolDefinition> tools) {
        if (tools == null || tools.isEmpty()) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (ToolDefinition td : tools) {
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("type", td.getType() != null ? td.getType() : "function");

            ToolDefinition.FunctionDef func = td.getFunction();
            Map<String, Object> funcMap = new LinkedHashMap<>();
            funcMap.put("name", func.getName());
            funcMap.put("description", func.getDescription());
            if (func.isStrict()) {
                funcMap.put("strict", true);
            }

            // 序列化 parameters (JSON Schema)
            ToolDefinition.Parameters params = func.getParameters();
            if (params != null) {
                Map<String, Object> paramsMap = new LinkedHashMap<>();
                paramsMap.put("type", params.getType() != null ? params.getType() : "object");

                Map<String, Object> propsMap = new LinkedHashMap<>();
                if (params.getProperties() != null) {
                    for (var entry : params.getProperties().entrySet()) {
                        propsMap.put(entry.getKey(), entry.getValue().toMap());
                    }
                }
                paramsMap.put("properties", propsMap);

                if (params.getRequired() != null && !params.getRequired().isEmpty()) {
                    paramsMap.put("required", params.getRequired());
                }
                if (!params.isAdditionalProperties()) {
                    paramsMap.put("additionalProperties", false);
                }
                funcMap.put("parameters", paramsMap);
            }

            def.put("function", funcMap);
            result.add(def);
        }
        return result;
    }

    /**
     * 从 API 响应的 tool_calls JSON 中提取 ToolCallRequest 列表。
     */
    List<FunctionCallResult.ToolCallRequest> extractToolCalls(JsonNode toolCallsNode) {
        List<FunctionCallResult.ToolCallRequest> result = new ArrayList<>();
        for (JsonNode tc : toolCallsNode) {
            String id = tc.path("id").asText();
            String type = tc.path("type").asText();
            // 只处理 function 类型（DeepSeek 当前仅支持 function）
            if (!"function".equals(type) && !type.isEmpty()) {
                continue;
            }
            JsonNode funcNode = tc.path("function");
            String name = funcNode.path("name").asText();
            String arguments = funcNode.path("arguments").asText();

            JsonNode argsNode = null;
            if (arguments != null && !arguments.isBlank()) {
                try {
                    argsNode = objectMapper.readTree(arguments);
                } catch (JsonProcessingException e) {
                    log.warn("Tool参数JSON解析失败 tool={} args={}", name, arguments);
                }
            }

            result.add(FunctionCallResult.ToolCallRequest.builder()
                    .id(id)
                    .name(name)
                    .arguments(arguments)
                    .argumentsNode(argsNode)
                    .build());
        }
        return result;
    }

    /**
     * 构建 assistant 消息（含 tool_calls），用于追加到 messages 历史。
     */
    Map<String, Object> buildAssistantToolCallMessage(
            List<FunctionCallResult.ToolCallRequest> toolCalls) {
        List<Map<String, Object>> tcList = new ArrayList<>();
        for (var tc : toolCalls) {
            Map<String, Object> tcMap = new LinkedHashMap<>();
            tcMap.put("id", tc.getId());
            tcMap.put("type", "function");
            tcMap.put("function", Map.of(
                    "name", tc.getName(),
                    "arguments", tc.getArguments() != null ? tc.getArguments() : "{}"
            ));
            tcList.add(tcMap);
        }
        return Map.of("role", "assistant", "tool_calls", tcList);
    }

    /**
     * JSON 字符串转义（用于错误信息嵌入）。
     */
    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}