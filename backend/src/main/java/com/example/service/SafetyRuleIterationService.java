package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.ComplianceRule;
import com.example.entity.RuleSuggestion;
import com.example.entity.SafetyRule;
import com.example.entity.SamplingResult;
import com.example.mapper.RuleSuggestionMapper;
import com.example.mapper.SafetyRuleMapper;
import com.example.mapper.SamplingResultMapper;
import com.example.mapper.ComplianceRuleMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 安全规则迭代服务。
 * 分析线上采样结果中的问题，自动生成规则建议，驱动 SafetyRule/ComplianceRule 持续演进。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SafetyRuleIterationService {

    private final SamplingResultMapper samplingResultMapper;
    private final RuleSuggestionMapper ruleSuggestionMapper;
    private final SafetyRuleMapper safetyRuleMapper;
    private final ComplianceRuleMapper complianceRuleMapper;
    private final ObjectMapper objectMapper;

    /** 提取违规关键词的正则：引号内的短语、或「涉及XX」形式的短语 */
    private static final Pattern KEYWORD_PATTERN = Pattern.compile(
            "[「\"\"]([^」\"\"]+)[」\"\"]|涉及([^，,。\\s]+)|包含([^，,。\\s]+)|出现([^，,。\\s]+)|使用了([^，,。\\s]+)");

    /**
     * 定时分析未处理的采样结果，生成规则建议。
     * 每10分钟执行一次。
     */
    @Scheduled(fixedDelay = 600_000)
    @Transactional
    public void analyzeSamplingResults() {
        log.info("开始分析未处理的采样结果...");

        LambdaQueryWrapper<SamplingResult> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SamplingResult::getAnalyzed, 0)
                .eq(SamplingResult::getVerdict, "fail")
                .orderByAsc(SamplingResult::getCreatedAt);

        List<SamplingResult> unprocessed = samplingResultMapper.selectList(wrapper);
        if (unprocessed.isEmpty()) {
            log.debug("没有新的未处理采样结果");
            return;
        }

        log.info("发现 {} 条未处理的失败采样结果", unprocessed.size());

        // 合井同类问题
        Map<String, List<SamplingResult>> groupedIssues = groupByIssue(unprocessed);

        for (Map.Entry<String, List<SamplingResult>> entry : groupedIssues.entrySet()) {
            String issueKeyword = entry.getKey();
            List<SamplingResult> relatedResults = entry.getValue();

            // 检查是否已有相同建议
            LambdaQueryWrapper<RuleSuggestion> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(RuleSuggestion::getTriggerPattern, issueKeyword)
                    .eq(RuleSuggestion::getStatus, "pending");
            if (ruleSuggestionMapper.selectCount(existWrapper) > 0) {
                continue;
            }

            String sourceIds = relatedResults.stream()
                    .map(r -> String.valueOf(r.getId()))
                    .collect(Collectors.joining(","));

            String reason = buildReason(relatedResults, issueKeyword);

            String suggestionType = determineSuggestionType(issueKeyword);

            RuleSuggestion suggestion = new RuleSuggestion();
            suggestion.setSuggestionType(suggestionType);
            suggestion.setTriggerPattern(issueKeyword);
            suggestion.setAction("block");
            suggestion.setPriority(calculatePriority(relatedResults.size()));
            suggestion.setReason(reason);
            suggestion.setSourceSampleIds(sourceIds);
            suggestion.setHitCount(relatedResults.size());
            suggestion.setStatus("pending");
            suggestion.setCreatedAt(LocalDateTime.now());

            ruleSuggestionMapper.insert(suggestion);
            log.info("生成规则建议: keyword={} type={} hitCount={}", issueKeyword, suggestionType, relatedResults.size());
        }

        // 标记所有已分析的采样结果为已处理
        for (SamplingResult result : unprocessed) {
            result.setAnalyzed(1);
            samplingResultMapper.updateById(result);
        }

        log.info("采样分析完成, 共处理 {} 条采样结果", unprocessed.size());
    }

    /**
     * 审批并应用规则建议。
     * 根据 suggestionType 创建 SafetyRule 或 ComplianceRule。
     */
    @Transactional
    public String approveSuggestion(Long suggestionId, String reviewerName) {
        RuleSuggestion suggestion = ruleSuggestionMapper.selectById(suggestionId);
        if (suggestion == null) {
            return "规则建议不存在";
        }
        if (!"pending".equals(suggestion.getStatus())) {
            return "该建议已处理";
        }

        boolean isCompliance = "compliance_rule".equals(suggestion.getSuggestionType());

        if (isCompliance) {
            // 检查是否已有相同合规规则
            LambdaQueryWrapper<ComplianceRule> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(ComplianceRule::getMatchPattern, suggestion.getTriggerPattern());
            if (complianceRuleMapper.selectCount(existWrapper) > 0) {
                suggestion.setStatus("approved");
                suggestion.setReviewedBy(reviewerName);
                suggestion.setReviewedAt(LocalDateTime.now());
                ruleSuggestionMapper.updateById(suggestion);
                return "该关键词已存在合规规则，无需重复添加";
            }

            ComplianceRule rule = new ComplianceRule();
            rule.setRuleType("forbidden_term");
            rule.setMatchPattern(suggestion.getTriggerPattern());
            rule.setAction("block");
            rule.setDescription(suggestion.getReason());
            rule.setIsActive(1);
            complianceRuleMapper.insert(rule);
        } else {
            // 检查是否已有相同安全规则
            LambdaQueryWrapper<SafetyRule> existWrapper = new LambdaQueryWrapper<>();
            existWrapper.eq(SafetyRule::getForbiddenKeywords, suggestion.getTriggerPattern());
            if (safetyRuleMapper.selectCount(existWrapper) > 0) {
                suggestion.setStatus("approved");
                suggestion.setReviewedBy(reviewerName);
                suggestion.setReviewedAt(LocalDateTime.now());
                ruleSuggestionMapper.updateById(suggestion);
                return "该关键词已存在安全规则，无需重复添加";
            }

            SafetyRule rule = new SafetyRule();
            rule.setUserCondition("generic");
            rule.setForbiddenKeywords(suggestion.getTriggerPattern());
            rule.setRiskLevel(suggestion.getPriority() >= 8 ? "HIGH" : suggestion.getPriority() >= 5 ? "MEDIUM" : "LOW");
            rule.setAlternativeSuggestion(suggestion.getReason());
            rule.setIsActive(1);
            safetyRuleMapper.insert(rule);
        }

        // 更新建议状态
        suggestion.setStatus("approved");
        suggestion.setReviewedBy(reviewerName);
        suggestion.setReviewedAt(LocalDateTime.now());
        ruleSuggestionMapper.updateById(suggestion);

        log.info("规则建议已审批并生效: id={} keyword={} type={} reviewer={}",
                suggestionId, suggestion.getTriggerPattern(), suggestion.getSuggestionType(), reviewerName);
        return "规则已生效";
    }

    /**
     * 拒绝规则建议。
     */
    public String rejectSuggestion(Long suggestionId, String reviewerName) {
        RuleSuggestion suggestion = ruleSuggestionMapper.selectById(suggestionId);
        if (suggestion == null) {
            return "规则建议不存在";
        }
        suggestion.setStatus("rejected");
        suggestion.setReviewedBy(reviewerName);
        suggestion.setReviewedAt(LocalDateTime.now());
        ruleSuggestionMapper.updateById(suggestion);
        return "建议已拒绝";
    }

    /**
     * 获取所有待审核的规则建议。
     */
    public List<RuleSuggestion> getPendingSuggestions() {
        LambdaQueryWrapper<RuleSuggestion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RuleSuggestion::getStatus, "pending")
                .orderByDesc(RuleSuggestion::getHitCount);
        return ruleSuggestionMapper.selectList(wrapper);
    }

    /**
     * 将 issues 按关键词合并分组。
     */
    private Map<String, List<SamplingResult>> groupByIssue(List<SamplingResult> results) {
        Map<String, List<SamplingResult>> grouped = new LinkedHashMap<>();

        for (SamplingResult result : results) {
            List<String> issues = parseJsonArray(result.getIssues());
            for (String issue : issues) {
                String keyword = extractKeyword(issue);
                if (StringUtils.hasText(keyword) && keyword.length() >= 2 && keyword.length() <= 30) {
                    grouped.computeIfAbsent(keyword, k -> new ArrayList<>()).add(result);
                }
            }
        }
        return grouped;
    }

    /**
     * 从问题描述中提取关键违规词。
     */
    private String extractKeyword(String issue) {
        if (!StringUtils.hasText(issue)) return null;
        Matcher matcher = KEYWORD_PATTERN.matcher(issue);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null && matcher.group(i).length() >= 2) {
                    return matcher.group(i).trim();
                }
            }
        }
        // 回退：取问题描述的前20字符作为关键词
        return issue.length() > 20 ? issue.substring(0, 20) : issue;
    }

    /**
     * 构建建议原因描述。
     */
    private String buildReason(List<SamplingResult> results, String keyword) {
        double avgSafety = results.stream().mapToDouble(r -> r.getSafety() != null ? r.getSafety() : 0).average().orElse(0);
        return String.format("Online采样发现 %d 次失败，关键词「%s」，平均安全评分 %.1f/10，建议添加拦截规则",
                results.size(), keyword, avgSafety);
    }

    /**
     * 判断建议类型。
     */
    private String determineSuggestionType(String keyword) {
        // 涉医疗诊断用语 -> compliance_rule
        if (keyword.contains("诊断") || keyword.contains("治疗") || keyword.contains("处方")
                || keyword.contains("治愈") || keyword.contains("根除") || keyword.contains("保证")
                || keyword.contains("绝对") || keyword.contains("100%")) {
            return "compliance_rule";
        }
        return "safety_rule";
    }

    /**
     * 根据命中次数计算优先级。
     */
    private int calculatePriority(int hitCount) {
        if (hitCount >= 5) return 9;
        if (hitCount >= 3) return 7;
        if (hitCount >= 2) return 5;
        return 3;
    }

    private List<String> parseJsonArray(String json) {
        if (!StringUtils.hasText(json)) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            // 如果解析失败，尝试当作纯文本
            return Collections.singletonList(json);
        }
    }
}