package com.example.agent.orchestrator;

import com.example.agent.HealthCoachAgent;
import com.example.agent.NutritionAgent;
import com.example.agent.PsychologyAgent;
import com.example.agent.SafetyReviewAgent;
import com.example.agent.model.AgentResult;
import com.example.agent.model.RoutingDecision;
import com.example.agent.tool.ToolCallContext;
import com.example.common.BusinessException;
import com.example.entity.SafetyReviewLog;
import com.example.entity.UserProfile;
import com.example.entity.KnowledgeDoc;
import com.example.mapper.SafetyReviewLogMapper;
import com.example.mapper.UserProfileMapper;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.monitor.ModelTier;
import com.example.resilience.FallbackService;
import com.example.resilience.OnlineSafetyCircuitBreaker;
import com.example.sdui.AiAgentResponse;
import com.example.sdui.ToolCallResult;
import com.example.sdui.Widget;
import com.example.service.HealthService;
import com.example.service.KnowledgeService;
import com.example.service.SafetyCheckerService;
import com.example.util.DataMaskingService;
import com.example.util.PromptSanitizer;
import com.example.vo.HealthRecordVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Multi-Agent 编排器。
 * 核心职责：
 * 1. 接收 RoutingDecision，按优先级调度 Agent
 * 2. 支持串行/并行调用模式
 * 3. 收集所有 Agent 结果并聚合
 * 4. 对聚合结果进行安全审查
 * 5. 生成最终 AiAgentResponse
 */
@Slf4j
@Service
public class AgentOrchestrator {

    private final HealthCoachAgent coachAgent;
    private final NutritionAgent nutritionAgent;
    private final PsychologyAgent psychologyAgent;
    private final SafetyReviewAgent safetyReviewAgent;
    private final SafetyCheckerService safetyCheckerService;
    private final OnlineSafetyCircuitBreaker circuitBreaker;
    private final FallbackService fallbackService;
    private final DeepSeekCostMonitor costMonitor;
    private final HealthService healthService;
    private final KnowledgeService knowledgeService;
    private final DataMaskingService dataMaskingService;
    private final UserProfileMapper userProfileMapper;
    private final SafetyReviewLogMapper safetyReviewLogMapper;
    private final ObjectMapper objectMapper;

    private final ExecutorService executor = Executors.newFixedThreadPool(
            Runtime.getRuntime().availableProcessors(),
            r -> {
                Thread t = new Thread(r, "agent-orchestrator");
                t.setDaemon(true);
                return t;
            });

    public AgentOrchestrator(HealthCoachAgent coachAgent,
                              NutritionAgent nutritionAgent,
                              PsychologyAgent psychologyAgent,
                              SafetyReviewAgent safetyReviewAgent,
                              SafetyCheckerService safetyCheckerService,
                              OnlineSafetyCircuitBreaker circuitBreaker,
                              FallbackService fallbackService,
                              DeepSeekCostMonitor costMonitor,
                              HealthService healthService,
                              KnowledgeService knowledgeService,
                              DataMaskingService dataMaskingService,
                              UserProfileMapper userProfileMapper,
                              SafetyReviewLogMapper safetyReviewLogMapper,
                              ObjectMapper objectMapper) {
        this.coachAgent = coachAgent;
        this.nutritionAgent = nutritionAgent;
        this.psychologyAgent = psychologyAgent;
        this.safetyReviewAgent = safetyReviewAgent;
        this.safetyCheckerService = safetyCheckerService;
        this.circuitBreaker = circuitBreaker;
        this.fallbackService = fallbackService;
        this.costMonitor = costMonitor;
        this.healthService = healthService;
        this.knowledgeService = knowledgeService;
        this.dataMaskingService = dataMaskingService;
        this.userProfileMapper = userProfileMapper;
        this.safetyReviewLogMapper = safetyReviewLogMapper;
        this.objectMapper = objectMapper;
    }

