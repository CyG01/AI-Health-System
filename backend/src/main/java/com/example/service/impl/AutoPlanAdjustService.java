package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.*;
import com.example.mapper.*;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.resilience.ModelRouter;
import com.example.service.HealthService;
import com.example.util.PromptSanitizer;
import com.example.vo.HealthRecordVO;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * 自动计划调整服务。
 * 实现「数据变化/用户反馈 → 自动计划再生」的闭环。
 *
 * 触发条件：
 *  - 睡眠不足 → 自动降低运动强度
 *  - 体重下降 → 自动调整饮食热量目标
 *  - 热量超标 → 自动调整饮食计划
 *  - 用户反馈难度过高 → 自动降低强度并重新生成计划
 *  - 连续低完成率 → 自动调整计划
 */
@Slf4j
@Service
public class AutoPlanAdjustService {

    private static final int SLEEP_THRESHOLD_MIN = 360;  // 6小时
    private static final double WEIGHT_CHANGE_THRESHOLD = 2.0;  // 体重变化≥2kg
    private static final int CALORIE_OVER_THRESHOLD = 2400;     // 2400大卡

    private final AiPlanMapper aiPlanMapper;
    private final AiPlanDetailMapper aiPlanDetailMapper;
    private final DailyCheckinMapper dailyCheckinMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final HealthService healthService;
    private final DeepSeekCostMonitor costMonitor;
    private final ModelRouter modelRouter;
    private final SysNotificationMapper sysNotificationMapper;

    public AutoPlanAdjustService(AiPlanMapper aiPlanMapper,
                                  AiPlanDetailMapper aiPlanDetailMapper,
                                  DailyCheckinMapper dailyCheckinMapper,
                                  HealthRecordMapper healthRecordMapper,
                                  HealthService healthService,
                                  DeepSeekCostMonitor costMonitor,
                                  ModelRouter modelRouter,
                                  SysNotificationMapper sysNotificationMapper) {
        this.aiPlanMapper = aiPlanMapper;
        this.aiPlanDetailMapper = aiPlanDetailMapper;
        this.dailyCheckinMapper = dailyCheckinMapper;
        this.healthRecordMapper = healthRecordMapper;
        this.healthService = healthService;
        this.costMonitor = costMonitor;
        this.modelRouter = modelRouter;
        this.sysNotificationMapper = sysNotificationMapper;
    }

    /**
     * 睡眠不足 → 自动降低当前运动计划强度。
     */
    @Async
    @Transactional
    public void adjustForInsufficientSleep(Long userId, int sleepMinutes) {
        AiPlan activePlan = getActiveSportPlan(userId);
        if (activePlan == null) return;

        log.info("睡眠不足自动调整计划 userId={} sleepMinutes={} planId={}",
                userId, sleepMinutes, activePlan.getId());

        HealthRecordVO health = healthService.getLatestHealthRecord(userId);
        String prompt = buildSleepAdjustPrompt(activePlan, health, sleepMinutes);

        try {
            String adjustedContent = modelRouter.singleChat(
                    "你是专业健康教练，根据用户睡眠不足的情况自动调整运动计划。必须输出结构化JSON。",
                    prompt, "plan_generate");

            applyPlanAdjustment(activePlan, userId, adjustedContent, "SLEEP_ADJUST");
            notifyUser(userId, "SLEEP_ADJUST",
                    "睡眠不足，计划已自动调整",
                    "昨晚睡眠不足（" + sleepMinutes + "分钟），已将运动计划自动调整为低强度恢复性训练。好好休息也是健康的一部分！");
        } catch (Exception e) {
            log.error("睡眠自动调整失败 userId={}", userId, e);
        }
    }

