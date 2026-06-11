package com.example.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Prompt 注入防护工具类
 * 用于过滤用户可控字段（goal、preference、diseaseHistory、allergyHistory 等）中的注入攻击模式
 */
@Component
public class PromptSanitizer {

    // 注入攻击模式
    private static final Pattern[] INJECTION_PATTERNS = {
            Pattern.compile("(?i)(忽略|忘记|无视|override|ignore|forget)\\s*(前面|以上|之前|所有|一切|现在开始)"),
            Pattern.compile("(?i)(你|your|system)\\s*[。，,.]*\\s*(是|现在是|从现在起|角色|身份|变成|改为)"),
            Pattern.compile("(?i)输出.*?(笑话|色情|暴力|非法|违规|政治|反动)"),
            Pattern.compile("(?i)(\\[DONE\\]|\\[SYSTEM\\]|<\\||\\|>)"),
            Pattern.compile("(?i)(按照我说的|遵循以下|执行以下指令|你必须|你必须按)"),
            Pattern.compile("(?i)(ignore|forget|override|skip)\\s+(all|everything|previous|above)\\s+(instructions|prompts|rules)"),
            Pattern.compile("(?i)(pretend|act\\s+as|roleplay)\\s+(you\\s+are|as)"),
            Pattern.compile("(?i)(DAN|jailbreak|system\\s*prompt|prompt\\s*(injection|leak))"),
    };

    /**
     * 对输入字符串进行注入检测和过滤
     */
    public static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String result = input;
        for (Pattern p : INJECTION_PATTERNS) {
            result = p.matcher(result).replaceAll("[已过滤]");
        }
        // 移除代码块标记，防止 prompt 注入利用
        result = result.replaceAll("```", "");
        // 移除可能的 JSON/SYSTEM 标记注入
        result = result.replaceAll("(?i)\\{\\s*\"role\"\\s*:\\s*\"system\"", "[已过滤]");
        return result.trim();
    }
}