    /**
     * 执行 Multi-Agent 工作流。
     *
     * @param decision 路由决策
     * @param userId   用户ID
     * @param userInput 用户原始输入
     * @return 聚合后的 AiAgentResponse
     */
    public AiAgentResponse execute(RoutingDecision decision, Long userId, String userInput) {
        long startTime = System.currentTimeMillis();

        // Step 0: 检查熔断器状态
        if (!circuitBreaker.allowAiCall()) {
            log.warn("安全熔断器已触发，返回规则引擎降级计划 userId={}", userId);
            return fallbackService.getFallbackResponse(userId, userInput);
        }

        // Step 0.5: 医疗红线 Layer1 正则硬匹配（AI 调用前拦截）
        if (!safetyCheckerService.layer1MedicalCheck(userId, userInput)) {
            log.warn("医疗红线Layer1拦截，拒绝AI调用 userId={}", userId);
            return AiAgentResponse.textOnly(
                    "为保障您的健康安全，您的问题涉及医疗诊断/处方/剂量相关内容，系统暂不支持回答。请咨询专业医疗机构。");
        }

        List<AgentResult> results;

        // Step 1: 根据路由决策调用 Agent
        if (decision.isParallel() && decision.getTargetAgents().size() > 1) {
            results = executeParallel(decision, userId, userInput);
        } else {
            results = executeSequential(decision, userId, userInput);
        }

        // Step 2: 聚合结果
        AiAgentResponse aggregated = aggregate(results, userId);

        // Step 3: 安全检查（始终执行，不再依赖 requireSafetyReview 标志）
        AiAgentResponse safeResponse = safetyReview(aggregated, userId);
        long totalMs = System.currentTimeMillis() - startTime;
        log.info("Multi-Agent执行完成 agents={} parallel={} latencyMs={}",
                decision.getTargetAgents(), decision.isParallel(), totalMs);
        return safeResponse;
    }

    /**
     * 根据审查判决记录安全评分到熔断器。
     */
    private void recordSafetyToCircuitBreaker(String verdict) {
        double score = switch (verdict) {
            case "PASS" -> 10.0;
            case "MODIFY" -> 7.0;
            case "BLOCK" -> 4.0;
            default -> 8.0;
        };
        circuitBreaker.recordSafetyScore(score);
    }

    /**
     * 串行执行多个 Agent。
     */
    private List<AgentResult> executeSequential(RoutingDecision decision, Long userId, String userInput) {
        List<AgentResult> results = new ArrayList<>();
        for (String agentName : decision.getTargetAgents()) {
            try {
                AgentResult result = callAgent(agentName, userId, userInput);
                results.add(result);
                // 将前一个 Agent 的输出作为后续 Agent 的上下文
                if (result.isSuccess() && result.getRawOutput() != null) {
                    userInput = userInput + "\n\n[上一轮" + agentName + "的回复]\n" + result.getRawOutput();
                }
            } catch (Exception e) {
                log.error("Agent调用失败 agent={} userId={}", agentName, userId, e);
                results.add(AgentResult.error(agentName, e.getMessage()));
            }
        }
        return results;
    }

    /**
     * 并行执行多个 Agent。
     */
    private List<AgentResult> executeParallel(RoutingDecision decision, Long userId, String userInput) {
        List<CompletableFuture<AgentResult>> futures = new ArrayList<>();
        for (String agentName : decision.getTargetAgents()) {
            CompletableFuture<AgentResult> future = CompletableFuture.supplyAsync(
                    () -> callAgent(agentName, userId, userInput), executor);
            futures.add(future);
        }

        List<AgentResult> results = new ArrayList<>();
        for (int i = 0; i < futures.size(); i++) {
            try {
                AgentResult result = futures.get(i).get(60, TimeUnit.SECONDS);
                results.add(result);
            } catch (TimeoutException e) {
                String agentName = decision.getTargetAgents().get(i);
                log.error("Agent调用超时 agent={}", agentName);
                results.add(AgentResult.error(agentName, "调用超时"));
            } catch (Exception e) {
                String agentName = decision.getTargetAgents().get(i);
                log.error("Agent并行调用失败 agent={}", agentName, e);
                results.add(AgentResult.error(agentName, e.getMessage()));
            }
        }
        return results;
    }

