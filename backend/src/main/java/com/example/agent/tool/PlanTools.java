package com.example.agent.tool;

import com.example.mapper.AiPlanMapper;
import com.example.mapper.DailyCheckinMapper;
import com.example.mapper.DietRecordMapper;
import com.example.mapper.ExerciseItemMapper;
import com.example.mapper.ExerciseRecordMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * LangChain4j Function Calling 工具集。
 * 模型通过 Tool 调用本地业务能力，替代手写 JSON Prompt 约束。
 * Tool 调用采用"建议-确认"模式：初期只生成建议草稿，用户确认后才落库。
 */
@Slf4j
@Component
public class PlanTools {

    private final AiPlanMapper aiPlanMapper;
    private final DietRecordMapper dietRecordMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final DailyCheckinMapper dailyCheckinMapper;
    private final ExerciseItemMapper exerciseItemMapper;

    public PlanTools(AiPlanMapper aiPlanMapper,
                     DietRecordMapper dietRecordMapper,
                     ExerciseRecordMapper exerciseRecordMapper,
                     DailyCheckinMapper dailyCheckinMapper,
                     ExerciseItemMapper exerciseItemMapper) {
        this.aiPlanMapper = aiPlanMapper;
        this.dietRecordMapper = dietRecordMapper;
        this.exerciseRecordMapper = exerciseRecordMapper;
        this.dailyCheckinMapper = dailyCheckinMapper;
        this.exerciseItemMapper = exerciseItemMapper;
    }

    @Tool("查询用户当前的生效计划信息")
    public String getActivePlan(@P("用户ID") Long userId) {
        log.info("Tool调用: getActivePlan userId={}", userId);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.AiPlan>()
                .eq(com.example.entity.AiPlan::getUserId, userId)
                .eq(com.example.entity.AiPlan::getStatus, 1);
        com.example.entity.AiPlan plan = aiPlanMapper.selectOne(wrapper);
        if (plan == null) {
            return "用户暂无生效计划";
        }
        return String.format("计划ID=%d, 类型=%s, 名称=%s, 持续%d天, 开始日期=%s",
                plan.getId(), plan.getPlanType(), plan.getPlanName(),
                plan.getDurationDays(), plan.getStartDate());
    }

    @Tool("记录用户单次饮食摄入")
    public String recordDiet(
            @P("用户ID") Long userId,
            @P("食物名称") String foodName,
            @P("摄入热量(千卡)") int calories,
            @P("蛋白质(克)") int protein,
            @P("碳水化合物(克)") int carbs,
            @P("脂肪(克)") int fat) {
        log.info("Tool调用: recordDiet userId={} food={} cal={}", userId, foodName, calories);
        // 建议-确认模式：仅返回记录摘要，不直接落库
        return String.format("已记录饮食: %s, 热量%d千卡, 蛋白质%dg, 碳水%dg, 脂肪%dg。请确认后保存。",
                foodName, calories, protein, carbs, fat);
    }

    @Tool("查询用户今日已摄入总热量")
    public String getTodayCalorieStatus(@P("用户ID") Long userId) {
        log.info("Tool调用: getTodayCalorieStatus userId={}", userId);
        java.time.LocalDate today = java.time.LocalDate.now();
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.DietRecord>()
                .eq(com.example.entity.DietRecord::getUserId, userId)
                .ge(com.example.entity.DietRecord::getCreateTime, today.atStartOfDay());
        var records = dietRecordMapper.selectList(wrapper);
        int totalCalories = records.stream()
                .mapToInt(r -> r.getCaloriesConsumed() != null ? r.getCaloriesConsumed() : 0)
                .sum();
        return String.format("今日已摄入%d千卡", totalCalories);
    }

    @Tool("查询用户近7天的运动完成率")
    public String getExerciseCompletionRate(@P("用户ID") Long userId) {
        log.info("Tool调用: getExerciseCompletionRate userId={}", userId);
        java.time.LocalDate sevenDaysAgo = java.time.LocalDate.now().minusDays(7);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.DailyCheckin>()
                .eq(com.example.entity.DailyCheckin::getUserId, userId)
                .ge(com.example.entity.DailyCheckin::getCheckDate, sevenDaysAgo);
        var checkins = dailyCheckinMapper.selectList(wrapper);
        if (checkins.isEmpty()) {
            return "近7天无打卡记录";
        }
        long completed = checkins.stream()
                .filter(c -> c.getExerciseStatus() != null && c.getExerciseStatus() >= 1)
                .count();
        int rate = (int) (completed * 100.0 / checkins.size());
        return String.format("近7天运动完成率=%d%%（%d/%d天完成）", rate, completed, checkins.size());
    }

    @Tool("查询用户历史运动偏好（已完成最多的运动类型）")
    public String getExercisePreference(@P("用户ID") Long userId) {
        log.info("Tool调用: getExercisePreference userId={}", userId);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.ExerciseRecord>()
                .eq(com.example.entity.ExerciseRecord::getUserId, userId)
                .orderByDesc(com.example.entity.ExerciseRecord::getCreateTime)
                .last("LIMIT 20");
        var records = exerciseRecordMapper.selectList(wrapper);
        if (records.isEmpty()) {
            return "暂无运动历史记录";
        }
        // 统计最常见的运动类型（通过 itemId 关联 ExerciseItem 获取类型名）
        java.util.Map<String, Long> typeCount = new java.util.LinkedHashMap<>();
        for (var r : records) {
            String typeName = "未知";
            if (r.getItemId() != null) {
                var item = exerciseItemMapper.selectById(r.getItemId());
                if (item != null && item.getType() != null) {
                    typeName = item.getType();
                }
            }
            typeCount.merge(typeName, 1L, Long::sum);
        }
        String preferred = typeCount.entrySet().stream()
                .max(java.util.Map.Entry.comparingByValue())
                .map(e -> e.getKey() + "(共" + e.getValue() + "次)")
                .orElse("未知");
        return "用户偏好运动: " + preferred;
    }

    @Tool("根据用户反馈和健康状况调整计划建议")
    public String suggestPlanAdjustment(
            @P("用户ID") Long userId,
            @P("调整原因，如'膝盖疼痛'、'强度太高'、'时间不够'") String reason) {
        log.info("Tool调用: suggestPlanAdjustment userId={} reason={}", userId, reason);
        return String.format("已记录调整原因: %s。将根据此原因优化后续计划建议。", reason);
    }
}