package com.example.agent.tool;

import com.example.mapper.ExerciseItemMapper;
import com.example.mapper.ExerciseRecordMapper;
import com.example.mapper.HealthRecordMapper;
import com.example.mapper.UserProfileMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 运动科学工具集。
 * 提供运动强度计算、热量消耗估算、心率区间、体能评估等能力。
 */
@Slf4j
@Component
public class ExerciseTools {

    private final ExerciseItemMapper exerciseItemMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final UserProfileMapper userProfileMapper;

    public ExerciseTools(ExerciseItemMapper exerciseItemMapper,
                         ExerciseRecordMapper exerciseRecordMapper,
                         HealthRecordMapper healthRecordMapper,
                         UserProfileMapper userProfileMapper) {
        this.exerciseItemMapper = exerciseItemMapper;
        this.exerciseRecordMapper = exerciseRecordMapper;
        this.healthRecordMapper = healthRecordMapper;
        this.userProfileMapper = userProfileMapper;
    }

    @Tool("根据运动类型、时长和用户体重计算消耗热量（MET法）")
    public String calculateExerciseCalories(
            @P("用户ID") Long userId,
            @P("运动项目名称（如'跑步'、'游泳'）") String exerciseName,
            @P("运动时长（分钟）") Integer durationMinutes) {
        log.info("Tool调用: calculateExerciseCalories userId={} exercise={} min={}",
                userId, exerciseName, durationMinutes);

        // 查询用户体重
        var healthWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.HealthRecord>()
                .eq(com.example.entity.HealthRecord::getUserId, userId)
                .eq(com.example.entity.HealthRecord::getIsLatest, 1);
        var health = healthRecordMapper.selectOne(healthWrapper);
        if (health == null || health.getWeight() == null) {
            return "无法获取用户体重数据";
        }
        double weightKg = health.getWeight();

        // 查询运动项目获取热量系数
        var itemWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.ExerciseItem>()
                .like(com.example.entity.ExerciseItem::getName, exerciseName)
                .eq(com.example.entity.ExerciseItem::getStatus, 1)
                .last("LIMIT 1");
        var item = exerciseItemMapper.selectOne(itemWrapper);

        if (item == null) {
            // 使用通用 MET 估算
            int met = estimateMet(exerciseName);
            double calories = met * weightKg * durationMinutes / 60.0;
            return String.format("运动热量消耗估算：\n" +
                    "  运动项目: %s（估算MET=%.1f）\n" +
                    "  体重: %.1f kg\n" +
                    "  时长: %d 分钟\n" +
                    "  消耗热量: %.0f 千卡\n" +
                    "  公式: MET × 体重(kg) × 时间(h)",
                    exerciseName, (double) met, weightKg, durationMinutes, calories);
        }

        // 使用 exercise_item 的 calorieCoefficient
        double coeff = item.getCalorieCoefficient() != null
                ? item.getCalorieCoefficient().doubleValue() : 0;
        double calories;
        if (coeff > 0) {
            calories = coeff * weightKg * durationMinutes;
        } else {
            // 回退到 MET 估算
            int met = estimateMet(item.getType() != null ? item.getType() : exerciseName);
            calories = met * weightKg * durationMinutes / 60.0;
        }

        String intensityLabel = getIntensityLabel(item.getDifficulty());
        return String.format("运动热量消耗计算：\n" +
                "  运动项目: %s (%s)\n" +
                "  强度等级: %s\n" +
                "  体重: %.1f kg\n" +
                "  时长: %d 分钟\n" +
                "  消耗热量: %.0f 千卡",
                item.getName(), item.getType() != null ? item.getType() : "",
                intensityLabel, weightKg, durationMinutes, calories);
    }