    /**
     * 调用单个 Agent，注入领域相关的健康数据上下文。
     */
    private AgentResult callAgent(String agentName, Long userId, String userInput) {
        long start = System.currentTimeMillis();
        String rawOutput;

        // 启动 ToolCallContext 以捕获 Function Calling 的 Tool 调用记录
        ToolCallContext.start();
        try {
            // 为每个 Agent 构建专属上下文 Prompt
            String contextualInput = buildAgentContext(agentName, userId, userInput);

            rawOutput = switch (agentName) {
                case "coach" -> coachAgent.chat(contextualInput);
                case "nutrition" -> nutritionAgent.analyze(contextualInput);
                case "psychology" -> psychologyAgent.counsel(contextualInput);
                default -> throw new BusinessException("未知Agent: " + agentName);
            };
        } catch (Exception e) {
            log.error("Agent执行异常 agent={} userId={}", agentName, userId, e);
            ToolCallContext.clear();
            return AgentResult.error(agentName, e.getMessage());
        }

        long latency = System.currentTimeMillis() - start;

        // 记录成本（估算，LangChain4j 暂不直接暴露 token count）
        int estimatedTokens = estimateTokens(rawOutput);
        costMonitor.recordCall(estimatedTokens / 2, estimatedTokens, ModelTier.forScenario(agentName));

        // 硬编码安全检查
        boolean safePassed = true;
        List<String> planItems = safetyCheckerService.extractPlanItems(rawOutput);
        var checkResult = safetyCheckerService.checkPlan(userId, planItems);
        if (!checkResult.isPassed()) {
            safePassed = false;
            log.warn("Agent输出未通过安全检查 agent={} issues={}", agentName, checkResult.getMessage());
        }

        // 捕获 Tool 调用记录并转换为 SDUI 协议
        List<ToolCallResult> toolCalls = ToolCallResult.from(ToolCallContext.getRecords());
        ToolCallContext.clear();

        return AgentResult.builder()
                .agentName(agentName)
                .rawOutput(rawOutput)
                .response(AiAgentResponse.builder()
                        .text(rawOutput)
                        .toolCalls(toolCalls)
                        .disclaimer("本建议仅供参考，不构成医疗诊断或处方。如有健康问题请咨询专业医生。")
                        .build())
                .inputTokens(estimatedTokens / 2)
                .outputTokens(estimatedTokens)
                .latencyMs(latency)
                .success(true)
                .safetyPassed(safePassed)
                .build();
    }

    /**
     * 聚合多个 Agent 的结果，含跨 Agent 冲突检测。
     * <p>
     * 融合策略：
     * 1. 单 Agent 直接返回（快路径）
     * 2. 多 Agent 纯文本拼接 + Widget/ToolCall 合并
     * 3. 对并行产出的多份结果进行轻量级冲突检测并附加融合提示
     */
    private AiAgentResponse aggregate(List<AgentResult> results, Long userId) {
        if (results.isEmpty()) {
            return AiAgentResponse.textOnly("系统暂无法处理您的请求，请稍后再试。");
        }

        // 单 Agent 直接返回（快路径）
        if (results.size() == 1 && results.get(0).isSuccess()) {
            return results.get(0).getResponse();
        }

        // 多 Agent 聚合
        StringBuilder text = new StringBuilder();
        List<Widget> allWidgets = new ArrayList<>();
        List<ToolCallResult> allToolCalls = new ArrayList<>();

        for (AgentResult r : results) {
            if (!r.isSuccess()) {
                text.append("[").append(r.getAgentName()).append("] 暂时无法响应\n\n");
                continue;
            }
            text.append("【").append(getAgentDisplayName(r.getAgentName())).append("】\n");
            text.append(r.getRawOutput()).append("\n\n");

            if (r.getResponse() != null) {
                if (r.getResponse().getWidgets() != null) {
                    allWidgets.addAll(r.getResponse().getWidgets());
                }
                if (r.getResponse().getToolCalls() != null) {
                    allToolCalls.addAll(r.getResponse().getToolCalls());
                }
            }
        }

        // 多 Agent 结果融合：轻量级冲突检测
        String fusionNote = detectConflicts(results);
        if (!fusionNote.isBlank()) {
            text.append("\n---\n\u26A0\uFE0F 【跨领域协作提示】").append(fusionNote);
        }

        return AiAgentResponse.builder()
                .text(text.toString().trim())
                .widgets(allWidgets)
                .toolCalls(allToolCalls)
                .disclaimer("本建议由多领域AI专家协作生成，仅供参考，不构成医疗诊断或处方。")
                .build();
    }

