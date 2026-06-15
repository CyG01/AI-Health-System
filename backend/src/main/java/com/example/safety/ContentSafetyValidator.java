package com.example.safety;

import com.example.agent.SafetyReviewAgent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 三层安全防线内容安全校验器 — 完整编排版。
 *
 * Layer1: Regex 正则快速拦截（毫秒级）
 * Layer2: BERT 模型语义判别（Python 微服务）→ 目标召回率 >= 99.9%
 * Layer3: SafetyReviewAgent 最终审查（LLM Agent）
 */
@Slf4j
@Component
public class ContentSafetyValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ===================== Layer1: Regex 关键词 =====================

    private static final Set<String> MEDICAL_REDLINE_KEYWORDS = new HashSet<>(Arrays.asList(
            "自杀", "轻生", "跳楼", "割腕", "上吊", "一了百了", "不想活",
            "安眠药自杀", "农药", "毒药", "百草枯", "氰化物",
            "精神类药物滥用", "吸毒", "成瘾药物",
            "医生坑人", "医院骗钱", "不打疫苗", "停掉所有药",
            "包治百病", "祖传秘方", "抗癌秘方",
            "非法堕胎", "代孕", "器官买卖", "捐卵卖卵",
            "试药", "兼职试药", "有偿试药"
    ));

    /** 药学相关白名单：咨询药物信息不拦截 */
    private static final Set<String> PHARMA_WHITELIST = new HashSet<>(Arrays.asList(
            "处方药有哪些", "抗生素是什么", "止痛药种类", "安眠药作用",
            "什么是安眠药", "安眠药说明书", "阿司匹林", "布洛芬"
    ));

    private static final Pattern REDLINE_REGEX = Pattern.compile(
            String.join("|", MEDICAL_REDLINE_KEYWORDS),
            Pattern.CASE_INSENSITIVE
    );

    /** DOS/注入/隐私泄露等通用危险模式 */
    private static final Set<Pattern> THREAT_PATTERNS = new HashSet<>(Arrays.asList(
            Pattern.compile("<script[\\s>]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\$\\{.*\\}"),
            Pattern.compile("../../../"),
            Pattern.compile("union\\s+select", Pattern.CASE_INSENSITIVE),
            Pattern.compile("(\\d{6}(19|20)\\d{2}(0[1-9]|1[0-2])(0[1-9]|[12]\\d|3[01])\\d{3}[\\dXx])"),
            Pattern.compile("1[3-9]\\d{9}")
    ));

    @Autowired(required = false)
    private BertRedlineClient bertRedlineClient;

    @Autowired(required = false)
    private SafetyReviewAgent safetyReviewAgent;

    // ===================== 公开 API =====================

    /**
     * 输入侧安全校验：Layer1 + Layer2（不调用 Layer3）。
     * 在用户消息进入 Agent 之前调用。
     */
    public SafetyResult validateInput(String content) {
        if (!StringUtils.hasText(content)) {
            return SafetyResult.pass();
        }

        // Layer1: Regex
        SafetyResult l1 = layer1RegexCheck(content);
        if (!l1.passed()) {
            log.warn("[Safety-L1] 输入拦截: {}", l1.reason());
            return l1;
        }

        // Layer2: BERT
        SafetyResult l2 = layer2BertCheck(content);
        if (!l2.passed()) {
            log.warn("[Safety-L2] 输入拦截: {}", l2.reason());
            return l2;
        }

        return SafetyResult.pass();
    }

    /**
     * 输出侧安全校验：Layer1 + Layer2 + Layer3。
     * 在 Agent 生成回复之后、返回给用户之前调用。
     */
    public SafetyResult validateOutput(String content, String userContext, String knowledgeContext) {
        if (!StringUtils.hasText(content)) {
            return SafetyResult.pass();
        }

        // Layer1: Regex
        SafetyResult l1 = layer1RegexCheck(content);
        if (!l1.passed()) {
            log.warn("[Safety-L1] 输出拦截: {}", l1.reason());
            return l1;
        }

        // Layer2: BERT
        SafetyResult l2 = layer2BertCheck(content);
        if (!l2.passed()) {
            log.warn("[Safety-L2] 输出拦截: {}", l2.reason());
            return l2;
        }

        // Layer3: LLM Agent 最终审查
        SafetyResult l3 = layer3AgentReview(content, userContext, knowledgeContext);
        if (!l3.passed()) {
            log.warn("[Safety-L3] 审查结果: {}", l3.reason());
            return l3;
        }

        return SafetyResult.pass();
    }

    // ===================== 各层实现 =====================

    private SafetyResult layer1RegexCheck(String content) {
        String lower = content.toLowerCase();

        // 威胁模式检查
        for (Pattern pattern : THREAT_PATTERNS) {
            if (pattern.matcher(content).find()) {
                return SafetyResult.block("检测到敏感信息或注入尝试（L1）");
            }
        }

        // 医疗红线关键词检查
        if (REDLINE_REGEX.matcher(content).find()) {
            for (String whitelist : PHARMA_WHITELIST) {
                if (lower.contains(whitelist.toLowerCase())) {
                    return SafetyResult.pass();
                }
            }
            return SafetyResult.block("消息涉及医疗安全红线内容，无法回复（L1）");
        }

        return SafetyResult.pass();
    }

    private SafetyResult layer2BertCheck(String content) {
        if (bertRedlineClient == null || !bertRedlineClient.isEnabled()) {
            return SafetyResult.pass();
        }

        try {
            BertRedlineClient.BertClassifyResponse bertResult =
                    bertRedlineClient.classify(content).get(2, TimeUnit.SECONDS);

            if (bertResult != null && !bertResult.isSafe()) {
                return SafetyResult.block(
                        String.format("消息经 AI 安全审查判定为高风险，无法回复（L2, confidence=%.2f）",
                                bertResult.getConfidence())
                );
            }
        } catch (java.util.concurrent.TimeoutException e) {
            log.warn("[Safety-L2] BERT 服务超时，降级放行");
        } catch (Exception e) {
            log.error("[Safety-L2] BERT 判别异常: {}", e.getMessage());
        }

        return SafetyResult.pass();
    }

    @SuppressWarnings("unchecked")
    private SafetyResult layer3AgentReview(String content, String userContext, String knowledgeContext) {
        if (safetyReviewAgent == null) {
            log.warn("[Safety-L3] SafetyReviewAgent 未注入，跳过");
            return SafetyResult.pass();
        }

        try {
            String reviewResult = safetyReviewAgent.review(
                    userContext != null ? userContext : "无特殊健康档案",
                    knowledgeContext != null ? knowledgeContext : "无专业知识库上下文",
                    content
            );

            if (reviewResult == null || reviewResult.isBlank()) {
                return SafetyResult.pass();
            }

            // 解析 JSON 审查结果
            Map<String, Object> result = MAPPER.readValue(reviewResult, Map.class);
            String verdict = (String) result.getOrDefault("verdict", "PASS");

            if ("BLOCK".equalsIgnoreCase(verdict)) {
                List<String> issues = (List<String>) result.get("issues");
                String issueText = issues != null && !issues.isEmpty()
                        ? String.join("; ", issues)
                        : "内容存在安全风险";
                return SafetyResult.block("内容经安全审查不通过: " + issueText);
            }

            if ("MODIFY".equalsIgnoreCase(verdict)) {
                List<String> suggestions = (List<String>) result.get("suggestions");
                String suggestionText = suggestions != null && !suggestions.isEmpty()
                        ? String.join("; ", suggestions)
                        : "内容需修改";
                return SafetyResult.modify(suggestionText);
            }

        } catch (JsonProcessingException e) {
            log.warn("[Safety-L3] JSON 解析失败: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[Safety-L3] 审查异常: {}", e.getMessage());
        }

        return SafetyResult.pass();
    }

    /**
     * 仅 Layer1 快速检查。
     */
    public SafetyResult quickCheck(String content) {
        return layer1RegexCheck(content);
    }

    /**
     * 批量校验输入。
     */
    public List<SafetyResult> batchValidateInput(List<String> contents) {
        return contents.stream().map(this::validateInput).toList();
    }

    // ===================== 内部类 =====================

    public record SafetyResult(
            boolean passed,
            String reason,
            boolean needsModify,
            String modifySuggestion
    ) {
        public static SafetyResult pass() {
            return new SafetyResult(true, "", false, "");
        }

        public static SafetyResult block(String reason) {
            return new SafetyResult(false, reason, false, "");
        }

        public static SafetyResult modify(String suggestion) {
            return new SafetyResult(false, "内容需修改", true, suggestion);
        }

        public boolean wasBlockedByRedline() {
            return !passed && !needsModify;
        }
    }
}