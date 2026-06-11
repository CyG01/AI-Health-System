package com.example.agent.tool;

import com.example.mapper.DailyCheckinMapper;
import com.example.mapper.EmotionRecordMapper;
import com.example.mapper.ExerciseItemMapper;
import com.example.mapper.ExerciseRecordMapper;
import com.example.mapper.SleepRecordMapper;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 心理健康 Agent 的工具集。
 * 提供情绪追踪、睡眠分析、行为模式查询等能力。
 */
@Slf4j
@Component
public class PsychologyTools {

    private final DailyCheckinMapper dailyCheckinMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final SleepRecordMapper sleepRecordMapper;
    private final EmotionRecordMapper emotionRecordMapper;
    private final ExerciseItemMapper exerciseItemMapper;

    public PsychologyTools(DailyCheckinMapper dailyCheckinMapper,
                            ExerciseRecordMapper exerciseRecordMapper,
                            SleepRecordMapper sleepRecordMapper,
                            EmotionRecordMapper emotionRecordMapper,
                            ExerciseItemMapper exerciseItemMapper) {
        this.dailyCheckinMapper = dailyCheckinMapper;
        this.exerciseRecordMapper = exerciseRecordMapper;
        this.sleepRecordMapper = sleepRecordMapper;
        this.emotionRecordMapper = emotionRecordMapper;
        this.exerciseItemMapper = exerciseItemMapper;
    }

    @Tool("查询用户近N天的打卡完成情况（用于评估行为坚持度）")
    public String getCheckinConsistency(
            @P("用户ID") Long userId,
            @P("天数（默认14天）") int days) {
        log.info("Tool调用: getCheckinConsistency userId={} days={}", userId, days);
        if (days <= 0) days = 14;
        if (days > 90) days = 90;

        LocalDate startDate = LocalDate.now().minusDays(days - 1);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.DailyCheckin>()
                .eq(com.example.entity.DailyCheckin::getUserId, userId)
                .ge(com.example.entity.DailyCheckin::getCheckDate, startDate);
        var checkins = dailyCheckinMapper.selectList(wrapper);

        long completed = checkins.stream()
                .filter(c -> c.getExerciseStatus() != null && c.getExerciseStatus() >= 1)
                .count();

        // 计算连续打卡天数
        int streak = 0;
        LocalDate today = LocalDate.now();
        for (int i = 0; i < days; i++) {
            LocalDate d = today.minusDays(i);
            boolean found = checkins.stream()
                    .anyMatch(c -> c.getCheckDate() != null && c.getCheckDate().equals(d)
                            && c.getExerciseStatus() != null && c.getExerciseStatus() >= 1);
            if (found) streak++;
            else break;
        }

        double rate = checkins.isEmpty() ? 0 : (completed * 100.0 / days);
        return String.format("近%d天打卡完成率: %.0f%%（%d/%d天）\n当前连续打卡: %d天",
                days, rate, completed, days, streak);
    }

