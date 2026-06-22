package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.BusinessException;
import com.example.convert.AiPlanConvert;
import com.example.entity.AiPlan;
import com.example.entity.AiPlanDetail;
import com.example.entity.DailyCheckin;
import com.example.mapper.AiPlanDetailMapper;
import com.example.mapper.AiPlanMapper;
import com.example.mapper.DailyCheckinMapper;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.resilience.AiCallQueueService;
import com.example.resilience.FallbackService;
import com.example.resilience.ModelRouter;
import com.example.sdui.*;
import com.example.service.*;
import com.example.util.PromptSanitizer;
import com.example.vo.HealthRecordVO;
import com.example.vo.SafetyCheckResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PlanAdjustServiceImpl implements PlanAdjustService {

    private final AiPlanMapper aiPlanMapper;
    private final AiPlanDetailMapper aiPlanDetailMapper;
    private final DailyCheckinMapper dailyCheckinMapper;
    private final HealthService healthService;
    private final DeepSeekService deepSeekService;
    private final AiPlanService aiPlanService;
    private final DeepSeekCostMonitor costMonitor;
    private final AiPlanConvert aiPlanConvert;
    private final SafetyCheckerService safetyCheckerService;
    private final ModelRouter modelRouter;
    private final FallbackService fallbackService;
    private final AiCallQueueService aiCallQueueService;
    private final PlanAdjustServiceImpl self;

    public PlanAdjustServiceImpl(AiPlanMapper aiPlanMapper,
                                  AiPlanDetailMapper aiPlanDetailMapper,
                                  DailyCheckinMapper dailyCheckinMapper,
                                  HealthService healthService,
                                  DeepSeekService deepSeekService,
                                  AiPlanService aiPlanService,
                                  DeepSeekCostMonitor costMonitor,
                                  AiPlanConvert aiPlanConvert,
                                  SafetyCheckerService safetyCheckerService,
                                  ModelRouter modelRouter,
                                  FallbackService fallbackService,
                                  AiCallQueueService aiCallQueueService,
                                  @Lazy PlanAdjustServiceImpl self) {
        this.aiPlanMapper = aiPlanMapper;
        this.aiPlanDetailMapper = aiPlanDetailMapper;
        this.dailyCheckinMapper = dailyCheckinMapper;
        this.healthService = healthService;
        this.deepSeekService = deepSeekService;
        this.aiPlanService = aiPlanService;
        this.costMonitor = costMonitor;
        this.aiPlanConvert = aiPlanConvert;
        this.safetyCheckerService = safetyCheckerService;
        this.modelRouter = modelRouter;
        this.fallbackService = fallbackService;
        this.aiCallQueueService = aiCallQueueService;
        this.self = self;
    }

    @Override
    public AiAgentResponse adjustPlan(Long originalPlanId, Long userId, String feedback) {
        if (costMonitor.isGlobalCostExceeded()) {
            throw new BusinessException("今日AI调用额度已用尽，请明天再试");
        }

        AiPlan originalPlan = aiPlanMapper.selectById(originalPlanId);
        if (originalPlan == null || !originalPlan.getUserId().equals(userId)) {
            throw new BusinessException(404, "计划不存在");
        }

        HealthRecordVO health = healthService.getLatestHealthRecord(userId);

        // 统计当前计划的完成情况
        String statsJson = buildStatsJson(originalPlanId, userId);

        // Prompt 注入防护：对用户反馈和健康目标进行过滤
        String sanitizedFeedback = PromptSanitizer.sanitize(
                feedback != null && !feedback.isEmpty() ? feedback : "无特殊反馈");
        String sanitizedGoal = PromptSanitizer.sanitize(
                health.getGoal() != null ? health.getGoal() : "未设定");

        // 构建调整Prompt
        String adjustPrompt = String.format(
                "你是专业健康教练。用户正在进行一个%d天的%s计划。\n" +
                        "当前用户数据：身高%.1fcm，体重%.1fkg，BMI%.1f，健康目标：%s。\n" +
                        "原计划内容：%s\n" +
                        "计划执行统计：%s\n" +
                        "用户反馈：%s\n\n" +
                        "请根据以上数据，生成一个调整后的新计划。\n" +
                        "输出格式要求：\n" +
                        (originalPlan.getPlanType().equals("sport") ?
                                "如果是运动计划，每个运动任务必须拆分为结构化阶段（热身/核心/放松）：\n" +
                                "{\"days\":[{\"d\":1,\"items\":[{\"name\":\"运动名称\",\"type\":\"sport\",\"durationMin\":30," +
                                "\"phases\":[{\"name\":\"热身\",\"type\":\"warmup\",\"minutes\":5,\"instruction\":\"...\"}," +
                                "{\"name\":\"核心训练\",\"type\":\"core\",\"minutes\":22,\"instruction\":\"...\"}," +
                                "{\"name\":\"放松拉伸\",\"type\":\"cooldown\",\"minutes\":3,\"instruction\":\"...\"}]}]}]}\n"
                                : "如果是饮食计划，输出扁平格式：{\"days\":[{\"d\":天数,\"items\":[\"具体任务\"]}]}\n") +
                        "调整原则：如果用户觉得太轻松就加大强度，觉得太难就降低强度；" +
                        "如果特定部位不适（如膝盖痛）则替换为低冲击运动；" +
                        "如果饮食计划不合理则优化食物搭配。必须用```json包裹JSON。",
                originalPlan.getDurationDays(),
                originalPlan.getPlanType().equals("sport") ? "运动" : "饮食",
                health.getHeight(), health.getWeight(), health.getBmi(), sanitizedGoal,
                truncate(originalPlan.getAiContent(), 500),
                statsJson,
                sanitizedFeedback
        );

        // 调用AI生成调整后的计划（DeepSeek 主路径 + ModelRouter 降级 + 队列削峰）
        String adjustedContent;
        boolean isFallback = false;
        try {
            adjustedContent = aiCallQueueService.submitWithPeakProtection(
                    () -> deepSeekService.callApiRaw(adjustPrompt),
                    "plan_adjust_user_" + userId);
        } catch (Exception e) {
            log.warn("DeepSeek 调整调用失败，降级到 ModelRouter planId={}", originalPlanId, e);
            try {
                adjustedContent = modelRouter.singleChat(
                        "你是专业健康教练，根据用户数据生成调整后的健康计划JSON。只输出JSON。",
                        adjustPrompt,
                        "plan_generate",
                        userId);
            } catch (Exception fallbackError) {
                log.error("ModelRouter 降级也失败，启用规则引擎兜底 planId={}", originalPlanId, fallbackError);
                adjustedContent = fallbackService.generateFallbackPlanJson(
                        health.getHeight().doubleValue(),
                        health.getWeight().doubleValue(),
                        sanitizedGoal,
                        originalPlan.getDurationDays());
                isFallback = true;
            }
        }

        // 安全检查：核查调整后的计划任务是否与用户健康状况冲突
        List<String> planItems = safetyCheckerService.extractPlanItems(adjustedContent);
        SafetyCheckResult safetyResult = safetyCheckerService.checkPlan(userId, planItems);
        if (!safetyResult.isPassed()) {
            log.warn("调整计划被安全检查拦截 userId={} planId={} message={}",
                    userId, originalPlanId, safetyResult.getMessage());
            throw new BusinessException(safetyResult.getMessage());
        }

        // 创建新计划（降级模式下标记来源）
        AiPlan newPlan = new AiPlan();
        newPlan.setUserId(userId);
        newPlan.setPlanType(originalPlan.getPlanType());
        newPlan.setPlanName(originalPlan.getPlanName() + (isFallback ? "-调整版(基础计划)" : "-调整版"));
        newPlan.setDurationDays(originalPlan.getDurationDays());
        newPlan.setAiContent(adjustedContent);
        newPlan.setStartDate(LocalDate.now());
        newPlan.setStatus(1);

        // DB writes in short transaction (after AI call completed outside transaction)
        self.saveAdjustedPlanInTransaction(originalPlan, newPlan, adjustedContent);

        log.info("AI动态调整计划成功 originalPlanId={} newPlanId={} isFallback={}",
                originalPlanId, newPlan.getId(), isFallback);

        // 构建 SDUI 响应
        return buildSduiResponse(newPlan, adjustedContent, isFallback);
    }

    private AiAgentResponse buildSduiResponse(AiPlan plan, String aiContent, boolean isFallback) {
        List<Widget> widgets = new ArrayList<>();

        // 文本块展示调整后的计划内容
        TextBlockWidget textBlock = new TextBlockWidget();
        textBlock.setTitle("调整后的计划");
        textBlock.setContent(aiContent);
        widgets.add(textBlock);

        // 进度环展示新计划概览
        ProgressRingWidget progressRing = new ProgressRingWidget();
        progressRing.setTitle("新计划进度");
        progressRing.setPercentage(0.0);
        progressRing.setLabel(plan.getPlanName());
        progressRing.setColor("green");
        progressRing.setSubText("共" + plan.getDurationDays() + "天");
        widgets.add(progressRing);

        // 提示条
        TipWidget tip = new TipWidget();
        if (isFallback) {
            tip.setTitle("重要提示");
            tip.setContent("当前AI服务不可用，已为您生成基于健康规则引擎的基础调整计划。"
                    + "该计划未经过AI个性化优化，建议在AI服务恢复后重新获取个性化方案。");
            tip.setCategory("warning");
        } else {
            tip.setTitle("温馨提示");
            tip.setContent("计划已根据你的反馈进行调整，新的计划从今天开始执行。");
            tip.setCategory("info");
        }
        widgets.add(tip);

        AiAgentResponse.AiAgentResponseBuilder builder = AiAgentResponse.builder()
                .protocolVersion("1.0")
                .text(aiContent)
                .widgets(widgets);

        if (isFallback) {
            builder.disclaimer("【重要】当前AI服务不可用，本计划由健康规则引擎自动生成。"
                    + "建议在AI服务恢复后重新获取个性化方案。"
                    + "本建议仅供参考，不构成医疗诊断或处方。如有健康问题请咨询专业医生。");
            builder.metadata(java.util.Map.of(
                    "planSource", "rule_fallback",
                    "aiAvailable", false,
                    "generatedAt", java.time.LocalDateTime.now().toString()
            ));
        } else {
            builder.disclaimer("本建议仅供参考，不构成医疗诊断或处方。如有健康问题请咨询专业医生。");
        }

        return builder.build();
    }

    private String buildStatsJson(Long planId, Long userId) {
        AiPlan plan = aiPlanMapper.selectById(planId);
        if (plan == null || plan.getStartDate() == null) return "{}";

        LocalDate startDate = plan.getStartDate();
        LambdaQueryWrapper<DailyCheckin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DailyCheckin::getUserId, userId)
                .ge(DailyCheckin::getCheckDate, startDate)
                .le(DailyCheckin::getCheckDate, LocalDate.now());

        List<DailyCheckin> checkins = dailyCheckinMapper.selectList(wrapper);
        int totalDays = 0;
        int exerciseComplete = 0;
        int dietComplete = 0;
        List<Double> weightChanges = new ArrayList<>();

        for (DailyCheckin c : checkins) {
            totalDays++;
            if (c.getExerciseStatus() != null && c.getExerciseStatus() >= 1) exerciseComplete++;
            if (c.getDietStatus() != null && c.getDietStatus() >= 1) dietComplete++;
            if (c.getCurrentWeight() != null) {
                weightChanges.add(c.getCurrentWeight().doubleValue());
            }
        }

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalDays", totalDays);
        stats.put("exerciseCompleteRate", totalDays > 0 ? (exerciseComplete * 100 / totalDays) : 0);
        stats.put("dietCompleteRate", totalDays > 0 ? (dietComplete * 100 / totalDays) : 0);
        stats.put("weightTrend", "待计算");
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(stats);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Short transactional helper: deactivate old plan + insert new plan + save details.
     * Called from adjustPlan() AFTER the AI call completes outside any transaction.
     */
    @Transactional
    public void saveAdjustedPlanInTransaction(AiPlan originalPlan, AiPlan newPlan, String adjustedContent) {
        originalPlan.setStatus(2);
        aiPlanMapper.updateById(originalPlan);

        aiPlanMapper.insert(newPlan);

        saveDetails(newPlan.getId(), adjustedContent, newPlan.getPlanType());
    }

    private void saveDetails(Long planId, String aiContent, String planType) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode root = mapper.readTree(aiContent);
            JsonNode days = root.path("days");
            if (days.isArray()) {
                for (JsonNode day : days) {
                    int daySeq = day.path("d").asInt();
                    JsonNode items = day.path("items");
                    if (items.isArray()) {
                        for (JsonNode itemNode : items) {
                            // 支持结构化phase格式和扁平字符串格式
                            if (itemNode.isObject()) {
                                String itemName = itemNode.path("name").asText();
                                String itemType = itemNode.path("type").asText(planType);
                                int durationMin = itemNode.path("durationMin").asInt(0);
                                JsonNode phases = itemNode.path("phases");
                                if (phases.isArray() && phases.size() > 0) {
                                    for (int pi = 0; pi < phases.size(); pi++) {
                                        JsonNode phase = phases.get(pi);
                                        AiPlanDetail detail = new AiPlanDetail();
                                        detail.setPlanId(planId);
                                        detail.setDaySequence(daySeq);
                                        detail.setItemType(itemType);
                                        detail.setItemName(itemName);
                                        detail.setTargetAmount(durationMin + "min");
                                        detail.setSubPhase(phase.path("name").asText());
                                        detail.setSubPhaseType(phase.path("type").asText());
                                        detail.setPhaseOrder(pi + 1);
                                        detail.setPhaseDurationMinutes(phase.path("minutes").asInt(0));
                                        aiPlanDetailMapper.insert(detail);
                                    }
                                } else {
                                    AiPlanDetail detail = new AiPlanDetail();
                                    detail.setPlanId(planId);
                                    detail.setDaySequence(daySeq);
                                    detail.setItemType(itemType);
                                    detail.setItemName(itemName);
                                    detail.setTargetAmount(durationMin + "min");
                                    aiPlanDetailMapper.insert(detail);
                                }
                            } else {
                                AiPlanDetail detail = new AiPlanDetail();
                                detail.setPlanId(planId);
                                detail.setDaySequence(daySeq);
                                detail.setItemType(planType);
                                detail.setItemName(itemNode.asText());
                                detail.setTargetAmount("按需完成");
                                aiPlanDetailMapper.insert(detail);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("解析计划detail失败", e);
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}