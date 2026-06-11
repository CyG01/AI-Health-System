package com.example.agent.orchestrator;

import com.example.agent.model.RoutingDecision;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.regex.Pattern;

/**
 * 多 Agent 意图路由器。
 * 基于关键词 + 模式匹配，对用户输入进行意图分类，生成 RoutingDecision。
 * 无需额外的 LLM 调用，保证路由速度且不消耗 Token 预算。
 */
@Slf4j
@Component
public class IntentRouter {

    // ---- 营养/饮食相关关键词 ----
    private static final List<String> NUTRITION_KEYWORDS = List.of(
            "吃", "食物", "饮食", "营养", "热量", "卡路里", "蛋白", "碳水", "脂肪",
            "食谱", "餐饮", "蔬菜", "水果", "肉类", "主食", "零食", "饮料", "奶",
            "鸡蛋", "鱼", "鸡胸", "牛肉", "减肥餐", "增肌餐", "外卖", "食堂",
            "早餐", "午餐", "晚餐", "加餐", "夜宵", "火锅", "烧烤",
            "饿了", "吃了", "吃什么", "怎么吃", "不该吃", "能吃吗",
            "维生素", "矿物质", "膳食纤维", "节食", "断食", "代餐",
            "血糖", "血脂", "胆固醇", "尿酸", "痛风", "糖尿病"
    );

    // ---- 运动/健身相关关键词 ----
    private static final List<String> COACH_KEYWORDS = List.of(
            "运动", "锻炼", "训练", "跑步", "游泳", "健身", "瑜伽", "拉伸",
            "力量", "有氧", "无氧", "HIIT", "跳绳", "深蹲", "俯卧撑", "哑铃",
            "引体向上", "平板支撑", "仰卧起坐", "骑行", "走路", "步数",
            "增肌", "减脂", "塑形", "体脂", "肌肉", "腹肌", "马甲线",
            "计划", "打卡", "完成率", "坚持", "多久", "每周几次",
            "膝盖", "腰疼", "受伤", "恢复", "热身", "放松",
            "心率", "最大摄氧量", "体重", "BMI", "体测"
    );

    // ---- 心理/情绪相关关键词 ----
    private static final List<String> PSYCHOLOGY_KEYWORDS = List.of(
            "焦虑", "抑郁", "压力", "失眠", "睡不着", "熬夜", "疲劳", "疲惫",
            "没动力", "不想动", "懒", "拖延", "崩溃", "难过", "伤心", "烦躁",
            "情绪", "心情", "心态", "放弃", "坚持不下去", "太难了",
            "自暴自弃", "暴食", "不想吃了", "没信心", "自卑", "身材焦虑",
            "冥想", "放松", "呼吸", "正念", "深呼吸", "减压", "打坐",
            "睡眠", "睡不好", "早醒", "多梦", "噩梦", "熬夜",
            "想哭", "委屈", "孤独", "没人理解", "支持", "鼓励"
    );

    // ---- 高风险/危机关键词（需要立即转人工或安全审查） ----
    private static final Pattern CRISIS_PATTERN = Pattern.compile(
            "自杀|自伤|自残|不想活|想死|结束生命|伤害自己|割腕|跳楼|安眠药过量|" +
            "暴饮暴食严重|绝食|催吐|厌食|暴食症|厌食症",
            Pattern.CASE_INSENSITIVE
    );

    // ---- 纯闲聊/寒暄关键词 ----
    private static final Set<String> GREETINGS = Set.of(
            "你好", "hi", "hello", "嗨", "在吗", "在不在", "早上好", "下午好", "晚上好",
            "谢谢", "感谢", "再见", "拜拜", "bye", "晚安", "早安", "午安"
    );

    /**
     * 分析用户输入，生成路由决策。
     */
    public RoutingDecision route(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return defaultDecision(userInput, "empty");
        }

        String lower = userInput.toLowerCase().trim();

        // 1. 危机检测（最高优先级）
        if (CRISIS_PATTERN.matcher(lower).find()) {
            return RoutingDecision.builder()
                    .userInput(userInput)
                    .intent("crisis")
                    .emotionLabel("crisis")
                    .targetAgents(new ArrayList<>(List.of("psychology")))
                    .parallel(false)
                    .requireSafetyReview(true)
                    .confidence(1.0)
                    .priorities(List.of(
                            RoutingDecision.AgentPriority.builder().agentName("psychology").priority(1).build()))
                    .build();
        }

