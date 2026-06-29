package com.example.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 本月概览
 */
public class DashboardMonthVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 月份，格式 yyyy-MM */
    private String month;

    /** 本月打卡天数 */
    private Integer checkinDays;
    /** 本月总天数 */
    private Integer totalDays;
    /** 打卡率 */
    private Double checkinRate;

    /** 本月运动消耗总热量 */
    private Integer exerciseCalories;
    /** 本月饮食摄入总热量 */
    private Integer dietCalories;
    /** 本月运动记录数 */
    private Integer exerciseRecordsCount;
    /** 本月饮食记录数 */
    private Integer dietRecordsCount;

    /** 按周汇总 */
    private List<WeekSummary> weeklySummary;

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public Integer getCheckinDays() { return checkinDays; }
    public void setCheckinDays(Integer checkinDays) { this.checkinDays = checkinDays; }

    public Integer getTotalDays() { return totalDays; }
    public void setTotalDays(Integer totalDays) { this.totalDays = totalDays; }

    public Double getCheckinRate() { return checkinRate; }
    public void setCheckinRate(Double checkinRate) { this.checkinRate = checkinRate; }

    public Integer getExerciseCalories() { return exerciseCalories; }
    public void setExerciseCalories(Integer exerciseCalories) { this.exerciseCalories = exerciseCalories; }

    public Integer getDietCalories() { return dietCalories; }
    public void setDietCalories(Integer dietCalories) { this.dietCalories = dietCalories; }

    public Integer getExerciseRecordsCount() { return exerciseRecordsCount; }
    public void setExerciseRecordsCount(Integer exerciseRecordsCount) { this.exerciseRecordsCount = exerciseRecordsCount; }

    public Integer getDietRecordsCount() { return dietRecordsCount; }
    public void setDietRecordsCount(Integer dietRecordsCount) { this.dietRecordsCount = dietRecordsCount; }

    public List<WeekSummary> getWeeklySummary() { return weeklySummary; }
    public void setWeeklySummary(List<WeekSummary> weeklySummary) { this.weeklySummary = weeklySummary; }

    public static class WeekSummary implements Serializable {
        private static final long serialVersionUID = 1L;

        private String weekLabel;
        private Integer checkinDays;
        private Integer exerciseCalories;
        private Integer dietCalories;

        public String getWeekLabel() { return weekLabel; }
        public void setWeekLabel(String weekLabel) { this.weekLabel = weekLabel; }

        public Integer getCheckinDays() { return checkinDays; }
        public void setCheckinDays(Integer checkinDays) { this.checkinDays = checkinDays; }

        public Integer getExerciseCalories() { return exerciseCalories; }
        public void setExerciseCalories(Integer exerciseCalories) { this.exerciseCalories = exerciseCalories; }

        public Integer getDietCalories() { return dietCalories; }
        public void setDietCalories(Integer dietCalories) { this.dietCalories = dietCalories; }
    }
}