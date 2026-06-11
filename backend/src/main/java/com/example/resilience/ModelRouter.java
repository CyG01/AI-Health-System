package com.example.resilience;

import com.example.billing.BillingService;
import com.example.common.BusinessException;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.monitor.ModelTier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 多模型智能路由器。
 *
 * 策略：
 * 1. 主模型（DeepSeek）健康 → 直接使用
 * 2. 主模型降级 → 按权重选择备选模型
 * 3. 主模型熔断 → 自动切备选，定时试探恢复
 * 4. 按场景路由：简单识别走便宜模型，计划生成走强模型
 *
 * 接入模型：
 * - deepseek-v3 (主力)
 * - qwen-max (备选1，通义千问)
 * - glm-4 (备选2，智谱)
 * - moonshot-v1 (备选3，月之暗面)
 */
@Slf4j
@Service
public class ModelRouter {

    private static final String DEEPSEEK = "deepseek-v3";
    private static final String QWEN = "qwen-max";
    private static final String GLM = "glm-4";
    private static final String MOONSHOT = "moonshot-v1";

    private final Map<String, ModelConfig> models = new HashMap<>();
    private final ModelHealthChecker healthChecker;
    private final DeepSeekCostMonitor costMonitor;
    private final BillingService billingService;
    private final ObjectMapper objectMapper;
    private final CircuitBreakerRegistry cbRegistry;
    private final RetryRegistry retryRegistry;

    /** 被质量降级的模型集合（幻觉率高或其他质量问题） */
    private final java.util.Set<String> deprioritizedModels = java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** 降级开始时间，用于自动恢复 */
    private final Map<String, java.time.Instant> deprioritizedSince = new java.util.concurrent.ConcurrentHashMap<>();

    /** 降级后恢复等待时间（分钟） */
    private static final long DEPRIORITIZE_RECOVERY_MINUTES = 30;

