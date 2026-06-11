package com.example.service.impl;

import com.example.agent.HealthCoachAgent;
import com.example.common.BusinessException;
import com.example.dto.PlanGenerateDTO;
import com.example.entity.AiPlan;
import com.example.entity.AiPlanDetail;
import com.example.mapper.AiPlanDetailMapper;
import com.example.mapper.AiPlanMapper;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.agent.tool.ToolCallContext;
import com.example.sdui.*;
import com.example.service.HealthService;
import com.example.service.SafetyCheckerService;
import com.example.util.AiResponseParser;
import com.example.util.DataMaskingService;
import com.example.util.PromptSanitizer;
import com.example.vo.AiPlanDetailVO;
import com.example.vo.HealthRecordVO;
import com.example.vo.SafetyCheckResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * V2 计划生成服务 — 基于 LangChain4j + Function Calling。
 * 与旧 AiPlanServiceImpl 并行运行，A/B 测试后逐步切流。
 */
@Slf4j
@Service
public class PlanGenerateV2Service {

    private static final String USER_LIMIT_KEY = "ai:plan:v2:limit:%d:%s";
    private static final int DAILY_LIMIT = 3;

    private final HealthCoachAgent healthCoachAgent;
    private final HealthService healthService;
    private final DeepSeekCostMonitor costMonitor;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AiPlanMapper aiPlanMapper;
    private final AiPlanDetailMapper aiPlanDetailMapper;
    private final DataMaskingService dataMaskingService;
    private final SafetyCheckerService safetyCheckerService;

    public PlanGenerateV2Service(HealthCoachAgent healthCoachAgent,
                                  HealthService healthService,
                                  DeepSeekCostMonitor costMonitor,
                                  RedisTemplate<String, Object> redisTemplate,
                                  AiPlanMapper aiPlanMapper,
                                  AiPlanDetailMapper aiPlanDetailMapper,
                                  DataMaskingService dataMaskingService,
                                  SafetyCheckerService safetyCheckerService) {
        this.healthCoachAgent = healthCoachAgent;
        this.healthService = healthService;
        this.costMonitor = costMonitor;
        this.redisTemplate = redisTemplate;
        this.aiPlanMapper = aiPlanMapper;
        this.aiPlanDetailMapper = aiPlanDetailMapper;
        this.dataMaskingService = dataMaskingService;
        this.safetyCheckerService = safetyCheckerService;
    }

    /**
     * V2 计划生成 — 使用 LangChain4j Agent + Function Calling。
     * 返回 AiAgentResponse（SDUI 协议），旧客户端可降级为纯文本。
     */
    @Transactional(rollbackFor = Exception.class)
    public AiAgentResponse generatePlan(PlanGenerateDTO dto, Long userId) {
        if (costMonitor.isGlobalCostExceeded()) {
            throw new BusinessException("今日AI调用额度已用尽，请明天再试");
        }

        // 频率限制
        String dateStr = LocalDate.now().toString();
        String limitKey = String.format(USER_LIMIT_KEY, userId, dateStr);
        Long callCount = redisTemplate.opsForValue().increment(limitKey, 1);
        if (callCount != null && callCount == 1) {
            redisTemplate.expire(limitKey, 1, TimeUnit.DAYS);
        }
        if (callCount != null && callCount > DAILY_LIMIT) {
            throw new BusinessException("今日AI计划生成次数已达上限(3次)，请明天再试");
        }

        HealthRecordVO health = healthService.getLatestHealthRecord(userId);

        // 构建用户消息（脱敏 + 注入防护）
        String userMessage = buildUserMessage(dto, health, userId);

        // 调用 LangChain4j Agent（含 Function Calling）
        // 启动 ToolCallContext 以捕获 Tool 调用记录
        ToolCallContext.start();
        String aiResponse;
        try {
            aiResponse = healthCoachAgent.generatePlan(userMessage);
        } catch (Exception e) {
            log.error("LangChain4j Agent 调用失败，回退到旧链路", e);
            ToolCallContext.clear();
            throw new BusinessException("AI服务调用失败，请稍后重试");
        }

        if (aiResponse == null || aiResponse.isBlank()) {
            ToolCallContext.clear();
            throw new BusinessException("AI返回内容为空");
        }

        // 安全检查：核查计划任务是否与用户健康状况冲突
        List<String> planItems = safetyCheckerService.extractPlanItems(aiResponse);
        SafetyCheckResult safetyResult = safetyCheckerService.checkPlan(userId, planItems);
        if (!safetyResult.isPassed()) {
            log.warn("V2计划被安全检查拦截 userId={} message={}", userId, safetyResult.getMessage());
            ToolCallContext.clear();
            throw new BusinessException(safetyResult.getMessage());
        }

        // 解析响应中的 JSON 提取 plan days
        AiPlan plan = savePlanFromResponse(userId, dto, aiResponse);

        // 构建 SDUI 响应（含 Tool 调用记录）
        return buildSduiResponse(plan, aiResponse);
    }

