package com.example.vo;

import java.io.Serializable;
import java.math.BigDecimal;

public class CheckinStatsVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer consecutiveDays;

    private Integer totalDays;

    private Integer currentWeekDays;

    private Integer currentMonthDays;

    private BigDecimal exerciseCompleteRate;

    private BigDecimal dietCompleteRate;

    public Integer getConsecutiveDays() {
        return consecutiveDays;
    }

    public void setConsecutiveDays(Integer consecutiveDays) {
        this.consecutiveDays = consecutiveDays;
    }

    public Integer getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(Integer totalDays) {
        this.totalDays = totalDays;
    }

    public Integer getCurrentWeekDays() {
        return currentWeekDays;
    }

    public void setCurrentWeekDays(Integer currentWeekDays) {
        this.currentWeekDays = currentWeekDays;
    }

    public Integer getCurrentMonthDays() {
        return currentMonthDays;
    }

    public void setCurrentMonthDays(Integer currentMonthDays) {
        this.currentMonthDays = currentMonthDays;
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
}