    /**
     * 体重变化 → 自动调整饮食计划热量目标。
     */
    @Async
    @Transactional
    public void adjustForWeightChange(Long userId, double oldWeight, double newWeight) {
        AiPlan activePlan = getActiveDietPlan(userId);
        if (activePlan == null) {
            // 如果没有饮食计划，创建一个
            activePlan = getActiveSportPlan(userId);
        }

        log.info("体重变化自动调整计划 userId={} oldWeight={} newWeight={}",
                userId, oldWeight, newWeight);

        HealthRecordVO health = healthService.getLatestHealthRecord(userId);
        String direction = newWeight < oldWeight ? "下降" : "上升";
        String prompt = String.format(
                "用户体重从%.1fkg变为%.1fkg（%s了%.1fkg），BMI=%.1f，健康目标：%s。\n" +
                        "请自动调整用户的饮食计划，重新计算每日热量摄入目标。\n" +
                        "以JSON格式输出，区分运动(sport)和饮食(diet)两个planType，每个都必须有days数组：\n" +
                        "{\"plans\":{\"sport\":{\"days\":[{\"d\":1,\"items\":[\"...\"]}]},\"diet\":{\"days\":[{\"d\":1,\"items\":[\"...\"]}]}}}\n" +
                        "如果体重下降且目标为减重，可适当减少热量但不低于1200大卡。如果体重下降但目标为增肌，应提高蛋白质摄入增加热量。",
                oldWeight, newWeight, direction, Math.abs(newWeight - oldWeight),
                health.getBmi(),
                PromptSanitizer.sanitize(health.getGoal() != null ? health.getGoal() : "保持健康"));

        try {
            String adjustedContent = modelRouter.singleChat(
                    "你是专业营养师，根据体重变化自动调整饮食计划。输出包含sport和diet两个部分的JSON。",
                    prompt, "plan_generate");

            // 解析并分别处理运动和饮食计划
            applyWeightChangeAdjustment(userId, adjustedContent);
            notifyUser(userId, "WEIGHT_CHANGE",
                    "体重变化，饮食计划已自动调整",
                    String.format("体重%s了%.1fkg，已自动调整饮食热量目标和运动建议。",
                            direction, Math.abs(newWeight - oldWeight)));
        } catch (Exception e) {
            log.error("体重变化自动调整失败 userId={}", userId, e);
        }
    }

    /**
     * 用户反馈难度过高 → 自动降低强度。
     */
    @Async
    @Transactional
    public void adjustForDifficultyFeedback(Long userId, String feedback, int satisfactionScore) {
        AiPlan activePlan = getActiveSportPlan(userId);
        if (activePlan == null) return;

        log.info("用户反馈难度自动调整 userId={} score={} feedback={}",
                userId, satisfactionScore, feedback);

        HealthRecordVO health = healthService.getLatestHealthRecord(userId);
        String prompt = String.format(
                "用户对当前计划不满意（评分%d/5），反馈：%s。\n" +
                        "当前计划内容：%s\n" +
                        "请自动调整计划。如果反馈表示太累/太难，请大幅降低强度并增加休息日；" +
                        "如果表示太轻松，请适度增加强度。输出结构化运动计划JSON（含phase拆分）：\n" +
                        "{\"days\":[{\"d\":1,\"items\":[{\"name\":\"...\",\"type\":\"sport\",\"durationMin\":20," +
                        "\"phases\":[{\"name\":\"热身\",\"type\":\"warmup\",\"minutes\":5,\"instruction\":\"...\"}," +
                        "{\"name\":\"核心训练\",\"type\":\"core\",\"minutes\":12,\"instruction\":\"...\"}," +
                        "{\"name\":\"放松\",\"type\":\"cooldown\",\"minutes\":3,\"instruction\":\"...\"}]}]}]}",
                satisfactionScore,
                PromptSanitizer.sanitize(feedback),
                truncate(activePlan.getAiContent(), 300));

        try {
            String adjustedContent = modelRouter.singleChat(
                    "你是专业健康教练，根据用户反馈自动调整计划难度。输出结构化JSON。",
                    prompt, "plan_generate");

            applyPlanAdjustment(activePlan, userId, adjustedContent, "DIFFICULTY_ADJUST");
            notifyUser(userId, "DIFFICULTY_ADJUST",
                    "计划已自动调整",
                    "根据你的反馈，已自动调整计划强度。如果仍有不适，可以继续反馈。");
        } catch (Exception e) {
            log.error("难度反馈自动调整失败 userId={}", userId, e);
        }
    }

    /**
     * 连续低完成率 → 自动调整计划。
     */
    @Async
    @Transactional
    public void adjustForLowCompletionRate(Long userId, double completionRate) {
        AiPlan activePlan = getActiveSportPlan(userId);
        if (activePlan == null) return;

        log.info("低完成率自动调整 userId={} rate={}%", userId, completionRate);

        HealthRecordVO health = healthService.getLatestHealthRecord(userId);
        String prompt = String.format(
                "用户的当前计划完成率仅%.0f%%，需要大幅简化计划。\n" +
                        "当前计划：%s\n用户BMI=%.1f，目标：%s。\n" +
                        "请生成一个大幅简化的运动计划，降低频率和强度，加入更多休息日。\n" +
                        "输出结构化JSON（含phase拆分），用```json包裹。",
                completionRate, truncate(activePlan.getAiContent(), 300),
                health.getBmi(),
                PromptSanitizer.sanitize(health.getGoal() != null ? health.getGoal() : "保持健康"));

        try {
            String adjustedContent = modelRouter.singleChat(
                    "你是专业健康教练，根据低完成率简化计划。输出结构化JSON。",
                    prompt, "plan_generate");

            applyPlanAdjustment(activePlan, userId, adjustedContent, "LOW_COMPLETION");
            notifyUser(userId, "LOW_COMPLETION",
                    "计划已自动简化",
                    "注意到你最近完成率较低，已将计划自动调整为更轻松的版本。慢慢来，保持节奏最重要！");
        } catch (Exception e) {
            log.error("低完成率自动调整失败 userId={}", userId, e);
        }
    }