    private String buildUserMessage(PlanGenerateDTO dto, HealthRecordVO health, Long userId) {
        StringBuilder sb = new StringBuilder();
        sb.append("请为用户ID=").append(userId).append("生成一份个性化健康计划。\n");

        if (health != null) {
            sb.append("用户数据：身高").append(health.getHeight()).append("cm，");
            sb.append("体重").append(health.getWeight()).append("kg，");
            sb.append("BMI=").append(health.getBmi()).append("。");

            if (health.getGoal() != null) {
                sb.append("健康目标：").append(PromptSanitizer.sanitize(health.getGoal())).append("。");
            }
            if (health.getDiseaseHistory() != null && !health.getDiseaseHistory().isBlank()) {
                String masked = dataMaskingService.maskDiseaseHistory(health.getDiseaseHistory());
                sb.append("病史：").append(PromptSanitizer.sanitize(masked)).append("。");
            }
            if (health.getAllergyHistory() != null && !health.getAllergyHistory().isBlank()) {
                String masked = dataMaskingService.maskAllergyHistory(health.getAllergyHistory());
                sb.append("过敏史：").append(PromptSanitizer.sanitize(masked)).append("。");
            }
            if (health.getFamilyHistory() != null && !health.getFamilyHistory().isBlank()) {
                String masked = dataMaskingService.maskFamilyHistory(health.getFamilyHistory());
                sb.append("家族病史：").append(PromptSanitizer.sanitize(masked)).append("。");
            }
            if (health.getMedication() != null && !health.getMedication().isBlank()) {
                String masked = dataMaskingService.maskMedication(health.getMedication());
                sb.append("当前用药：").append(PromptSanitizer.sanitize(masked)).append("。");
            }
        }

        sb.append("\n计划类型：").append(dto.getPlanType());
        sb.append("，持续").append(dto.getDurationDays()).append("天。");
        if (dto.getIntensity() != null) {
            sb.append("偏好强度：").append(PromptSanitizer.sanitize(dto.getIntensity())).append("。");
        }
        if (dto.getTastePreference() != null) {
            sb.append("口味偏好：").append(PromptSanitizer.sanitize(dto.getTastePreference())).append("。");
        }

        sb.append("\n你可以利用可用的工具（如查询用户运动偏好、现有计划、今日热量摄入等）获取更多上下文，");
        sb.append("然后基于这些信息生成一个分天的个性化健康计划。\n");

        sb.append("\n重要输出格式要求：\n");
        sb.append("1. 如果计划类型为运动(sport)，请将每天的每项运动任务拆分为结构化阶段，输出格式为：\n");
        sb.append("{ \"days\": [{ \"d\": 1, \"items\": [{\"name\":\"跑步训练\",\"type\":\"sport\",\"durationMin\":30,");
        sb.append("\"phases\":[{");
        sb.append("\"name\":\"热身\",\"type\":\"warmup\",\"minutes\":5,\"instruction\":\"慢跑+动态拉伸\"");
        sb.append("},{");
        sb.append("\"name\":\"核心跑步\",\"type\":\"core\",\"minutes\":22,\"instruction\":\"保持目标心率区间匀速跑\"");
        sb.append("},{");
        sb.append("\"name\":\"放松拉伸\",\"type\":\"cooldown\",\"minutes\":3,\"instruction\":\"静态拉伸腿部和髋部肌群\"");
        sb.append("}]}]}]}\n");
        sb.append("2. 如果计划类型为饮食(diet)，输出原来的扁平格式即可：{\"days\":[{\"d\":天数,\"items\":[\"具体任务\"]}]}\n");
        sb.append("3. 必须把JSON包裹在```json和```代码块中。\n");
        sb.append("4. 每个phase必须有 name、type、minutes、instruction 四个字段。");

        return sb.toString();
    }

