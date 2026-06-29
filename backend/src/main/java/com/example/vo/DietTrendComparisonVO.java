package com.example.vo;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 饮食热量多维度趋势对比VO（本周 vs 上周 / 本月 vs 上月）。
 */
public class DietTrendComparisonVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 当前周期总热量 */
    private int currentTotalCalories;
    /** 对比周期总热量 */
    private int previousTotalCalories;
    /** 热量变化百分比 */
    private double calorieChangePercent;
    /** 当前周期标签（如 "6/4-6/10"） */
    private String currentPeriodLabel;
    /** 对比周期标签（如 "5/28-6/3"） */
    private String previousPeriodLabel;
    /** 每日数据：当前周期 {date: "6/4", calories: 1800} */
    private List<DailyCal> currentDaily;
    /** 每日数据：对比周期 */
    private List<DailyCal> previousDaily;

    public static class DailyCal implements Serializable {
        private static final long serialVersionUID = 1L;
        private String date;
        private int calories;
        private String dayLabel;

        public DailyCal() {}
        public DailyCal(String date, int calories, String dayLabel) {
            this.date = date;
            this.calories = calories;
            this.dayLabel = dayLabel;
        }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public int getCalories() { return calories; }
        public void setCalories(int calories) { this.calories = calories; }
        public String getDayLabel() { return dayLabel; }
        public void setDayLabel(String dayLabel) { this.dayLabel = dayLabel; }
    }

    public int getCurrentTotalCalories() { return currentTotalCalories; }
    public void setCurrentTotalCalories(int currentTotalCalories) { this.currentTotalCalories = currentTotalCalories; }
    public int getPreviousTotalCalories() { return previousTotalCalories; }
    public void setPreviousTotalCalories(int previousTotalCalories) { this.previousTotalCalories = previousTotalCalories; }
    public double getCalorieChangePercent() { return calorieChangePercent; }
    public void setCalorieChangePercent(double calorieChangePercent) { this.calorieChangePercent = calorieChangePercent; }
    public String getCurrentPeriodLabel() { return currentPeriodLabel; }
    public void setCurrentPeriodLabel(String currentPeriodLabel) { this.currentPeriodLabel = currentPeriodLabel; }
    public String getPreviousPeriodLabel() { return previousPeriodLabel; }
    public void setPreviousPeriodLabel(String previousPeriodLabel) { this.previousPeriodLabel = previousPeriodLabel; }
    public List<DailyCal> getCurrentDaily() { return currentDaily; }
    public void setCurrentDaily(List<DailyCal> currentDaily) { this.currentDaily = currentDaily; }
    public List<DailyCal> getPreviousDaily() { return previousDaily; }
    public void setPreviousDaily(List<DailyCal> previousDaily) { this.previousDaily = previousDaily; }
}