    /**
     * 热量超标 → 自动调整饮食计划。
     */
    @Async
    @Transactional
    public void adjustForCalorieOverflow(Long userId, int totalCalories) {
        AiPlan activePlan = getActiveDietPlan(userId);
        if (activePlan == null) return;

        log.info("热量超标自动调整 userId={} totalCal={}", userId, totalCalories);

        String prompt = String.format(
                "用户今日已摄入%d大卡，超过建议量。当前饮食计划：%s。\n" +
                        "请自动调整为更严格的低热量饮食计划，减少主食和高油食物，增加蔬菜和蛋白质。\n" +
                        "输出JSON格式：{\"days\":[{\"d\":天数,\"items\":[\"具体饮食建议\"]}]}",
                totalCalories, truncate(activePlan.getAiContent(), 300));

        try {
            String adjustedContent = modelRouter.singleChat(
                    "你是专业营养师，自动调整饮食计划以控制热量。输出JSON。",
                    prompt, "plan_generate");

            applyPlanAdjustment(activePlan, userId, adjustedContent, "CALORIE_ADJUST");
            notifyUser(userId, "CALORIE_ADJUST",
                    "饮食计划已自动调整",
                    String.format("今日已摄入%d大卡，已将饮食计划自动调整为低热量方案。", totalCalories));
        } catch (Exception e) {
            log.error("热量超标自动调整失败 userId={}", userId, e);
        }
    }

    // ============================================================
    // 辅助方法
    // ============================================================

    private AiPlan getActiveSportPlan(Long userId) {
        List<AiPlan> plans = aiPlanMapper.selectList(
                new LambdaQueryWrapper<AiPlan>()
                        .eq(AiPlan::getUserId, userId)
                        .eq(AiPlan::getStatus, 1)
                        .eq(AiPlan::getPlanType, "sport"));
        return plans.isEmpty() ? null : plans.get(0);
    }

    private AiPlan getActiveDietPlan(Long userId) {
        List<AiPlan> plans = aiPlanMapper.selectList(
                new LambdaQueryWrapper<AiPlan>()
                        .eq(AiPlan::getUserId, userId)
                        .eq(AiPlan::getStatus, 1)
                        .eq(AiPlan::getPlanType, "diet"));
        return plans.isEmpty() ? null : plans.get(0);
    }

