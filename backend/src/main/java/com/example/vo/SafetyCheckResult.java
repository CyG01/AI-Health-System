package com.example.vo;

import com.example.entity.ComplianceRule;
import com.example.entity.SafetyRule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 安全检查结果。
 */
public class SafetyCheckResult implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 是否通过安全检查 */
    private boolean passed = true;

    /** 安全违规项（运动禁忌） */
    private List<SafetyRule> violations = new ArrayList<>();

    /** 合规问题（医疗术语等） */
    private List<ComplianceRule> complianceIssues = new ArrayList<>();

    /** 违规对应的任务描述 */
    private List<String> offendingItems = new ArrayList<>();

    /** 汇总提示消息 */
    private String message;

    /** 替代建议 */
    private List<String> suggestions = new ArrayList<>();

    /** 是否需要 AI 二次判断 */
    private boolean ambiguous;

    public void addViolation(SafetyRule rule, String item) {
        this.passed = false;
        this.violations.add(rule);
        this.offendingItems.add(item);
        if (rule.getAlternativeSuggestion() != null) {
            this.suggestions.add(rule.getAlternativeSuggestion());
        }
    }

    public void addComplianceIssue(ComplianceRule rule, String item) {
        if ("block".equals(rule.getAction())) {
            this.passed = false;
        }
        this.complianceIssues.add(rule);
        this.offendingItems.add(item);
    }

    public boolean isPassed() {
        return passed;
    }

    public void setPassed(boolean passed) {
        this.passed = passed;
    }

    public List<SafetyRule> getViolations() {
        return violations;
    }

    public void setViolations(List<SafetyRule> violations) {
        this.violations = violations;
    }

    public List<ComplianceRule> getComplianceIssues() {
        return complianceIssues;
    }

    public void setComplianceIssues(List<ComplianceRule> complianceIssues) {
        this.complianceIssues = complianceIssues;
    }

    public List<String> getOffendingItems() {
        return offendingItems;
    }

    public void setOffendingItems(List<String> offendingItems) {
        this.offendingItems = offendingItems;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public boolean isAmbiguous() {
        return ambiguous;
    }

    public void setAmbiguous(boolean ambiguous) {
        this.ambiguous = ambiguous;
    }
}