        // 2. 计算各领域匹配分数
        int nutritionScore = countMatches(lower, NUTRITION_KEYWORDS);
        int coachScore = countMatches(lower, COACH_KEYWORDS);
        int psychologyScore = countMatches(lower, PSYCHOLOGY_KEYWORDS);

        // 3. 纯闲聊检测
        if (isPureGreeting(userInput)) {
            return RoutingDecision.builder()
                    .userInput(userInput)
                    .intent("chitchat")
                    .emotionLabel("neutral")
                    .targetAgents(new ArrayList<>(List.of("coach")))
                    .parallel(false)
                    .requireSafetyReview(false)
                    .confidence(0.9)
                    .build();
        }

        // 4. 根据分数决定路由
        int maxScore = Math.max(nutritionScore, Math.max(coachScore, psychologyScore));

        // 无匹配：默认健康教练
        if (maxScore == 0) {
            return RoutingDecision.builder()
                    .userInput(userInput)
                    .intent("coach")
                    .emotionLabel(detectEmotion(lower))
                    .targetAgents(new ArrayList<>(List.of("coach")))
                    .parallel(false)
                    .requireSafetyReview(true)
                    .confidence(0.5)
                    .build();
        }

        // 5. 多领域交叉判断
        List<String> targets = new ArrayList<>();
        List<RoutingDecision.AgentPriority> priorities = new ArrayList<>();

        // 心理领域得分高时优先
        if (psychologyScore >= maxScore * 0.7) {
            targets.add("psychology");
            priorities.add(RoutingDecision.AgentPriority.builder()
                    .agentName("psychology").priority(1).build());
        }
        if (coachScore >= maxScore * 0.7) {
            targets.add("coach");
            priorities.add(RoutingDecision.AgentPriority.builder()
                    .agentName("coach").priority(targets.size() + 1).build());
        }
        if (nutritionScore >= maxScore * 0.7) {
            targets.add("nutrition");
            priorities.add(RoutingDecision.AgentPriority.builder()
                    .agentName("nutrition").priority(targets.size() + 1).build());
        }

        // 至少保留最高分领域
        if (targets.isEmpty()) {
            if (coachScore >= nutritionScore && coachScore >= psychologyScore) {
                targets.add("coach");
            } else if (nutritionScore >= coachScore && nutritionScore >= psychologyScore) {
                targets.add("nutrition");
            } else {
                targets.add("psychology");
            }
        }

        boolean isParallel = targets.size() > 1;
        String intent = isParallel ? "mixed" : targets.get(0);
        double confidence = Math.min(0.95, 0.5 + maxScore * 0.15);

        return RoutingDecision.builder()
                .userInput(userInput)
                .intent(intent)
                .emotionLabel(detectEmotion(lower))
                .targetAgents(targets)
                .parallel(isParallel)
                .requireSafetyReview(true)
                .confidence(confidence)
                .priorities(priorities)
                .build();
    }

    private int countMatches(String text, List<String> keywords) {
        int count = 0;
        for (String kw : keywords) {
            if (text.contains(kw)) {
                count++;
            }
        }
        return count;
    }

    private String detectEmotion(String text) {
        if (text.contains("焦虑") || text.contains("压力") || text.contains("紧张") || text.contains("担心")) {
            return "anxious";
        }
        if (text.contains("难过") || text.contains("伤心") || text.contains("崩溃") || text.contains("想哭")) {
            return "sad";
        }
        if (text.contains("烦躁") || text.contains("生气") || text.contains("恼怒") || text.contains("崩溃")) {
            return "frustrated";
        }
        if (text.contains("开心") || text.contains("兴奋") || text.contains("成功") || text.contains("好消息")) {
            return "excited";
        }
        if (text.contains("累") || text.contains("疲劳") || text.contains("疲惫") || text.contains("没精神")) {
            return "tired";
        }
        return "neutral";
    }

    private boolean isPureGreeting(String input) {
        String cleaned = input.replaceAll("[!,，。.？?！\\s]", "").toLowerCase();
        for (String greeting : GREETINGS) {
            if (cleaned.equals(greeting.toLowerCase()) || cleaned.startsWith(greeting.toLowerCase())) {
                return true;
            }
        }
        return cleaned.length() <= 3;
    }

    private RoutingDecision defaultDecision(String userInput, String intent) {
        return RoutingDecision.builder()
                .userInput(userInput)
                .intent(intent)
                .emotionLabel("neutral")
                .targetAgents(new ArrayList<>(List.of("coach")))
                .parallel(false)
                .requireSafetyReview(false)
                .confidence(0.5)
                .build();
    }
}