    @Tool("查询用户近N天的睡眠情况")
    public String getSleepPattern(
            @P("用户ID") Long userId,
            @P("天数（默认7天）") int days) {
        log.info("Tool调用: getSleepPattern userId={} days={}", userId, days);
        if (days <= 0) days = 7;
        if (days > 30) days = 30;

        LocalDate startDate = LocalDate.now().minusDays(days - 1);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.SleepRecord>()
                .eq(com.example.entity.SleepRecord::getUserId, userId)
                .ge(com.example.entity.SleepRecord::getRecordDate, startDate);
        var records = sleepRecordMapper.selectList(wrapper);

        if (records.isEmpty()) {
            return "暂无睡眠记录";
        }

        double avgHours = records.stream()
                .mapToDouble(r -> r.getSleepHours() != null ? r.getSleepHours().doubleValue() : 0)
                .average().orElse(0);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("近%d天睡眠：平均 %.1f 小时\n", days, avgHours));
        for (var r : records) {
            sb.append(String.format("  %s: %.1f小时 | 深睡%d分钟\n",
                    r.getRecordDate(),
                    r.getSleepHours() != null ? r.getSleepHours() : 0,
                    r.getDeepSleepMinutes() != null ? r.getDeepSleepMinutes() : 0));
        }
        return sb.toString();
    }

    @Tool("获取行为激励建议（基于近期完成情况生成个性化鼓励）")
    public String getMotivationAdvice(
            @P("用户ID") Long userId,
            @P("最近的情绪状态，如'沮丧'、'疲惫'、'积极'") String mood) {
        log.info("Tool调用: getMotivationAdvice userId={} mood={}", userId, mood);
        return String.format(
                "用户情绪状态: %s。请根据此情绪状态，用温暖鼓励的语气，给出3条简短的行为激励建议，" +
                "帮助用户克服当前困难，重拾健康计划的信心。", mood);
    }

    @Tool("查询用户近期的运动类型偏好和行为模式")
    public String getBehaviorPattern(
            @P("用户ID") Long userId,
            @P("查询天数（默认30天）") int days) {
        log.info("Tool调用: getBehaviorPattern userId={} days={}", userId, days);
        if (days <= 0) days = 30;

        LocalDate startDate = LocalDate.now().minusDays(days - 1);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.ExerciseRecord>()
                .eq(com.example.entity.ExerciseRecord::getUserId, userId)
                .ge(com.example.entity.ExerciseRecord::getCreateTime, startDate.atStartOfDay());
        var records = exerciseRecordMapper.selectList(wrapper);

        if (records.isEmpty()) {
            return "近" + days + "天暂无运动记录";
        }

        // 统计运动类型偏好（通过 itemId 关联 ExerciseItem 获取类型名）
        java.util.Map<String, Long> typeCount = new java.util.LinkedHashMap<>();
        for (var r : records) {
            String typeName = "其他";
            if (r.getItemId() != null) {
                var item = exerciseItemMapper.selectById(r.getItemId());
                if (item != null && item.getType() != null) {
                    typeName = item.getType();
                }
            }
            typeCount.merge(typeName, 1L, Long::sum);
        }

        // 统计每周运动频率
        long weeks = Math.max(1, days / 7);
        double avgPerWeek = (double) records.size() / weeks;

        StringBuilder sb = new StringBuilder();
        sb.append(String.format("近%d天运动行为模式：\n", days));
        sb.append(String.format("  总记录: %d次 | 周均: %.1f次\n", records.size(), avgPerWeek));
        sb.append("  运动类型偏好：\n");
        typeCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(e -> sb.append(String.format("    - %s: %d次\n", e.getKey(), e.getValue())));

        return sb.toString();
    }

    @Tool("查询用户近N天的情绪记录历史")
    public String getEmotionHistory(
            @P("用户ID") Long userId,
            @P("天数（默认14天）") int days) {
        log.info("Tool调用: getEmotionHistory userId={} days={}", userId, days);
        if (days <= 0) days = 14;
        if (days > 90) days = 90;

        LocalDate startDate = LocalDate.now().minusDays(days - 1);
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.EmotionRecord>()
                .eq(com.example.entity.EmotionRecord::getUserId, userId)
                .ge(com.example.entity.EmotionRecord::getCreatedAt, startDate.atStartOfDay())
                .orderByDesc(com.example.entity.EmotionRecord::getCreatedAt);
        var records = emotionRecordMapper.selectList(wrapper);

        if (records.isEmpty()) {
            return "近" + days + "天暂无情绪记录";
        }

        // 统计情绪分布
        java.util.Map<String, Long> emotionCounts = new java.util.HashMap<>();
        for (var r : records) {
            String type = r.getEmotionType() != null ? r.getEmotionType() : "NEUTRAL";
            emotionCounts.merge(type, 1L, Long::sum);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("近").append(days).append("天情绪记录分析：\n");
        sb.append("情绪分布：\n");
        emotionCounts.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .forEach(e -> sb.append(String.format("  %s: %d次\n", getEmotionLabel(e.getKey()), e.getValue())));

        // 最近5条记录
        sb.append("\n最近记录：\n");
        int showCount = Math.min(5, records.size());
        for (int i = 0; i < showCount; i++) {
            var r = records.get(i);
            String date = r.getCreatedAt() != null
                    ? r.getCreatedAt().toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    : "未知日期";
            String text = r.getOriginalText() != null && r.getOriginalText().length() > 50
                    ? r.getOriginalText().substring(0, 50) + "..."
                    : (r.getOriginalText() != null ? r.getOriginalText() : "");
            sb.append(String.format("  %s [%s] %s\n", date,
                    getEmotionLabel(r.getEmotionType()), text));
        }

        return sb.toString();
    }

    @Tool("查询用户近N天负面情绪天数统计")
    public String getNegativeEmotionDays(
            @P("用户ID") Long userId,
            @P("天数（默认14天）") int days) {
        log.info("Tool调用: getNegativeEmotionDays userId={} days={}", userId, days);
        if (days <= 0) days = 14;

        int negativeDays = emotionRecordMapper.countNegativeDays(userId, days);
        return String.format("近%d天中出现负面情绪（疲惫/沮丧/焦虑/痛苦）共 %d 天", days, negativeDays);
    }

    private String getEmotionLabel(String type) {
        if (type == null) return "中性";
        return switch (type.toUpperCase()) {
            case "TIRED" -> "疲惫";
            case "FRUSTRATED" -> "沮丧";
            case "EXCITED" -> "兴奋";
            case "ANXIOUS" -> "焦虑";
            case "PAIN" -> "痛苦";
            case "NEUTRAL" -> "中性";
            case "SAD" -> "悲伤";
            default -> type;
        };
    }
}