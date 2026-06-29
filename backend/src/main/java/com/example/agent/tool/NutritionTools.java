package com.example.agent.tool;

import com.example.mapper.DietRecordMapper;
import com.example.mapper.FoodItemMapper;
import com.example.mapper.HealthRecordMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 营养师 Agent 的工具集。
 * 提供饮食记录、营养查询、热量计算等能力。
 */
@Slf4j
@Component
public class NutritionTools {

    private final DietRecordMapper dietRecordMapper;
    private final FoodItemMapper foodItemMapper;
    private final HealthRecordMapper healthRecordMapper;

    public NutritionTools(DietRecordMapper dietRecordMapper, FoodItemMapper foodItemMapper,
                          HealthRecordMapper healthRecordMapper) {
        this.dietRecordMapper = dietRecordMapper;
        this.foodItemMapper = foodItemMapper;
        this.healthRecordMapper = healthRecordMapper;
    }

    @Tool("查询用户指定日期的饮食记录汇总")
    public String getDailyDietSummary(
            @P("用户ID") Long userId,
            @P("查询日期（yyyy-MM-dd格式，默认今天）") String date) {
        log.info("Tool调用: getDailyDietSummary userId={} date={}", userId, date);
        LocalDate queryDate = (date != null && !date.isBlank())
                ? LocalDate.parse(date) : LocalDate.now();

        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.DietRecord>()
                .eq(com.example.entity.DietRecord::getUserId, userId)
                .ge(com.example.entity.DietRecord::getCreateTime, queryDate.atStartOfDay())
                .lt(com.example.entity.DietRecord::getCreateTime, queryDate.plusDays(1).atStartOfDay());
        var records = dietRecordMapper.selectList(wrapper);

        if (records.isEmpty()) {
            return String.format("%s 暂无饮食记录", queryDate);
        }

        int totalCal = records.stream().mapToInt(r -> r.getCaloriesConsumed() != null ? r.getCaloriesConsumed() : 0).sum();
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s 饮食汇总：共摄入 %d 千卡\n", queryDate, totalCal));
        for (int i = 0; i < records.size(); i++) {
            var r = records.get(i);
            sb.append(String.format("  %d. %s %d千卡\n", i + 1,
                    r.getRemark() != null ? r.getRemark() : "未记录",
                    r.getCaloriesConsumed() != null ? r.getCaloriesConsumed() : 0));
        }
        return sb.toString();
    }

    @Tool("查询近N天的热量摄入趋势")
    public String getCalorieTrend(
            @P("用户ID") Long userId,
            @P("天数（默认7天）") int days) {
        log.info("Tool调用: getCalorieTrend userId={} days={}", userId, days);
        if (days <= 0) days = 7;
        if (days > 30) days = 30;

        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.DietRecord>()
                .eq(com.example.entity.DietRecord::getUserId, userId)
                .ge(com.example.entity.DietRecord::getCreateTime, startDate.atStartOfDay());
        var records = dietRecordMapper.selectList(wrapper);

        java.util.Map<LocalDate, Integer> dailyMap = new java.util.TreeMap<>();
        for (LocalDate d = startDate; !d.isAfter(endDate); d = d.plusDays(1)) {
            dailyMap.put(d, 0);
        }
        for (var r : records) {
            LocalDate d = r.getCreateTime() != null ? r.getCreateTime().toLocalDate() : null;
            if (d != null && dailyMap.containsKey(d)) {
                dailyMap.merge(d, r.getCaloriesConsumed() != null ? r.getCaloriesConsumed() : 0, Integer::sum);
            }
        }

        StringBuilder sb = new StringBuilder("近").append(days).append("天热量趋势：\n");
        for (var entry : dailyMap.entrySet()) {
            sb.append(String.format("  %s: %d千卡\n", entry.getKey(), entry.getValue()));
        }
        return sb.toString();
    }

