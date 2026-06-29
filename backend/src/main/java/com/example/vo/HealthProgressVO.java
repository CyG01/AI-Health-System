package com.example.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 体重目标进度追踪
 */
public class HealthProgressVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前体重(kg) */
    private Integer currentWeight;

    /** 目标体重(kg) */
    private Integer targetWeight;

    /** 初始体重(kg)（最近一次设置了目标体重的记录） */
    private Integer initialWeight;

    /** 已减体重(kg)，正数表示减重，负数表示增重 */
    private Integer lostWeight;

    /** 剩余需减(kg) */
    private Integer remainingWeight;

    /** 进度百分比(0-100) */
    private Integer progressPercent;

    /** 已完成 */
    private boolean completed;

    /** 体重变化趋势 */
    private List<HealthAssessmentVO.TrendPoint> weightTrend;

    public Integer getCurrentWeight() { return currentWeight; }
    public void setCurrentWeight(Integer currentWeight) { this.currentWeight = currentWeight; }

    public Integer getTargetWeight() { return targetWeight; }
    public void setTargetWeight(Integer targetWeight) { this.targetWeight = targetWeight; }

    public Integer getInitialWeight() { return initialWeight; }
    public void setInitialWeight(Integer initialWeight) { this.initialWeight = initialWeight; }

    public Integer getLostWeight() { return lostWeight; }
    public void setLostWeight(Integer lostWeight) { this.lostWeight = lostWeight; }

    public Integer getRemainingWeight() { return remainingWeight; }
    public void setRemainingWeight(Integer remainingWeight) { this.remainingWeight = remainingWeight; }

    public Integer getProgressPercent() { return progressPercent; }
    public void setProgressPercent(Integer progressPercent) { this.progressPercent = progressPercent; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public List<HealthAssessmentVO.TrendPoint> getWeightTrend() { return weightTrend; }
    public void setWeightTrend(List<HealthAssessmentVO.TrendPoint> weightTrend) { this.weightTrend = weightTrend; }
}