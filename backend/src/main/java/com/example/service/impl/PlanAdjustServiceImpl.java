package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.BusinessException;
import com.example.convert.AiPlanConvert;
import com.example.dto.PlanGenerateDTO;
import com.example.entity.AiPlan;
import com.example.entity.AiPlanDetail;
import com.example.entity.DailyCheckin;
import com.example.entity.HealthRecord;
import com.example.mapper.AiPlanDetailMapper;
import com.example.mapper.AiPlanMapper;
import com.example.mapper.DailyCheckinMapper;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.service.*;
import com.example.vo.AiPlanDetailVO;
import com.example.vo.HealthRecordVO;
import lombok.extern.slf4j.Slf4j;
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

    public PlanAdjustServiceImpl(AiPlanMapper aiPlanMapper,
                                  AiPlanDetailMapper aiPlanDetailMapper,
                                  DailyCheckinMapper dailyCheckinMapper,
                                  HealthService healthService,
                                  DeepSeekService deepSeekService,
                                  AiPlanService aiPlanService,
                                  DeepSeekCostMonitor costMonitor,
                                  AiPlanConvert aiPlanConvert) {
        this.aiPlanMapper = aiPlanMapper;
        this.aiPlanDetailMapper = aiPlanDetailMapper;
        this.dailyCheckinMapper = dailyCheckinMapper;
        this.healthService = healthService;
        this.deepSeekService = deepSeekService;
        this.aiPlanService = aiPlanService;
        this.costMonitor = costMonitor;
        this.aiPlanConvert = aiPlanConvert;
    }

    @Override
    @Transactional
    public AiPlanDetailVO adjustPlan(Long originalPlanId, Long userId, String feedback) {
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

        // 构建调整Prompt
        String adjustPrompt = String.format(
                "你是专业健康教练。用户正在进行一个%d天的%s计划。\n" +
                        "当前用户数据：身高%.1fcm，体重%.1fkg，BMI%.1f，健康目标：%s。\n" +
                        "原计划内容：%s\n" +
                        "计划执行统计：%s\n" +
                        "用户反馈：%s\n\n" +
                        "请根据以上数据，生成一个调整后的新计划，输出格式与原计划相同：\n" +
                        "{\"days\":[{\"d\":天数,\"items\":[\"具体可执行任务1\",\"任务2\"]}]}\n" +
                        "调整原则：如果用户觉得太轻松就加大强度，觉得太难就降低强度；" +
                        "如果特定部位不适（如膝盖痛）则替换为低冲击运动；" +
                        "如果饮食计划不合理则优化食物搭配。只输出JSON。",
                originalPlan.getDurationDays(),
                originalPlan.getPlanType().equals("sport") ? "运动" : "饮食",
                health.getHeight(), health.getWeight(), health.getBmi(), health.getGoal() != null ? health.getGoal() : "未设定",
                truncate(originalPlan.getAiContent(), 500),
                statsJson,
                feedback != null && !feedback.isEmpty() ? feedback : "无特殊反馈"
        );

        // 调用DeepSeek生成调整后的计划
        String adjustedContent = deepSeekService.callApiRaw(adjustPrompt);

        // 创建新计划
        AiPlan newPlan = new AiPlan();
        newPlan.setUserId(userId);
        newPlan.setPlanType(originalPlan.getPlanType());
        newPlan.setPlanName(originalPlan.getPlanName() + "-调整版");
        newPlan.setDurationDays(originalPlan.getDurationDays());
        newPlan.setAiContent(adjustedContent);
        newPlan.setStartDate(LocalDate.now());
        newPlan.setStatus(1);

        // 将旧计划的生效状态取消
        originalPlan.setStatus(2);
        aiPlanMapper.updateById(originalPlan);

        aiPlanMapper.insert(newPlan);

        // 解析并保存detail
        saveDetails(newPlan.getId(), adjustedContent, newPlan.getPlanType());

        log.info("AI动态调整计划成功 originalPlanId={} newPlanId={}", originalPlanId, newPlan.getId());
        return aiPlanConvert.toAiPlanDetailVO(newPlan);
    }

    private String buildStatsJson(Long planId, Long userId) {
        // 获取计划开始后的打卡记录
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
                        for (JsonNode item : items) {
                            AiPlanDetail detail = new AiPlanDetail();
                            detail.setPlanId(planId);
                            detail.setDaySequence(daySeq);
                            detail.setItemType(planType);
                            detail.setItemName(item.asText());
                            detail.setTargetAmount("按需完成");
                            aiPlanDetailMapper.insert(detail);
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