    /**
     * 轻量级跨 Agent 冲突检测。
     * 对多 Agent 并行产出的文本进行关键词级交叉校验，检测潜在矛盾建议。
     * 不引入额外 LLM 调用，避免增加响应延迟。
     *
     * @return 冲突提示文本，无冲突时返回空字符串
     */
    private String detectConflicts(List<AgentResult> results) {
        List<AgentResult> successResults = results.stream()
                .filter(AgentResult::isSuccess)
                .toList();
        if (successResults.size() < 2) {
            return "";
        }

        StringBuilder conflicts = new StringBuilder();

        // 检测运动强度冲突（教练 vs 营养师 / 心理）
        String coachText = extractTextByAgent(successResults, "coach");
        String nutritionText = extractTextByAgent(successResults, "nutrition");
        String psychText = extractTextByAgent(successResults, "psychology");

        if (!coachText.isBlank() && !nutritionText.isBlank()) {
            // 教练推荐高强度而营养师推荐减量/低强度
            boolean coachHigh = containsAny(coachText, "高强度", "冲刺", "大重量", "爆发力", "HIIT", "间歇跑", "力量训练");
            boolean nutritionLow = containsAny(nutritionText, "低强度", "减量", "轻断食", "极低热量", "禁食");
            if (coachHigh && nutritionLow) {
                conflicts.append("\n- 运动教练建议较高强度训练，同时营养师建议降低摄入，请根据身体感受权衡执行，必要时咨询专业人士。");
            }
        }

        if (!coachText.isBlank() && !psychText.isBlank()) {
            // 教练推荐高强度但心理师建议放松
            boolean coachHigh = containsAny(coachText, "高强度", "冲刺", "每天训练", "坚持", "硬拉");
            boolean psychRelax = containsAny(psychText, "休息", "放松", "减负", "暂停训练", "放慢节奏");
            if (coachHigh && psychRelax) {
                conflicts.append("\n- 运动教练和心理顾问的建议在训练强度上存在不同侧重，建议优先关注心理状态，运动强度可循序渐进。");
            }
        }

        if (!nutritionText.isBlank()) {
            // 营养建议内部矛盾检测（如同时出现"增加碳水"和"减少碳水"）
            if (containsAny(nutritionText, "增加碳水", "多摄入碳水", "碳水补充")
                    && containsAny(nutritionText, "减少碳水", "低碳水", "控制碳水")) {
                conflicts.append("\n- 营养建议中碳水摄入方向存在不一致，建议根据运动日和休息日区分碳水摄入策略。");
            }
        }

        String result = conflicts.toString();
        if (result.length() > 500) {
            result = result.substring(0, 500) + "...";
        }
        return result;
    }

    /**
     * 从结果列表中提取指定 Agent 的原始文本。
     */
    private String extractTextByAgent(List<AgentResult> results, String agentName) {
        return results.stream()
                .filter(r -> agentName.equals(r.getAgentName()) && r.isSuccess())
                .map(AgentResult::getRawOutput)
                .findFirst()
                .orElse("");
    }

