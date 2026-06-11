package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

/**
 * 安全规则表 — 用于拦截危险运动计划。
 * 心血管/骨科等禁忌运动的硬编码规则表。
 */
@TableName("safety_rule")
public class SafetyRule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户健康状况（如"高血压"、"膝盖损伤"、"孕期"） */
    private String userCondition;

    /** 禁止的运动关键词（逗号分隔，如"深蹲,HIIT,倒立"） */
    private String forbiddenKeywords;

    /** 最大建议时长（分钟） */
    private Integer maxDuration;

    /** 最大建议强度：低/中/高 */
    private String maxIntensity;

    /** 风险等级：HIGH/MEDIUM/LOW */
    private String riskLevel;

    /** 替代建议 */
    private String alternativeSuggestion;

    /** 是否启用 */
    private Integer isActive;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserCondition() {
        return userCondition;
    }

    public void setUserCondition(String userCondition) {
        this.userCondition = userCondition;
    }

    public String getForbiddenKeywords() {
        return forbiddenKeywords;
    }

    public void setForbiddenKeywords(String forbiddenKeywords) {
        this.forbiddenKeywords = forbiddenKeywords;
    }

    public Integer getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(Integer maxDuration) {
        this.maxDuration = maxDuration;
    }

    public String getMaxIntensity() {
        return maxIntensity;
    }

    public void setMaxIntensity(String maxIntensity) {
        this.maxIntensity = maxIntensity;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public String getAlternativeSuggestion() {
        return alternativeSuggestion;
    }

    public void setAlternativeSuggestion(String alternativeSuggestion) {
        this.alternativeSuggestion = alternativeSuggestion;
    }

    public Integer getIsActive() {
        return isActive;
    }

    public void setIsActive(Integer isActive) {
        this.isActive = isActive;
    }
}