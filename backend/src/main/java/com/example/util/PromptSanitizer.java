package com.example.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Prompt 注入防护工具类
 * 用于过滤用户可控字段（goal、preference、diseaseHistory、allergyHistory 等）中的注入攻击模式
 */
@Component
public class PromptSanitizer {

    // 通用注入攻击模式
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

    // 健康领域特化注入模式：防止用户诱导 AI 开处方、诊断疾病或绕过安全过滤
    private static final Pattern[] HEALTH_INJECTION_PATTERNS = {
            // 伪装医生身份
            Pattern.compile("(?i)(假装|扮演|你现在是|你就是)\\s*(医生|医师|大夫|主任医师|专家|教授|药剂师)"),
            Pattern.compile("(?i)(pretend|act\\s+as|you\\s+are)\\s*(a\\s+)?(doctor|physician|surgeon|pharmacist|medical\\s+expert)"),
            // 诱导开处方/用药建议
            Pattern.compile("(?i)(给我开|帮我开|推荐我吃|建议我服用| prescribe|recommend\\s+(me\\s+)?)\\s*(药|处方|抗生素|安眠药|止痛药|激素)"),
            // 绕过安全过滤
            Pattern.compile("(?i)(不用|无需|跳过|忽略|关闭)\\s*(免责声明|安全提示|风险提示|警告|医疗提醒)"),
            Pattern.compile("(?i)(no\\s+need\\s+to|skip|ignore|disable)\\s*(disclaimer|safety|warning|caution)"),
            // 诱导确诊
            Pattern.compile("(?i)(帮我确诊|诊断我|判断我得的是|我是不是得了)"),
    };

    /** 用户消息最大长度 */
    private static final int MAX_CONTENT_LENGTH = 2000;

    /** 标记已过滤的占位符 */
    private static final String FILTERED_TAG = "[已过滤]";

    /**
     * 对输入字符串进行注入检测和过滤（通用模式）
     */
    public static String sanitize(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }
        String result = input;
        for (Pattern p : INJECTION_PATTERNS) {
            result = p.matcher(result).replaceAll(FILTERED_TAG);
        }
        // 移除代码块标记，防止 prompt 注入利用
        result = result.replaceAll("```", "");
        // 移除可能的 JSON/SYSTEM 标记注入
        result = result.replaceAll("(?i)\\{\\s*\"role\"\\s*:\\s*\"system\"", FILTERED_TAG);
        return result.trim();
    }

    /**
     * 对聊天消息内容进行注入检测（通用 + 健康领域双重过滤）。
     * 同时执行长度截断保护。
     */
    public static String sanitizeChatMessage(String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        // 长度截断
        String truncated = content.length() > MAX_CONTENT_LENGTH
                ? content.substring(0, MAX_CONTENT_LENGTH)
                : content;

        // 通用注入过滤
        String result = sanitize(truncated);

        // 健康领域注入过滤
        for (Pattern p : HEALTH_INJECTION_PATTERNS) {
            result = p.matcher(result).replaceAll(FILTERED_TAG);
        }

        return result;
    }

    /**
     * 检测输入是否包含注入攻击模式（不修改内容，仅判断）。
     * 用于审计日志记录。
     */
    public static boolean containsInjection(String input) {
        if (input == null || input.isBlank()) {
            return false;
        }
        for (Pattern p : INJECTION_PATTERNS) {
            if (p.matcher(input).find()) return true;
        }
        for (Pattern p : HEALTH_INJECTION_PATTERNS) {
            if (p.matcher(input).find()) return true;
        }
        return input.contains("```")
                || input.toLowerCase().contains("{\"role\":\"system\"");
    }
}