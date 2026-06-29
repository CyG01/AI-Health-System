package com.example.resilience;

import com.example.entity.ExerciseRule;
import com.example.mapper.ExerciseRuleMapper;
import com.example.sdui.AiAgentResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * 熔断降级服务。
 * 当安全熔断器 / AI 服务全部不可用时，基于 exercise_rules 规则引擎返回安全降级响应。
 */
@Slf4j
@Service
public class FallbackService {

    private final ExerciseRuleMapper exerciseRuleMapper;

    public FallbackService(ExerciseRuleMapper exerciseRuleMapper) {
        this.exerciseRuleMapper = exerciseRuleMapper;
    }

    /**
     * 获取降级响应（规则引擎安全兜底）。
     * 优先级：exercise_rules 表匹配个性化建议 → 静态安全文案。
     * 降级时明确告知用户当前 AI 不可用。
     */
    public AiAgentResponse getFallbackResponse(Long userId, String userInput) {
        return AiAgentResponse.builder()
                .text(buildSafeStaticResponse(userInput))
                .disclaimer("【重要提示】当前AI服务暂时不可用，系统已切换至安全基础模式。"
                        + "以下内容由健康规则引擎生成，非AI个性化建议。"
                        + "如需获得完整AI健康评估，请在服务恢复后重试。"
                        + "如有紧急健康问题，请立即就医或咨询专业医生。")
                .metadata(Map.of(
                        "planSource", "rule_fallback",
                        "aiAvailable", false,
                        "fallbackReason", "safety_circuit_breaker",
                        "userId", userId
                ))
                .build();
    }

    /**
     * 基于规则引擎生成降级运动计划（JSON 格式，与 AI 输出兼容）。
     * 在 AI 模型全部不可用时，作为 PlanAdjustService 的最后兜底。
     *
     * @param heightCm     身高（cm）
     * @param weightKg     体重（kg）
     * @param goal         健康目标（减重/增肌/保持/康复）
     * @param durationDays 计划天数
     * @return 降级计划 JSON
     */
    public String generateFallbackPlanJson(double heightCm, double weightKg,
                                            String goal, int durationDays) {
        double bmi = weightKg / ((heightCm / 100) * (heightCm / 100));
        BigDecimal bmiDecimal = BigDecimal.valueOf(bmi).setScale(1, RoundingMode.HALF_UP);

        log.info("FallbackService 生成降级计划 goal={} bmi={} days={}", goal, bmiDecimal, durationDays);

        // 从规则表匹配推荐运动
        List<ExerciseRule> rules = exerciseRuleMapper.matchByGoalAndBmi(goal, bmiDecimal.doubleValue());

        StringBuilder json = new StringBuilder("{\"days\":[");

        for (int d = 1; d <= durationDays; d++) {
            if (d > 1) json.append(",");
            json.append("{\"d\":").append(d).append(",\"items\":[");

            if (rules != null && !rules.isEmpty()) {
                ExerciseRule rule = rules.get(d % rules.size());
                json.append("\"").append(rule.getExerciseName())
                    .append(" ").append(rule.getDefaultDuration()).append("分钟（")
                    .append(rule.getDefaultIntensity()).append("强度）\"");

                if (rules.size() > 1) {
                    ExerciseRule rule2 = rules.get((d + 1) % rules.size());
                    if (!rule2.getId().equals(rule.getId())) {
                        json.append(",\"").append(rule2.getExerciseName())
                            .append(" ").append(rule2.getDefaultDuration()).append("分钟（")
                            .append(rule2.getDefaultIntensity()).append("强度）\"");
                    }
                }
            } else {
                int exerciseMin = 30 + (d % 3) * 5;
                json.append("\"快走或慢跑").append(exerciseMin).append("分钟（中等强度）\"");
            }

            int calorieTarget = (int) (1500 + weightKg * 5);
            String mealTip = bmi > 25 ? "控制碳水摄入，增加蔬菜比例" : "均衡饮食，保证蛋白质摄入";
            json.append(",\"").append(mealTip).append("\",")
                .append("\"每日目标热量").append(calorieTarget).append("kcal\"");
            json.append("]}");
        }
        json.append("]}");

        log.info("FallbackService 降级计划生成完成 rulesMatched={}", rules != null ? rules.size() : 0);
        return json.toString();
    }

    private String buildSafeStaticResponse(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return "您好！AI健康助手正在进行安全维护，暂时提供基础建议。\n\n"
                    + "【每日健康基础建议】\n"
                    + "1. 保证7-8小时睡眠\n"
                    + "2. 每天饮水1500-2000ml\n"
                    + "3. 保持30分钟适量运动\n"
                    + "4. 均衡饮食，多吃蔬果\n\n"
                    + "请稍后再试以获取个性化方案。";
        }

        return "您好！由于系统正在进行安全自检，暂时无法提供AI个性化建议。\n\n"
                + "您输入的内容涉及：「" + truncate(userInput, 50) + "」\n\n"
                + "【安全提示】\n"
                + "1. AI健康建议仅供参考，不构成医疗诊断\n"
                + "2. 如有身体不适，请及时就医\n"
                + "3. 系统完成安全检查后将恢复个性化服务，请稍后再试";
    }

    private String truncate(String s, int maxLen) {
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}