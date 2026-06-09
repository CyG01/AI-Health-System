package com.example.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 本周概览
 */
public class DashboardWeekVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 周起始日期 */
    private String weekStart;
    /** 周结束日期 */
    private String weekEnd;

    /** 本周打卡天数 */
    private Integer checkinDays;
    /** 本周运动消耗总热量 */
    private Integer exerciseCalories;
    /** 本周饮食摄入总热量 */
    private Integer dietCalories;
    /** 本周运动记录数 */
    private Integer exerciseRecordsCount;
    /** 本周饮食记录数 */
    private Integer dietRecordsCount;

    /** 每日明细 */
    private List<DaySummary> dailySummary;

    public String getWeekStart() { return weekStart; }
    public void setWeekStart(String weekStart) { this.weekStart = weekStart; }

    public String getWeekEnd() { return weekEnd; }
    public void setWeekEnd(String weekEnd) { this.weekEnd = weekEnd; }

    public Integer getCheckinDays() { return checkinDays; }
    public void setCheckinDays(Integer checkinDays) { this.checkinDays = checkinDays; }

    public Integer getExerciseCalories() { return exerciseCalories; }
    public void setExerciseCalories(Integer exerciseCalories) { this.exerciseCalories = exerciseCalories; }

    public Integer getDietCalories() { return dietCalories; }
    public void setDietCalories(Integer dietCalories) { this.dietCalories = dietCalories; }

    public Integer getExerciseRecordsCount() { return exerciseRecordsCount; }
    public void setExerciseRecordsCount(Integer exerciseRecordsCount) { this.exerciseRecordsCount = exerciseRecordsCount; }

    public Integer getDietRecordsCount() { return dietRecordsCount; }
    public void setDietRecordsCount(Integer dietRecordsCount) { this.dietRecordsCount = dietRecordsCount; }

    public List<DaySummary> getDailySummary() { return dailySummary; }
    public void setDailySummary(List<DaySummary> dailySummary) { this.dailySummary = dailySummary; }

    public static class DaySummary implements Serializable {
        private static final long serialVersionUID = 1L;

        private String date;
        private boolean checkedIn;
        private Integer exerciseCalories;
        private Integer dietCalories;
        private Integer exerciseCount;
        private Integer dietCount;

        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }

        public boolean isCheckedIn() { return checkedIn; }
        public void setCheckedIn(boolean checkedIn) { this.checkedIn = checkedIn; }

        public Integer getExerciseCalories() { return exerciseCalories; }
        public void setExerciseCalories(Integer exerciseCalories) { this.exerciseCalories = exerciseCalories; }

        public Integer getDietCalories() { return dietCalories; }
        public void setDietCalories(Integer dietCalories) { this.dietCalories = dietCalories; }

        public Integer getExerciseCount() { return exerciseCount; }
        public void setExerciseCount(Integer exerciseCount) { this.exerciseCount = exerciseCount; }

        public Integer getDietCount() { return dietCount; }
        public void setDietCount(Integer dietCount) { this.dietCount = dietCount; }
    }
}