    @Tool("查询系统食物库中的食物营养信息")
    public String searchFood(
            @P("食物名称关键词") String keyword) {
        log.info("Tool调用: searchFood keyword={}", keyword);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.FoodItem>()
                .like(com.example.entity.FoodItem::getName, keyword)
                .eq(com.example.entity.FoodItem::getStatus, 1)
                .last("LIMIT 10");
        var foods = foodItemMapper.selectList(wrapper);

        if (foods.isEmpty()) {
            return "未找到与「" + keyword + "」匹配的食物";
        }

        StringBuilder sb = new StringBuilder("搜索结果：\n");
        for (var f : foods) {
            sb.append(String.format("  - %s | 热量:%d千卡/100g | 蛋白质:%dg | 碳水:%dg | 脂肪:%dg",
                    f.getName(),
                    f.getCaloriePer100g() != null ? f.getCaloriePer100g() : 0,
                    f.getProteinPer100g() != null ? f.getProteinPer100g() : 0,
                    f.getCarbsPer100g() != null ? f.getCarbsPer100g() : 0,
                    f.getFatPer100g() != null ? f.getFatPer100g() : 0));
            if (f.getCategory() != null) sb.append(" | 分类:").append(f.getCategory());
            sb.append("\n");
        }
        return sb.toString();
    }

    @Tool("为用户推荐符合目标的食谱建议")
    public String recommendMealPlan(
            @P("用户ID") Long userId,
            @P("健康目标：减重/增肌/保持/减脂") String goal,
            @P("热量预算（千卡，可选）") Integer calorieBudget) {
        log.info("Tool调用: recommendMealPlan userId={} goal={} budget={}", userId, goal, calorieBudget);
        int budget = (calorieBudget != null && calorieBudget > 0) ? calorieBudget : 1800;

        return String.format(
                "推荐%s食谱（日预算%d千卡）：\n" +
                "早餐（~%.0f千卡）：燕麦粥+鸡蛋+牛奶\n" +
                "午餐（~%.0f千卡）：糙米饭+鸡胸肉+时蔬\n" +
                "加餐（~%.0f千卡）：酸奶+坚果\n" +
                "晚餐（~%.0f千卡）：蒸鱼+豆腐+蔬菜沙拉",
                goal, budget,
                budget * 0.25, budget * 0.35, budget * 0.15, budget * 0.25);
    }

    @Tool("根据身高、体重、年龄、性别计算基础代谢率(BMR)")
    public String calculateBmr(
            @P("身高(cm)") Integer height,
            @P("体重(kg)") Integer weight,
            @P("年龄(岁)") Integer age,
            @P("性别：male/female") String gender) {
        log.info("Tool调用: calculateBmr h={} w={} age={} gender={}", height, weight, age, gender);
        // Mifflin-St Jeor 公式
        int bmr;
        if ("male".equalsIgnoreCase(gender) || "男".equalsIgnoreCase(gender)) {
            bmr = (int) Math.round(10 * weight + 6.25 * height - 5 * age + 5);
        } else {
            bmr = (int) Math.round(10 * weight + 6.25 * height - 5 * age - 161);
        }
        return String.format("基础代谢率(BMR): %d 千卡/天\n" +
                "计算公式: Mifflin-St Jeor 方程", bmr);
    }

    @Tool("根据目标和总热量计算每日宏量营养素（蛋白质/碳水/脂肪）推荐摄入量")
    public String calculateMacroRatios(
            @P("总热量（千卡）") Integer totalCalories,
            @P("健康目标：lose_weight/gain_muscle/maintain/low_carb 中文也支持") String goal) {
        log.info("Tool调用: calculateMacroRatios calories={} goal={}", totalCalories, goal);

        double proteinPct, carbsPct, fatPct;

        String lowerGoal = goal.toLowerCase();
        if (lowerGoal.contains("增肌") || lowerGoal.contains("gain")) {
            // 增肌：高蛋白中等碳水低脂肪
            proteinPct = 0.30;
            carbsPct = 0.45;
            fatPct = 0.25;
        } else if (lowerGoal.contains("减重") || lowerGoal.contains("lose") || lowerGoal.contains("减脂")) {
            // 减重：较高蛋白中等脂肪低碳水
            proteinPct = 0.35;
            carbsPct = 0.25;
            fatPct = 0.40;
        } else if (lowerGoal.contains("低碳") || lowerGoal.contains("low")) {
            // 生酮/低碳：高脂肪高蛋白极低碳水
            proteinPct = 0.30;
            carbsPct = 0.05;
            fatPct = 0.65;
        } else {
            // 保持：均衡比例
            proteinPct = 0.25;
            carbsPct = 0.45;
            fatPct = 0.30;
        }

        int proteinGrams = (int) Math.round(totalCalories * proteinPct / 4);
        int carbsGrams = (int) Math.round(totalCalories * carbsPct / 4);
        int fatGrams = (int) Math.round(totalCalories * fatPct / 9);

        return String.format("每日宏量营养素推荐（总热量%d千卡，目标：%s）：\n" +
                "  蛋白质: %d 克 (%.0f%% 热量，每公斤体重约%.1fg)\n" +
                "  碳水化合物: %d 克 (%.0f%% 热量)\n" +
                "  脂肪: %d 克 (%.0f%% 热量)",
                totalCalories, goal,
                proteinGrams, proteinPct * 100, (double) proteinGrams / (totalCalories / 200),
                carbsGrams, carbsPct * 100,
                fatGrams, fatPct * 100);
    }

