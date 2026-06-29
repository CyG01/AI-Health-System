package com.example.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 健康评估 含 AI 建议和趋势分析
 */
public class HealthAssessmentVO extends HealthRecordVO {

    private String bmiLevel;

    private List<String> risks;

    /** 健康评分 0-100 */
    private Integer healthScore;

    /** AI 生成的个性化改善建议 */
    private String aiSuggestion;

    /** 体重趋势列表 */
    private List<TrendPoint> weightTrend;

    /** BMI趋势列表 */
    private List<TrendPoint> bmiTrend;

    // === 新增评估维度 ===

    /** 估算体脂率(%) */
    private BigDecimal estimatedBodyFatRate;

    /** 体脂率等级 */
    private String bodyFatLevel;

    /** 基础代谢评估 */
    private String bmrAssessment;

    /** 心血管风险评估 */
    private String cardiovascularRisk;

    /** 运动能力评估 */
    private String exerciseAbility;

    public String getBmiLevel() {
        return bmiLevel;
    }

    public void setBmiLevel(String bmiLevel) {
        this.bmiLevel = bmiLevel;
    }

    public List<String> getRisks() {
        return risks;
    }

    public void setRisks(List<String> risks) {
        this.risks = risks;
    }

    public Integer getHealthScore() { return healthScore; }
    public void setHealthScore(Integer healthScore) { this.healthScore = healthScore; }

    public String getAiSuggestion() { return aiSuggestion; }
    public void setAiSuggestion(String aiSuggestion) { this.aiSuggestion = aiSuggestion; }

    public List<TrendPoint> getWeightTrend() { return weightTrend; }
    public void setWeightTrend(List<TrendPoint> weightTrend) { this.weightTrend = weightTrend; }

    public List<TrendPoint> getBmiTrend() { return bmiTrend; }
    public void setBmiTrend(List<TrendPoint> bmiTrend) { this.bmiTrend = bmiTrend; }

    public BigDecimal getEstimatedBodyFatRate() { return estimatedBodyFatRate; }
    public void setEstimatedBodyFatRate(BigDecimal estimatedBodyFatRate) { this.estimatedBodyFatRate = estimatedBodyFatRate; }

    public String getBodyFatLevel() { return bodyFatLevel; }
    public void setBodyFatLevel(String bodyFatLevel) { this.bodyFatLevel = bodyFatLevel; }

    public String getBmrAssessment() { return bmrAssessment; }
    public void setBmrAssessment(String bmrAssessment) { this.bmrAssessment = bmrAssessment; }

    public String getCardiovascularRisk() { return cardiovascularRisk; }
    public void setCardiovascularRisk(String cardiovascularRisk) { this.cardiovascularRisk = cardiovascularRisk; }

    public String getExerciseAbility() { return exerciseAbility; }
    public void setExerciseAbility(String exerciseAbility) { this.exerciseAbility = exerciseAbility; }

    /**
     * 趋势数据点
     */
    public static class TrendPoint implements Serializable {
        private static final long serialVersionUID = 1L;

        private String date;
        private BigDecimal value;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public BigDecimal getValue() { return value; }
        public void setValue(BigDecimal value) { this.value = value; }
    }
}
