package com.example.vo;

import java.io.Serializable;
import java.math.BigDecimal;

public class ProgressVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private BigDecimal totalCheckinRate;

    private BigDecimal exerciseCompleteRate;

    private BigDecimal dietCompleteRate;

    private BigDecimal weightChange;

    private BigDecimal targetProgressPercent;

    private String goal;

    public BigDecimal getTotalCheckinRate() {
        return totalCheckinRate;
    }

    public void setTotalCheckinRate(BigDecimal totalCheckinRate) {
        this.totalCheckinRate = totalCheckinRate;
    }

    public BigDecimal getExerciseCompleteRate() {
        return exerciseCompleteRate;
    }

    public void setExerciseCompleteRate(BigDecimal exerciseCompleteRate) {
        this.exerciseCompleteRate = exerciseCompleteRate;
    }

    public BigDecimal getDietCompleteRate() {
        return dietCompleteRate;
    }

    public void setDietCompleteRate(BigDecimal dietCompleteRate) {
        this.dietCompleteRate = dietCompleteRate;
    }

    public BigDecimal getWeightChange() {
        return weightChange;
    }

    public void setWeightChange(BigDecimal weightChange) {
        this.weightChange = weightChange;
    }

    public BigDecimal getTargetProgressPercent() {
        return targetProgressPercent;
    }

    public void setTargetProgressPercent(BigDecimal targetProgressPercent) {
        this.targetProgressPercent = targetProgressPercent;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }
}