    private AiPlan savePlanFromResponse(Long userId, PlanGenerateDTO dto, String aiResponse) {
        AiPlan plan = new AiPlan();
        plan.setUserId(userId);
        plan.setPlanType(dto.getPlanType());
        plan.setPlanName(buildPlanName(dto));
        plan.setDurationDays(dto.getDurationDays());
        plan.setAiContent(aiResponse);
        plan.setStartDate(LocalDate.now());
        plan.setStatus(1);

        aiPlanMapper.insert(plan);

        // 尝试从响应中提取 JSON 并保存 detail
        try {
            JsonNode root = AiResponseParser.extractJson(aiResponse);
            JsonNode days = root.path("days");
            if (days.isArray()) {
                for (JsonNode dayNode : days) {
                    int daySeq = dayNode.path("d").asInt();
                    JsonNode items = dayNode.path("items");
                    if (!items.isArray()) continue;
                    for (JsonNode itemNode : items) {
                        // 支持结构化phase格式（新格式）和扁平字符串格式（旧格式兼容）
                        if (itemNode.isObject()) {
                            String itemName = itemNode.path("name").asText();
                            String itemType = itemNode.path("type").asText("sport");
                            int durationMin = itemNode.path("durationMin").asInt(0);
                            JsonNode phases = itemNode.path("phases");
                            if (phases.isArray() && phases.size() > 0) {
                                for (int pi = 0; pi < phases.size(); pi++) {
                                    JsonNode phase = phases.get(pi);
                                    AiPlanDetail detail = new AiPlanDetail();
                                    detail.setPlanId(plan.getId());
                                    detail.setDaySequence(daySeq);
                                    detail.setItemType(itemType);
                                    detail.setItemName(itemName);
                                    detail.setTargetAmount(String.valueOf(durationMin) + "min");
                                    detail.setStatus(0);
                                    detail.setSubPhase(phase.path("name").asText());
                                    detail.setSubPhaseType(phase.path("type").asText());
                                    detail.setPhaseOrder(pi + 1);
                                    detail.setPhaseDurationMinutes(phase.path("minutes").asInt(0));
                                    aiPlanDetailMapper.insert(detail);
                                }
                            } else {
                                // 运动任务无phase时，作为整体插入
                                AiPlanDetail detail = new AiPlanDetail();
                                detail.setPlanId(plan.getId());
                                detail.setDaySequence(daySeq);
                                detail.setItemType(itemType);
                                detail.setItemName(itemName);
                                detail.setTargetAmount(durationMin + "min");
                                detail.setStatus(0);
                                aiPlanDetailMapper.insert(detail);
                            }
                        } else {
                            // 兼容旧格式：扁平字符串
                            String text = itemNode.asText();
                            if (text == null || text.isBlank()) continue;
                            AiPlanDetail detail = new AiPlanDetail();
                            detail.setPlanId(plan.getId());
                            detail.setDaySequence(daySeq);
                            detail.setItemType(dto.getPlanType());
                            detail.setItemName(text);
                            detail.setTargetAmount("");
                            detail.setStatus(0);
                            aiPlanDetailMapper.insert(detail);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("V2 计划 detail 解析失败，跳过 detail 落库 planId={}", plan.getId(), e);
        }

        log.info("V2计划生成成功 userId={} planId={}", userId, plan.getId());
        return plan;
    }

    private AiAgentResponse buildSduiResponse(AiPlan plan, String aiResponse) {
        List<Widget> widgets = new ArrayList<>();

        // 文本块展示计划内容
        TextBlockWidget textBlock = new TextBlockWidget();
        textBlock.setTitle("计划内容");
        textBlock.setContent(aiResponse);
        widgets.add(textBlock);

        // 进度环展示计划概览
        ProgressRingWidget progressRing = new ProgressRingWidget();
        progressRing.setTitle("计划进度");
        progressRing.setPercentage(0.0);
        progressRing.setLabel(plan.getPlanName());
        progressRing.setColor("blue");
        progressRing.setSubText("共" + plan.getDurationDays() + "天");
        widgets.add(progressRing);

        // 提示条
        TipWidget tip = new TipWidget();
        tip.setTitle("温馨提示");
        tip.setContent("计划已生成，可根据实际情况反馈调整强度。");
        tip.setCategory("info");
        widgets.add(tip);

        // 从 ToolCallContext 提取 Tool 调用记录并转换为 SDUI 协议
        List<ToolCallResult> toolCalls = ToolCallResult.from(ToolCallContext.getRecords());

        // 清理 ThreadLocal
        ToolCallContext.clear();

        return AiAgentResponse.builder()
                .protocolVersion("1.0")
                .text(aiResponse)
                .widgets(widgets)
                .toolCalls(toolCalls)
                .disclaimer("本建议仅供参考，不构成医疗诊断或处方。如有健康问题请咨询专业医生。")
                .build();
    }

    private String buildPlanName(PlanGenerateDTO dto) {
        String prefix;
        switch (dto.getPlanType()) {
            case "sport": prefix = "运动"; break;
            case "diet": prefix = "饮食"; break;
            case "comprehensive": prefix = "综合"; break;
            case "rehabilitation": prefix = "康复"; break;
            case "meditation": prefix = "冥想"; break;
            default: prefix = "健康";
        }
        return prefix + "计划(" + dto.getDurationDays() + "天)";
    }
}