    @Transactional
    public void applyPlanAdjustment(AiPlan oldPlan, Long userId, String newContent, String adjustType) {
        // 停用旧计划
        oldPlan.setStatus(2);
        aiPlanMapper.updateById(oldPlan);

        // 创建新计划
        AiPlan newPlan = new AiPlan();
        newPlan.setUserId(userId);
        newPlan.setPlanType(oldPlan.getPlanType());
        newPlan.setPlanName(oldPlan.getPlanName() + "-自动调整");
        newPlan.setDurationDays(oldPlan.getDurationDays());
        newPlan.setAiContent(newContent);
        newPlan.setStartDate(LocalDate.now());
        newPlan.setStatus(1);
        aiPlanMapper.insert(newPlan);

        // 解析并保存 detail
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode root = mapper.readTree(newContent);
            JsonNode days = root.path("days");
            if (days.isArray()) {
                for (JsonNode day : days) {
                    int daySeq = day.path("d").asInt();
                    JsonNode items = day.path("items");
                    if (!items.isArray()) continue;
                    for (JsonNode itemNode : items) {
                        if (itemNode.isObject()) {
                            String itemName = itemNode.path("name").asText();
                            JsonNode phases = itemNode.path("phases");
                            if (phases.isArray() && phases.size() > 0) {
                                for (int pi = 0; pi < phases.size(); pi++) {
                                    JsonNode phase = phases.get(pi);
                                    AiPlanDetail detail = new AiPlanDetail();
                                    detail.setPlanId(newPlan.getId());
                                    detail.setDaySequence(daySeq);
                                    detail.setItemType(oldPlan.getPlanType());
                                    detail.setItemName(itemName);
                                    detail.setTargetAmount(itemNode.path("durationMin").asInt(0) + "min");
                                    detail.setSubPhase(phase.path("name").asText());
                                    detail.setSubPhaseType(phase.path("type").asText());
                                    detail.setPhaseOrder(pi + 1);
                                    detail.setPhaseDurationMinutes(phase.path("minutes").asInt(0));
                                    detail.setStatus(0);
                                    aiPlanDetailMapper.insert(detail);
                                }
                            } else {
                                AiPlanDetail detail = new AiPlanDetail();
                                detail.setPlanId(newPlan.getId());
                                detail.setDaySequence(daySeq);
                                detail.setItemType(oldPlan.getPlanType());
                                detail.setItemName(itemName);
                                detail.setTargetAmount(itemNode.path("durationMin").asInt(0) + "min");
                                detail.setStatus(0);
                                aiPlanDetailMapper.insert(detail);
                            }
                        } else {
                            AiPlanDetail detail = new AiPlanDetail();
                            detail.setPlanId(newPlan.getId());
                            detail.setDaySequence(daySeq);
                            detail.setItemType(oldPlan.getPlanType());
                            detail.setItemName(itemNode.asText());
                            detail.setTargetAmount("按需完成");
                            detail.setStatus(0);
                            aiPlanDetailMapper.insert(detail);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("自动调整 detail 解析失败 planId={}", newPlan.getId(), e);
        }

        log.info("自动调整计划完成 userId={} oldPlanId={} newPlanId={} type={}",
                userId, oldPlan.getId(), newPlan.getId(), adjustType);
    }

    /**
     * 体重变化的调整需要同时处理运动和饮食两个计划。
     */
    private void applyWeightChangeAdjustment(Long userId, String content) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode root = mapper.readTree(content);
            JsonNode plans = root.path("plans");

            // 调整运动计划
            AiPlan sportPlan = getActiveSportPlan(userId);
            if (sportPlan != null && plans.has("sport")) {
                String sportContent = plans.path("sport").toString();
                applyPlanAdjustment(sportPlan, userId, sportContent, "WEIGHT_CHANGE_SPORT");
            }

            // 调整饮食计划
            AiPlan dietPlan = getActiveDietPlan(userId);
            if (dietPlan != null && plans.has("diet")) {
                String dietContent = plans.path("diet").toString();
                applyPlanAdjustment(dietPlan, userId, dietContent, "WEIGHT_CHANGE_DIET");
            } else if (plans.has("diet")) {
                // 如果没有饮食计划，创建一个
                String dietContent = plans.path("diet").toString();
                AiPlan newPlan = new AiPlan();
                newPlan.setUserId(userId);
                newPlan.setPlanType("diet");
                newPlan.setPlanName("饮食计划(自动生成)");
                newPlan.setDurationDays(7);
                newPlan.setAiContent(dietContent);
                newPlan.setStartDate(LocalDate.now());
                newPlan.setStatus(1);
                aiPlanMapper.insert(newPlan);
                saveFlatDetails(newPlan.getId(), dietContent);
            }
        } catch (Exception e) {
            log.error("体重变化调整解析失败 userId={}", userId, e);
        }
    }

    private void saveFlatDetails(Long planId, String content) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode root = mapper.readTree(content);
            JsonNode days = root.path("days");
            if (days.isArray()) {
                for (JsonNode day : days) {
                    int daySeq = day.path("d").asInt();
                    JsonNode items = day.path("items");
                    if (items.isArray()) {
                        for (JsonNode item : items) {
                            AiPlanDetail detail = new AiPlanDetail();
                            detail.setPlanId(planId);
                            detail.setDaySequence(daySeq);
                            detail.setItemType("diet");
                            detail.setItemName(item.asText());
                            detail.setTargetAmount("按需完成");
                            detail.setStatus(0);
                            aiPlanDetailMapper.insert(detail);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("饮食detail解析失败 planId={}", planId, e);
        }
    }

    private String buildSleepAdjustPrompt(AiPlan plan, HealthRecordVO health, int sleepMinutes) {
        return String.format(
                "用户昨晚睡眠仅%d分钟（不足6小时），当前运动计划：%s。\n" +
                        "用户BMI=%.1f，健康目标：%s。\n" +
                        "请自动调整运动计划：\n" +
                        "1. 将今天的运动改为低强度恢复训练（瑜伽/拉伸/慢走）\n" +
                        "2. 取消高强度运动\n" +
                        "3. 保持原有的结构化输出格式（含热身/核心/放松三阶段）\n" +
                        "输出JSON：{\"days\":[{\"d\":1,\"items\":[...]}]}，用```json包裹。",
                sleepMinutes, truncate(plan.getAiContent(), 300),
                health.getBmi(),
                PromptSanitizer.sanitize(health.getGoal() != null ? health.getGoal() : "保持健康"));
    }

    private void notifyUser(Long userId, String type, String title, String content) {
        try {
            SysNotification notification = new SysNotification();
            notification.setUserId(userId);
            notification.setTitle("[自动调整] " + title);
            notification.setContent(content);
            notification.setType("AUTO_ADJUST");
            notification.setTargetType("plan");
            notification.setIsRead(0);
            sysNotificationMapper.insert(notification);
        } catch (Exception e) {
            log.error("推送自动调整通知失败 userId={}", userId, e);
        }
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}