    @Tool("根据食物名称和摄入重量(克)计算总热量和营养素")
    public String calculateFoodNutrition(
            @P("食物名称关键词") String foodName,
            @P("摄入重量（克）") Integer grams) {
        log.info("Tool调用: calculateFoodNutrition food={} grams={}", foodName, grams);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.FoodItem>()
                .like(com.example.entity.FoodItem::getName, foodName)
                .eq(com.example.entity.FoodItem::getStatus, 1)
                .last("LIMIT 1");
        var food = foodItemMapper.selectOne(wrapper);
        if (food == null) {
            return "未找到食物: " + foodName;
        }

        double factor = grams / 100.0;
        int calories = (int) Math.round((food.getCaloriePer100g() != null ? food.getCaloriePer100g() : 0) * factor);
        BigDecimal protein = (food.getProteinPer100g() != null ? food.getProteinPer100g() : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(factor)).setScale(1, RoundingMode.HALF_UP);
        BigDecimal carbs = (food.getCarbsPer100g() != null ? food.getCarbsPer100g() : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(factor)).setScale(1, RoundingMode.HALF_UP);
        BigDecimal fat = (food.getFatPer100g() != null ? food.getFatPer100g() : BigDecimal.ZERO)
                .multiply(BigDecimal.valueOf(factor)).setScale(1, RoundingMode.HALF_UP);

        return String.format("食物营养计算结果：\n" +
                "  食物: %s\n" +
                "  摄入量: %d 克\n" +
                "  总热量: %d 千卡\n" +
                "  蛋白质: %s 克\n" +
                "  碳水化合物: %s 克\n" +
                "  脂肪: %s 克",
                food.getName(), grams, calories, protein, carbs, fat);
    }

    @Tool("分析今日摄入与用户目标的营养素缺口")
    public String analyzeNutrientGap(@P("用户ID") Long userId) {
        log.info("Tool调用: analyzeNutrientGap userId={}", userId);

        // 查询用户最新健康档案
        var healthWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.HealthRecord>()
                .eq(com.example.entity.HealthRecord::getUserId, userId)
                .eq(com.example.entity.HealthRecord::getIsLatest, 1);
        var health = healthRecordMapper.selectOne(healthWrapper);
        if (health == null) {
            return "用户暂无健康档案数据";
        }

        // 查询今日已摄入
        LocalDate today = LocalDate.now();
        var dietWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.DietRecord>()
                .eq(com.example.entity.DietRecord::getUserId, userId)
                .ge(com.example.entity.DietRecord::getCreateTime, today.atStartOfDay());
        var records = dietRecordMapper.selectList(dietWrapper);

        int totalCal = 0;
        int totalProtein = 0;
        int totalCarbs = 0;
        int totalFat = 0;

        for (var r : records) {
            if (r.getCaloriesConsumed() != null) totalCal += r.getCaloriesConsumed();
            // DietRecord 不存储宏量营养素，所以只能基于热量估算
        }

        int targetCal = health.getDailyCalorie() != null ? health.getDailyCalorie() : 1800;
        int calGap = targetCal - totalCal;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("今日营养素摄入分析：\n"));
        sb.append(String.format("  目标总热量: %d 千卡\n", targetCal));
        sb.append(String.format("  已摄入热量: %d 千卡\n", totalCal));
        sb.append(String.format("  热量缺口: %d 千卡\n", calGap));

        if (health.getBmr() != null) {
            sb.append(String.format("  基础代谢: %d 千卡\n", health.getBmr()));
        }

        if (calGap > 100) {
            sb.append(String.format("\n建议：还可以摄入约 %d 千卡食物。", calGap));
        } else if (calGap < -100) {
            sb.append(String.format("\n提示：今日已超额约 %d 千卡，建议减少后续摄入量。", -calGap));
        } else {
            sb.append("\n提示：今日热量摄入接近目标，保持得很好！");
        }

        return sb.toString();
    }
}