    @Value("${deepseek.api-key}")
    private String deepseekApiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com/v1}")
    private String deepseekBaseUrl;

    @Value("${deepseek.model:deepseek-chat}")
    private String deepseekModel;

    /** 备选模型配置（可通过配置文件覆盖） */
    @Value("${resilience.qwen.api-key:}")
    private String qwenApiKey;

    @Value("${resilience.glm.api-key:}")
    private String glmApiKey;

    @Value("${resilience.moonshot.api-key:}")
    private String moonshotApiKey;

    public ModelRouter(ModelHealthChecker healthChecker,
                        DeepSeekCostMonitor costMonitor,
                        BillingService billingService,
                        ObjectMapper objectMapper,
                        CircuitBreakerRegistry cbRegistry,
                        RetryRegistry retryRegistry) {
        this.healthChecker = healthChecker;
        this.costMonitor = costMonitor;
        this.billingService = billingService;
        this.objectMapper = objectMapper;
        this.cbRegistry = cbRegistry;
        this.retryRegistry = retryRegistry;
    }

    /**
     * 初始化模型配置（在第一次调用时懒加载）。
     */
    private void ensureInitialized() {
        if (!models.isEmpty()) return;

        synchronized (this) {
            if (!models.isEmpty()) return;

            // DeepSeek 主力
            models.put(DEEPSEEK, ModelConfig.builder()
                    .modelId(DEEPSEEK)
                    .displayName("DeepSeek V3")
                    .baseUrl(deepseekBaseUrl)
                    .apiKey(deepseekApiKey)
                    .modelName(deepseekModel)
                    .weight(1.0)
                    .costFactor(0.5)
                    .latencyFactor(2.0)
                    .primary(true)
                    .suitableScenarios(List.of("plan_generate", "safety_check", "chat",
                            "food_recognize", "sentiment_analysis"))
                    .build());

            // 通义千问 备选1
            if (qwenApiKey != null && !qwenApiKey.isBlank()) {
                models.put(QWEN, ModelConfig.builder()
                        .modelId(QWEN)
                        .displayName("通义千问 Max")
                        .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                        .apiKey(qwenApiKey)
                        .modelName("qwen-max")
                        .weight(0.9)
                        .costFactor(1.2)
                        .latencyFactor(3.0)
                        .suitableScenarios(List.of("plan_generate", "chat", "food_recognize"))
                        .build());
            }

            // 智谱 GLM-4 备选2
            if (glmApiKey != null && !glmApiKey.isBlank()) {
                models.put(GLM, ModelConfig.builder()
                        .modelId(GLM)
                        .displayName("智谱 GLM-4")
                        .baseUrl("https://open.bigmodel.cn/api/paas/v4")
                        .apiKey(glmApiKey)
                        .modelName("glm-4")
                        .weight(0.85)
                        .costFactor(0.8)
                        .latencyFactor(2.5)
                        .suitableScenarios(List.of("simple_chat", "food_recognize", "sentiment_analysis"))
                        .build());
            }

            // 月之暗面 备选3
            if (moonshotApiKey != null && !moonshotApiKey.isBlank()) {
                models.put(MOONSHOT, ModelConfig.builder()
                        .modelId(MOONSHOT)
                        .displayName("Moonshot V1")
                        .baseUrl("https://api.moonshot.cn/v1")
                        .apiKey(moonshotApiKey)
                        .modelName("moonshot-v1-8k")
                        .weight(0.8)
                        .costFactor(0.9)
                        .latencyFactor(2.8)
                        .suitableScenarios(List.of("chat", "food_recognize"))
                        .build());
            }
        }
    }

    /**
     * 发送 Chat Completion 请求，自动选择最佳模型。
     *
     * @param messages 消息列表 [{"role":"system","content":"..."}, ...]
     * @param scenario 调用场景（用于模型选择）
     * @return AI 响应文本
     */
    public String chat(List<Map<String, String>> messages, String scenario) {
        return chat(messages, scenario, null);
    }

    /**
     * 发送 Chat Completion 请求（带用户订阅等级，实现成本适配路由）。
     *
     * @param messages 消息列表
     * @param scenario 调用场景
     * @param userId   用户ID（用于按订阅等级选择模型，null时仅按可用性选择）
     * @return AI 响应文本
     */
    public String chat(List<Map<String, String>> messages, String scenario, Long userId) {
        ensureInitialized();
        String modelId = selectModel(scenario, userId);
        ModelConfig config = models.get(modelId);

        if (config == null || !healthChecker.canCall(modelId)) {
            modelId = healthChecker.selectHealthiest(new ArrayList<>(models.keySet()));
            if (modelId == null) {
                throw new BusinessException("所有AI模型当前不可用，请稍后再试");
            }
            config = models.get(modelId);
            log.warn("主模型不可用，降级到 model={}", modelId);
        }

        return callModel(modelId, config, messages, scenario);
    }

    /**
     * 简化的单轮对话（无多轮消息）。
     */
    public String singleChat(String systemPrompt, String userMessage, String scenario) {
        return singleChat(systemPrompt, userMessage, scenario, null);
    }

    /**
     * 简化的单轮对话（带用户订阅等级，实现成本适配路由）。
     */
    public String singleChat(String systemPrompt, String userMessage, String scenario, Long userId) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (systemPrompt != null && !systemPrompt.isBlank()) {
            messages.add(Map.of("role", "system", "content", systemPrompt));
        }
        messages.add(Map.of("role", "user", "content", userMessage));
        return chat(messages, scenario, userId);
    }

    /**
     * 按场景和用户订阅等级选择最佳模型。
     *
     * 策略：
     * - 免费用户：优先低成本模型（GLM > DeepSeek），仅在场景必需时才用DeepSeek
     * - Pro用户：  优先主力模型DeepSeek，降级时可接受所有模型
     * - 企业用户： 优先DeepSeek/Qwen高质量模型，完全不考虑成本
     * - 未传userId：仅按可用性和动态权重选择（原有行为）
     *
     * @param scenario 调用场景
     * @param userId   用户ID（null时不做成本适配）
     */
    private String selectModel(String scenario, Long userId) {
        tryRecoverDeprioritized();
        String primary = DEEPSEEK;

        // 无用户上下文 → 仅按可用性选择（不引入计费逻辑）
        if (userId == null) {
            return selectByAvailability(scenario);
        }

        String tier = billingService.getUserTier(userId);

        // 企业版：始终走最强模型（DeepSeek > Qwen），成本不敏感
        if ("enterprise".equals(tier)) {
            if (healthChecker.canCall(DEEPSEEK) && !deprioritizedModels.contains(DEEPSEEK)) return DEEPSEEK;
            if (healthChecker.canCall(QWEN) && !deprioritizedModels.contains(QWEN)) return QWEN;
            return selectByAvailability(scenario);
        }

        // 免费版：优先低成本模型
        if ("free".equals(tier)) {
            if ("plan_generate".equals(scenario)
                    && healthChecker.canCall(DEEPSEEK)
                    && !deprioritizedModels.contains(DEEPSEEK)) {
                return DEEPSEEK;
            }

            List<String> cheapModels = models.keySet().stream()
                    .filter(id -> healthChecker.canCall(id) && !deprioritizedModels.contains(id))
                    .sorted(Comparator.comparingDouble(id -> models.get(id).getCostFactor()))
                    .toList();

            if (!cheapModels.isEmpty()) {
                String selected = cheapModels.get(0);
                log.debug("免费用户路由选择最便宜模型 userId={} scenario={} model={} costFactor={}",
                        userId, scenario, selected, models.get(selected).getCostFactor());
                return selected;
            }
            return primary;
        }

        // Pro版：优先主力模型，降级时可接受备选
        if (healthChecker.canCall(primary) && !deprioritizedModels.contains(primary)) {
            return primary;
        }
        return selectByAvailability(scenario);
    }

    /**
     * 仅按可用性和动态权重选择模型（不引入计费逻辑）。
     * 排除被质量降级的模型。
     */
    private String selectByAvailability(String scenario) {
        tryRecoverDeprioritized();
        String primary = DEEPSEEK;

        // 主力模型健康且未被降级 → 直接使用
        if (healthChecker.canCall(primary) && !deprioritizedModels.contains(primary)) {
            return primary;
        }

        // 主力不可用或被降级 → 按场景选备选
        List<String> candidates = new ArrayList<>();
        for (var entry : models.entrySet()) {
            if (!entry.getKey().equals(primary)
                    && entry.getValue().getSuitableScenarios().contains(scenario)
                    && healthChecker.canCall(entry.getKey())
                    && !deprioritizedModels.contains(entry.getKey())) {
                candidates.add(entry.getKey());
            }
        }

        if (candidates.isEmpty()) {
            candidates = models.keySet().stream()
                    .filter(id -> !id.equals(primary)
                            && healthChecker.canCall(id)
                            && !deprioritizedModels.contains(id))
                    .toList();
        }

        if (candidates.isEmpty()) {
            // 所有模型都被降级了，只能回退到主模型
            log.warn("所有备选模型被降级或不可用，回退到主模型 {}", primary);
            return primary;
        }

        return candidates.stream()
                .max(Comparator.comparingDouble(id -> {
                    ModelConfig c = models.get(id);
                    c.calculateDynamicWeight();
                    return c.getDynamicWeight();
                }))
                .orElse(primary);
    }

    /**
     * 调用具体模型 API（OpenAI 兼容格式）。
     * 备选模型通过 Resilience4j 编程式 API 获得熔断+重试保护。
     */
    private String callModel(String modelId, ModelConfig config,
                              List<Map<String, String>> messages, String scenario) {
        // 备选模型使用 Resilience4j 保护；主模型（DeepSeek）在 DeepSeekService 层已有注解保护
        if (!config.isPrimary()) {
            return callModelWithResilience(modelId, config, messages, scenario);
        }

        return doCallModel(modelId, config, messages, scenario);
    }

    /**
     * 用 Resilience4j 编程式 API 包裹备选模型调用。
     */
    private String callModelWithResilience(String modelId, ModelConfig config,
                                            List<Map<String, String>> messages, String scenario) {
        String cbName = mapModelToCbName(modelId);
        CircuitBreaker circuitBreaker = cbRegistry.circuitBreaker(cbName);
        Retry retry = retryRegistry.retry(cbName);

        Supplier<String> callSupplier = () -> doCallModel(modelId, config, messages, scenario);

        Supplier<String> retrySupplier = Retry.decorateSupplier(retry, callSupplier);
        Supplier<String> cbSupplier = CircuitBreaker.decorateSupplier(circuitBreaker, retrySupplier);

        try {
            return cbSupplier.get();
        } catch (Exception e) {
            log.error("备选模型经 Resilience4j 保护后仍失败 model={}", modelId, e);

            // 尝试下一个备选
            List<String> remaining = models.keySet().stream()
                    .filter(id -> !id.equals(modelId)
                            && healthChecker.canCall(id)
                            && !deprioritizedModels.contains(id))
                    .toList();
            if (!remaining.isEmpty()) {
                String nextModel = remaining.get(0);
                log.warn("自动切换至备选模型 model={} → {}", modelId, nextModel);
                ModelConfig nextConfig = models.get(nextModel);
                if (!nextConfig.isPrimary()) {
                    return callModelWithResilience(nextModel, nextConfig, messages, scenario);
                }
                return doCallModel(nextModel, nextConfig, messages, scenario);
            }

            throw new BusinessException("AI服务暂时不可用: " + e.getMessage());
        }
    }

    /**
     * 将模型 ID 映射到 Resilience4j 配置名称。
     */
    private String mapModelToCbName(String modelId) {
        return switch (modelId) {
            case QWEN -> "qwen";
            case GLM -> "glm";
            case MOONSHOT -> "moonshot";
            default -> modelId;
        };
    }

    /**
     * 执行实际的 HTTP 调用（无 Resilience4j 包裹）。
     */
    private String doCallModel(String modelId, ModelConfig config,
                                List<Map<String, String>> messages, String scenario) {
        long start = System.currentTimeMillis();

        try {
            WebClient client = WebClient.builder()
                    .baseUrl(config.getBaseUrl())
                    .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + config.getApiKey())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .build();

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", config.getModelName());
            body.put("temperature", 0.3);
            body.put("max_tokens", 2048);

            ArrayNode msgs = body.putArray("messages");
            for (var msg : messages) {
                ObjectNode node = msgs.addObject();
                node.put("role", msg.get("role"));
                node.put("content", msg.get("content"));
            }

            String response = client.post()
                    .uri("/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(60))
                    .onErrorResume(e -> {
                        log.error("模型调用失败 model={} error={}", modelId, e.getMessage());
                        return Mono.just("{\"error\":\"" + e.getMessage() + "\"}");
                    })
                    .block();

            long latency = System.currentTimeMillis() - start;

            if (response != null && response.contains("\"error\"")) {
                healthChecker.recordCall(modelId, false, latency);
                healthChecker.markFailure(modelId);
                log.error("模型返回错误 model={} response={}", modelId, response);
                throw new BusinessException("AI模型返回错误");
            }

            // 解析响应
            String content = extractContent(response);
            healthChecker.recordCall(modelId, true, latency);
            healthChecker.markSuccess(modelId);

            // 成本记录
            int totalTokens = estimateTokens(content);
            costMonitor.recordCall(totalTokens / 2, totalTokens, ModelTier.forScenario(scenario));

            log.info("模型调用成功 model={} scenario={} latencyMs={} tokens~={}",
                    modelId, scenario, latency, totalTokens);

            return content;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            healthChecker.recordCall(modelId, false, latency);
            healthChecker.markFailure(modelId);

            log.error("模型调用异常 model={} scenario={}", modelId, scenario, e);

            // 尝试下一个备选
            List<String> remaining = models.keySet().stream()
                    .filter(id -> !id.equals(modelId)
                            && healthChecker.canCall(id)
                            && !deprioritizedModels.contains(id))
                    .toList();
            if (!remaining.isEmpty()) {
                String nextModel = remaining.get(0);
                log.warn("自动切换至备选模型 model={} → {}", modelId, nextModel);
                return doCallModel(nextModel, models.get(nextModel), messages, scenario);
            }

            throw new BusinessException("AI服务暂时不可用: " + e.getMessage());
        }
    }

    /**
     * 从 OpenAI 格式响应中提取 content。
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
            log.error("解析AI响应失败 response={}", response, e);
        }
        return response;
    }

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
     * 判断主模型是否健康可用。
     */
    public boolean isPrimaryHealthy() {
        ensureInitialized();
        return models.values().stream()
                .filter(ModelConfig::isPrimary)
                .findFirst()
                .map(mc -> healthChecker.canCall(mc.getModelId()) && !deprioritizedModels.contains(mc.getModelId()))
                .orElse(false);
    }

    /**
     * 因质量原因降级指定模型（幻觉率超标、安全分下降等触发）。
     * 降级后模型将被排除在选择列表外，直到恢复等待时间过后自动恢复。
     *
     * @param modelId 要降级的模型ID（如 "deepseek-v3"）
     */
    public void deprioritizeModel(String modelId) {
        if (deprioritizedModels.add(modelId)) {
            deprioritizedSince.put(modelId, java.time.Instant.now());
            log.warn("模型已因质量问题被降级 model={}，将在{}分钟后自动恢复",
                    modelId, DEPRIORITIZE_RECOVERY_MINUTES);
        }
    }

    /**
     * 手动恢复被降级的模型。
     */
    public void restoreModel(String modelId) {
        if (deprioritizedModels.remove(modelId)) {
            deprioritizedSince.remove(modelId);
            log.info("模型已手动恢复 model={}", modelId);
        }
    }

    /**
     * 获取当前被降级的模型列表（供运维 API）。
     */
    public java.util.Set<String> getDeprioritizedModels() {
        return new java.util.HashSet<>(deprioritizedModels);
    }

    /**
     * 检查并自动恢复超时的降级模型。
     * 在 selectModel 时被调用。
     */
    private void tryRecoverDeprioritized() {
        java.time.Instant now = java.time.Instant.now();
        var iterator = deprioritizedModels.iterator();
        while (iterator.hasNext()) {
            String modelId = iterator.next();
            java.time.Instant since = deprioritizedSince.get(modelId);
            if (since != null &&
                    now.toEpochMilli() - since.toEpochMilli() > DEPRIORITIZE_RECOVERY_MINUTES * 60_000) {
                iterator.remove();
                deprioritizedSince.remove(modelId);
                log.info("降级模型已自动恢复 model={} duration={}m",
                        modelId, DEPRIORITIZE_RECOVERY_MINUTES);
            }
        }
    }

    /**
     * 获取模型状态（供运维 API）。
     */
    public Map<String, Object> getModelStatus() {
        ensureInitialized();
        Map<String, Object> status = new HashMap<>();
        for (var entry : models.entrySet()) {
            Map<String, Object> info = new HashMap<>();
            info.put("displayName", entry.getValue().getDisplayName());
            info.put("health", healthChecker.getHealthStatus(entry.getKey()));
            info.put("canCall", healthChecker.canCall(entry.getKey()));
            info.put("successRate", healthChecker.getRecentSuccessRate(entry.getKey()));
            info.put("avgLatencyMs", healthChecker.getAverageLatency(entry.getKey()));
            info.put("primary", entry.getValue().isPrimary());
            status.put(entry.getKey(), info);
        }
        return status;
    }
}