    /**
     * 检查文本中是否包含任一关键词。
     */
    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    /**
     * AI 安全审查（第二道防线，LLM审查）。
     * 审查失败时降级到 SafetyCheckerService 规则引擎兜底。
     */
    private AiAgentResponse safetyReview(AiAgentResponse response, Long userId) {
        if (response.getText() == null || response.getText().isBlank()) {
            return response;
        }

        long reviewStart = System.currentTimeMillis();
        String responseText = response.getText();
        String userContext = buildSafetyContext(userId);
        String knowledgeContext = buildKnowledgeReviewContext(responseText);

        try {
            String reviewResult = safetyReviewAgent.review(userContext, knowledgeContext, responseText);
            JsonNode review = parseReviewJson(reviewResult);
            String verdict = review.has("verdict") ? review.get("verdict").asText() : "PASS";
            String riskLevel = review.has("riskLevel") ? review.get("riskLevel").asText() : "none";
            String issues = review.has("issues") ? review.get("issues").toString() : "[]";
            String suggestions = review.has("suggestions") ? review.get("suggestions").toString() : "[]";
            long latencyMs = System.currentTimeMillis() - reviewStart;

            // 记录真实安全评分到熔断器
            recordSafetyToCircuitBreaker(verdict);

            // 写入安全审查审计日志
            saveSafetyReviewLog(userId, verdict, riskLevel, issues, suggestions,
                    responseText, latencyMs, false);

            if ("BLOCK".equals(verdict)) {
                log.warn("安全审查拦截 userId={} issues={}", userId, issues);
                return AiAgentResponse.textOnly(
                        "为保障您的安全，当前回复经审查后暂不显示。建议咨询专业健康人士。");
            }

            if ("MODIFY".equals(verdict)) {
                String suggestion = review.has("suggestions") && review.get("suggestions").isArray()
                        ? String.join("; ", parseStringArray(review, "suggestions")) : "";
                log.info("安全审查建议修改 userId={} suggestion={}", userId, suggestion);

                String modifiedText = responseText
                        + "\n\n---\n\u26A0\uFE0F [安全提示] " + suggestion;
                response.setText(modifiedText);
                response.getMetadata().put("safetyWarning", suggestion);
            }

        } catch (Exception e) {
            log.error("安全审查LLM调用失败，降级到规则引擎检查 userId={}", userId, e);
            // 降级：使用规则引擎兜底
            AiAgentResponse fallbackChecked = fallbackSafetyCheck(response, userId);
            recordSafetyToCircuitBreaker("MODIFY"); // 降级检查保守评分
            return fallbackChecked;
        }

        return response;
    }

    /**
     * 规则引擎兜底安全检查（LLM不可用时）。
     */
    private AiAgentResponse fallbackSafetyCheck(AiAgentResponse response, Long userId) {
        long reviewStart = System.currentTimeMillis();
        List<String> planItems = safetyCheckerService.extractPlanItems(response.getText());
        var checkResult = safetyCheckerService.checkPlan(userId, planItems);
        long latencyMs = System.currentTimeMillis() - reviewStart;

        if (!checkResult.isPassed()) {
            log.warn("规则引擎兜底拦截 userId={} issues={}", userId, checkResult.getMessage());
            saveSafetyReviewLog(userId, "BLOCK", "high",
                    "[\"" + checkResult.getMessage().replace("\n", "\\n") + "\"]",
                    "[]", response.getText(), latencyMs, true);
            response.getMetadata().put("safetyFallbackBlocked", true);
            return AiAgentResponse.textOnly(
                    "为保障您的安全，当前回复经审查后暂不显示。建议咨询专业健康人士。");
        }

        saveSafetyReviewLog(userId, "PASS", "none", "[]", "[]",
                response.getText(), latencyMs, true);
        response.getMetadata().put("safetyFallbackPassed", true);
        return response;
    }

    /**
     * 保存安全审查审计日志（异步，不阻塞主流程）。
     */
    private void saveSafetyReviewLog(Long userId, String verdict, String riskLevel,
                                      String issues, String suggestions,
                                      String content, long latencyMs, boolean fallbackMode) {
        try {
            SafetyReviewLog logEntry = new SafetyReviewLog();
            logEntry.setUserId(userId);
            logEntry.setVerdict(verdict);
            logEntry.setRiskLevel(riskLevel);
            logEntry.setIssues(issues);
            logEntry.setSuggestions(suggestions);
            logEntry.setFallbackMode(fallbackMode);
            logEntry.setContentDigest(content.length() > 500
                    ? content.substring(0, 500) : content);
            logEntry.setLatencyMs(latencyMs);
            logEntry.setCreatedAt(LocalDateTime.now());
            safetyReviewLogMapper.insert(logEntry);
        } catch (Exception e) {
            log.warn("安全审查审计日志写入失败 userId={}", userId, e);
        }
    }