    @Tool("根据年龄计算靶心率区间（热身/燃脂/有氧/无氧/极限）")
    public String calculateHeartRateZones(
            @P("年龄（岁）") Integer age,
            @P("静息心率（可选，默认70）") Integer restingHr) {
        log.info("Tool调用: calculateHeartRateZones age={} restingHr={}", age, restingHr);

        int rhr = (restingHr != null && restingHr > 0) ? restingHr : 70;
        int maxHr = 220 - age;  // 最大心率估计
        int reserveHr = maxHr - rhr;  // 储备心率

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("心率区间计算（年龄%d岁，静息心率%d bpm，最大心率%d bpm）：\n\n",
                age, rhr, maxHr));

        // 5个心率区间 (Karvonen 公式: RHR + % × HRR)
        sb.append(String.format("  【热身区】 %d - %d bpm | 储备心率%%: 50-60%%\n" +
                "    用途：运动前热身、恢复日低强度活动\n\n",
                rhr + (int)(reserveHr * 0.50), rhr + (int)(reserveHr * 0.60)));

        sb.append(String.format("  【燃脂区】 %d - %d bpm | 储备心率%%: 60-70%%\n" +
                "    用途：燃脂减重、基础有氧耐力\n\n",
                rhr + (int)(reserveHr * 0.60), rhr + (int)(reserveHr * 0.70)));

        sb.append(String.format("  【有氧区】 %d - %d bpm | 储备心率%%: 70-80%%\n" +
                "    用途：增强心肺功能、提升耐力\n\n",
                rhr + (int)(reserveHr * 0.70), rhr + (int)(reserveHr * 0.80)));

        sb.append(String.format("  【无氧区】 %d - %d bpm | 储备心率%%: 80-90%%\n" +
                "    用途：提升乳酸阈值、高强度间歇训练\n\n",
                rhr + (int)(reserveHr * 0.80), rhr + (int)(reserveHr * 0.90)));

        sb.append(String.format("  【极限区】 %d - %d bpm | 储备心率%%: 90-100%%\n" +
                "    用途：冲刺训练、竞技比赛（需谨慎，不建议初学者尝试）",
                rhr + (int)(reserveHr * 0.90), maxHr));