    /**
     * 解析安全审查返回的 JSON（兼容 markdown 代码块包裹）。
     */
    private JsonNode parseReviewJson(String raw) throws Exception {
        String cleaned = raw
                .replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();
        return objectMapper.readTree(cleaned);
    }

    /**
     * 从 JSON 节点中提取字符串数组。
     */
    private List<String> parseStringArray(JsonNode root, String field) {
        List<String> list = new ArrayList<>();
        if (root.has(field) && root.get(field).isArray()) {
            root.get(field).forEach(node -> list.add(node.asText()));
        }
        return list;
    }

    /**
     * 构建安全审查专用的用户健康上下文。
     */
    private String buildSafetyContext(Long userId) {
        StringBuilder ctx = new StringBuilder();
        try {
            HealthRecordVO health = healthService.getLatestHealthRecord(userId);
            UserProfile profile = userProfileMapper.selectById(userId);

            if (health != null) {
                if (health.getDiseaseHistory() != null && !health.getDiseaseHistory().isBlank()) {
                    String masked = dataMaskingService.maskDiseaseHistory(health.getDiseaseHistory());
                    String sanitized = PromptSanitizer.sanitize(masked);
                    if (!sanitized.isBlank()) {
                        ctx.append("疾病史: ").append(sanitized).append("; ");
                    }
                }
                if (health.getAllergyHistory() != null && !health.getAllergyHistory().isBlank()) {
                    String masked = dataMaskingService.maskAllergyHistory(health.getAllergyHistory());
                    String sanitized = PromptSanitizer.sanitize(masked);
                    if (!sanitized.isBlank()) {
                        ctx.append("过敏史: ").append(sanitized).append("; ");
                    }
                }
                if (health.getBmi() != null) {
                    ctx.append(String.format("BMI: %.1f; ", health.getBmi()));
                }
                if (health.getGoal() != null && !health.getGoal().isBlank()) {
                    ctx.append("健康目标: ").append(health.getGoal()).append("; ");
                }
            }
            if (profile != null) {
                if (profile.getFitnessLevel() != null) {
                    ctx.append("运动基础: ").append(profile.getFitnessLevel()).append("; ");
                }
                if (profile.getChronicDiseases() != null && !profile.getChronicDiseases().isBlank()
                        && !"无".equals(profile.getChronicDiseases())) {
                    ctx.append("慢性病: ").append(profile.getChronicDiseases()).append("; ");
                }
                if (profile.getInjuries() != null && !profile.getInjuries().isBlank()
                        && !"无".equals(profile.getInjuries())) {
                    ctx.append("损伤史: ").append(profile.getInjuries()).append("; ");
                }
            }
        } catch (Exception e) {
            log.warn("构建安全上下文失败 userId={}", userId, e);
        }

        if (ctx.length() == 0) {
            return "无特殊健康风险记录";
        }
        return ctx.toString();
    }

    /**
     * 构建安全审查专用的专业知识库上下文。
     * 从权威知识库中检索与待审查内容相关的指南、标准，作为 LLM 安全审查的检验依据。
     *
     * @param contentToReview 待审查的 AI 输出内容
     * @return 知识库引用上下文，若未检索到相关内容则返回提示文本
     */
    private String buildKnowledgeReviewContext(String contentToReview) {
        try {
            boolean isMedicalCore = knowledgeService.isMedicalCoreQuestion(contentToReview);
            List<KnowledgeDoc> docs = knowledgeService.searchRelevant(contentToReview, isMedicalCore, 5);

            if (docs == null || docs.isEmpty()) {
                return "当前知识库中未检索到直接相关的权威指南，请基于通用医学/营养/运动安全标准进行审查。";
            }

            return knowledgeService.buildKnowledgeContext(docs);
        } catch (Exception e) {
            log.warn("构建知识库审查上下文失败", e);
            return "知识库检索暂时不可用，请基于通用安全标准进行审查。";
        }
    }

    private String getAgentDisplayName(String agentName) {
        return switch (agentName) {
            case "coach" -> "运动教练";
            case "nutrition" -> "营养师";
            case "psychology" -> "心理健康顾问";
            default -> agentName;
        };
    }

    /**
     * 构建各 Agent 专属的健康数据上下文 Prompt。
     * 健康教练获取运动+计划数据，营养师获取饮食+身体数据，心理咨询师获取情绪+睡眠数据。
     */
    private String buildAgentContext(String agentName, Long userId, String userInput) {
        StringBuilder ctx = new StringBuilder();

        try {
            HealthRecordVO health = healthService.getLatestHealthRecord(userId);
            UserProfile profile = userProfileMapper.selectById(userId);

            if (health != null || profile != null) {
                ctx.append("【用户健康档案】\n");

                if (health != null) {
                    ctx.append(String.format("- 身高: %.1fcm, 体重: %.1fkg, BMI: %.1f\n",
                            health.getHeight(), health.getWeight(), health.getBmi()));
                    ctx.append(String.format("- 基础代谢: %dkcal, 日推荐热量: %dkcal\n",
                            health.getBmr(), health.getDailyCalorie()));
                    if (health.getGoal() != null && !health.getGoal().isBlank()) {
                        ctx.append("- 健康目标: ").append(health.getGoal()).append("\n");
                    }
                    if (health.getDiseaseHistory() != null && !health.getDiseaseHistory().isBlank()) {
                        String masked = dataMaskingService.maskDiseaseHistory(health.getDiseaseHistory());
                        String sanitized = PromptSanitizer.sanitize(masked);
                        if (!sanitized.isBlank()) {
                            ctx.append("- 疾病史: ").append(sanitized).append("\n");
                        }
                    }
                    if (health.getAllergyHistory() != null && !health.getAllergyHistory().isBlank()) {
                        String masked = dataMaskingService.maskAllergyHistory(health.getAllergyHistory());
                        String sanitized = PromptSanitizer.sanitize(masked);
                        if (!sanitized.isBlank()) {
                            ctx.append("- 过敏史: ").append(sanitized).append("\n");
                        }
                    }
                }

                if (profile != null) {
                    if (profile.getFitnessLevel() != null) {
                        ctx.append("- 运动基础: ").append(profile.getFitnessLevel()).append("\n");
                    }
                    if (profile.getDietPreferences() != null && !profile.getDietPreferences().isBlank()) {
                        ctx.append("- 饮食偏好: ").append(profile.getDietPreferences()).append("\n");
                    }
                    if (profile.getDailyAvailableMin() != null) {
                        ctx.append("- 每日可用时间: ").append(profile.getDailyAvailableMin()).append("分钟\n");
                    }
                }

                // 领域专属上下文
                ctx.append("\n").append(getAgentSpecificHint(agentName));
            }
        } catch (Exception e) {
            log.warn("构建Agent上下文失败 agent={} userId={}", agentName, userId, e);
        }

        if (ctx.length() > 0) {
            ctx.append("\n---\n【用户问题】\n<user_message>\n").append(userInput).append("\n</user_message>");
            return ctx.toString();
        }
        return "<user_message>\n" + userInput + "\n</user_message>";
    }

    /**
     * 针对不同 Agent 给出领域专属的调用提示。
     */
    private String getAgentSpecificHint(String agentName) {
        return switch (agentName) {
            case "coach" -> "【运动教练提示】请根据用户的身体数据、运动基础和可用时间，"
                    + "给出具体的运动计划建议。如用户有疾病史，避开禁忌运动。"
                    + "可用工具：查询运动偏好/完成率、计算运动消耗热量、心率区间、运动强度推荐、体能评估。";
            case "nutrition" -> "【营养师提示】请根据用户的BMI、热量需求和饮食偏好，"
                    + "分析饮食结构并给出建议。如用户有过敏史或疾病史，避开相关食物。"
                    + "可用工具：饮食记录查询、食物营养计算、BMR计算、宏量营养素配比、营养素缺口分析。";
            case "psychology" -> "【心理咨询提示】请以共情态度回应用户，"
                    + "结合用户的健康目标和运动基础分析行为障碍。"
                    + "可用工具：打卡坚持度、睡眠模式、情绪历史、行为模式分析、激励建议。";
            default -> "";
        };
    }
    /**
     * 粗略估算 token 数（中文约 1.5 字符/token，英文约 4 字符/token）。
     */
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
}