        return sb.toString();
    }

    @Tool("根据用户体能水平和健康目标推荐适合的运动强度")
    public String recommendExerciseIntensity(
            @P("用户ID") Long userId,
            @P("运动目标：减脂/增肌/耐力/康复") String target) {
        log.info("Tool调用: recommendExerciseIntensity userId={} target={}", userId, target);

        // 查询用户档案
        var profileWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.UserProfile>()
                .eq(com.example.entity.UserProfile::getUserId, userId);
        var profile = userProfileMapper.selectOne(profileWrapper);

        var healthWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.HealthRecord>()
                .eq(com.example.entity.HealthRecord::getUserId, userId)
                .eq(com.example.entity.HealthRecord::getIsLatest, 1);
        var health = healthRecordMapper.selectOne(healthWrapper);

        String fitnessLevel = profile != null && profile.getFitnessLevel() != null
                ? profile.getFitnessLevel() : "OCCASIONAL";
        int dailyMin = profile != null && profile.getDailyAvailableMin() != null
                ? profile.getDailyAvailableMin() : 30;
        boolean hasInjury = profile != null && profile.getInjuries() != null
                && !profile.getInjuries().isBlank();
        boolean hasDisease = health != null && health.getDiseaseHistory() != null
                && !health.getDiseaseHistory().isBlank();

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("运动强度推荐（体能水平：%s，每日可用时间：%d分钟）：\n\n",
                getFitnessLevelLabel(fitnessLevel), dailyMin));

        if (hasInjury || hasDisease) {
            sb.append("⚠ 用户有运动损伤或慢性疾病史，建议优先咨询医生，运动从低强度开始。\n\n");
        }

        String targetLower = target != null ? target.toLowerCase() : "";

        switch (fitnessLevel.toUpperCase()) {
            case "SEDENTARY" -> {
                sb.append("推荐从低强度起步（心率：最大心率的50-60%）：\n");
                sb.append("  - 快走/散步 每天20-30分钟\n");
                sb.append("  - 基础拉伸/瑜伽 每周3次\n");
                sb.append("  - 循序渐进，每2周增加5分钟时长\n");
            }
            case "OCCASIONAL" -> {
                sb.append("推荐中低强度为主（心率：最大心率的60-70%）：\n");
                if (targetLower.contains("减脂")) {
                    sb.append("  - 慢跑/骑行 30-45分钟 × 每周4次\n");
                    sb.append("  - HIIT入门 每周1-2次\n");
                } else if (targetLower.contains("增肌")) {
                    sb.append("  - 自重力量训练 × 每周3次\n");
                    sb.append("  - 轻重量哑铃 每组12-15次\n");
                } else {
                    sb.append("  - 有氧+力量混合训练 × 每周3-4次\n");
                }
            }
            case "REGULAR" -> {
                sb.append("推荐中等强度为主体（心率：最大心率的70-80%）：\n");
                if (targetLower.contains("减脂")) {
                    sb.append("  - 跑步/游泳 45-60分钟 × 每周4-5次\n");
                    sb.append("  - HIIT高强度间歇 × 每周2次\n");
                    sb.append("  - 力量训练 × 每周3次\n");
                } else if (targetLower.contains("增肌")) {
                    sb.append("  - 重量训练 每组8-12次至力竭 × 每周4-5次\n");
                    sb.append("  - 补充低强度有氧 每周2次\n");
                } else {
                    sb.append("  - 每周5天训练：3天力量 + 2天有氧\n");
                }
            }
            case "ADVANCED" -> {
                sb.append("推荐中高强度训练（心率：最大心率的75-90%）：\n");
                if (targetLower.contains("减脂")) {
                    sb.append("  - HIIT+力量组合 × 每周5-6次\n");
                    sb.append("  - 长跑/骑行/游泳 60分钟+ × 每周2-3次\n");
                } else if (targetLower.contains("增肌")) {
                    sb.append("  - 分化训练 每组6-10次 × 每周5-6次\n");
                    sb.append("  - 超级组/递减组等进阶技巧\n");
                } else {
                    sb.append("  - 周期化训练：力量期+耐力期交替\n");
                }
            }
        }

        sb.append(String.format("\n建议每周总运动时间：%d - %d 分钟", dailyMin * 4, dailyMin * 6));
        return sb.toString();
    }

    @Tool("基于用户历史运动数据评估当前体能水平")
    public String assessFitnessLevel(@P("用户ID") Long userId) {
        log.info("Tool调用: assessFitnessLevel userId={}", userId);

        // 查询近2个月的运动记录
        LocalDate twoMonthsAgo = LocalDate.now().minusDays(60);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.ExerciseRecord>()
                .eq(com.example.entity.ExerciseRecord::getUserId, userId)
                .ge(com.example.entity.ExerciseRecord::getCreateTime, twoMonthsAgo.atStartOfDay());
        var records = exerciseRecordMapper.selectList(wrapper);

        if (records.isEmpty()) {
            var profileWrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.UserProfile>()
                    .eq(com.example.entity.UserProfile::getUserId, userId);
            var profile = userProfileMapper.selectOne(profileWrapper);
            String declaredLevel = profile != null && profile.getFitnessLevel() != null
                    ? profile.getFitnessLevel() : "未设置";
            return String.format("近60天无运动记录。用户自评体能水平：%s。建议从基础运动开始建立习惯。",
                    getFitnessLevelLabel(declaredLevel));
        }

        // 统计指标
        int totalSessions = records.size();
        long uniqueDates = records.stream()
                .map(r -> r.getCreateTime() != null ? r.getCreateTime().toLocalDate() : null)
                .filter(d -> d != null)
                .distinct().count();
        int totalMinutes = records.stream()
                .mapToInt(r -> r.getDurationMinutes() != null ? r.getDurationMinutes() : 0)
                .sum();
        int totalCalories = records.stream()
                .mapToInt(r -> r.getCaloriesBurned() != null ? r.getCaloriesBurned() : 0)
                .sum();

        // 统计运动类型分布
        Map<String, Long> typeCount = new LinkedHashMap<>();
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

        double sessionsPerWeek = (double) totalSessions / 8.57;  // 60天约8.57周
        double avgDuration = totalMinutes > 0 ? (double) totalMinutes / totalSessions : 0;

        // 评估体能等级
        String assessedLevel;
        String recommendation;
        if (sessionsPerWeek >= 4 && avgDuration >= 45) {
            assessedLevel = "ADVANCED（高级）";
            recommendation = "运动频率和强度充足，可尝试周期性训练计划，挑战更高目标。";
        } else if (sessionsPerWeek >= 3 && avgDuration >= 30) {
            assessedLevel = "REGULAR（规律）";
            recommendation = "运动习惯良好，建议逐步增加强度或尝试新运动类型。";
        } else if (sessionsPerWeek >= 1.5) {
            assessedLevel = "OCCASIONAL（偶有运动）";
            recommendation = "有一定运动基础，建议将频率提升到每周3次以上。";
        } else {
            assessedLevel = "SEDENTARY（久坐少动）";
            recommendation = "运动频率偏低，建议从每周2次轻量运动开始建立习惯。";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("近60天体适能评估：\n\n"));
        sb.append(String.format("  评估等级: %s\n", assessedLevel));
        sb.append(String.format("  总运动次数: %d 次（约%.1f次/周）\n", totalSessions, sessionsPerWeek));
        sb.append(String.format("  运动总时长: %d 分钟（平均%.0f分钟/次）\n", totalMinutes, avgDuration));
        sb.append(String.format("  运动总消耗: %d 千卡\n", totalCalories));
        sb.append(String.format("  运动天数: %d 天\n\n", uniqueDates));

        sb.append("运动类型分布：\n");
        typeCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(e -> sb.append(String.format("  - %s: %d次\n", e.getKey(), e.getValue())));

        sb.append("\n").append(recommendation);
        return sb.toString();
    }

    // ---- 辅助方法 ----

    /**
     * 根据运动名称估算 MET 值。
     */
    private int estimateMet(String exerciseName) {
        if (exerciseName == null) return 5;
        String lower = exerciseName.toLowerCase();
        if (lower.contains("跑步") || lower.contains("run")) return 8;
        if (lower.contains("游泳") || lower.contains("swim")) return 7;
        if (lower.contains("骑行") || lower.contains("bike") || lower.contains("cycle")) return 6;
        if (lower.contains("跳绳") || lower.contains("jump")) return 10;
        if (lower.contains("深蹲") || lower.contains("squat") || lower.contains("力量") || lower.contains("strength")) return 5;
        if (lower.contains("瑜伽") || lower.contains("yoga") || lower.contains("拉伸") || lower.contains("stretch")) return 3;
        if (lower.contains("走路") || lower.contains("步") || lower.contains("walk")) return 3;
        if (lower.contains("hiit")) return 10;
        if (lower.contains("俯卧撑") || lower.contains("push")) return 5;
        if (lower.contains("平板") || lower.contains("plank")) return 3;
        if (lower.contains("引体") || lower.contains("pull")) return 6;
        if (lower.contains("仰卧起坐") || lower.contains("腹肌")) return 4;
        return 5; // 默认中等强度
    }

    private String getIntensityLabel(String difficulty) {
        if (difficulty == null) return "中等";
        return switch (difficulty.toUpperCase()) {
            case "EASY" -> "低强度";
            case "MEDIUM" -> "中等强度";
            case "HARD" -> "高强度";
            default -> "中等";
        };
    }

    private String getFitnessLevelLabel(String level) {
        if (level == null) return "未知";
        return switch (level.toUpperCase()) {
            case "SEDENTARY" -> "久坐少动";
            case "OCCASIONAL" -> "偶有运动";
            case "REGULAR" -> "规律运动";
            case "ADVANCED" -> "运动达人";
            default -> level;
        };
    }
}