package com.example.vo;

import java.io.Serializable;
import java.util.List;

public class DashboardTodayVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private boolean isCheckedIn;

    /** 连续打卡天数 */
    private Integer streakDays;

    private Long planId;
    private String planName;
    private List<TaskItem> tasks;
    private Integer completedTasks;
    private Integer totalTasks;

    private Integer exerciseCaloriesBurned;
    private Integer exerciseRecordsCount;

    private Integer dietCaloriesConsumed;
    private Integer dietRecordsCount;

    public boolean getIsCheckedIn() { return isCheckedIn; }
    public void setIsCheckedIn(boolean isCheckedIn) { this.isCheckedIn = isCheckedIn; }

    public Integer getStreakDays() { return streakDays; }
    public void setStreakDays(Integer streakDays) { this.streakDays = streakDays; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public List<TaskItem> getTasks() { return tasks; }
    public void setTasks(List<TaskItem> tasks) { this.tasks = tasks; }

    public Integer getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(Integer completedTasks) { this.completedTasks = completedTasks; }

    public Integer getTotalTasks() { return totalTasks; }
    public void setTotalTasks(Integer totalTasks) { this.totalTasks = totalTasks; }

    public Integer getExerciseCaloriesBurned() { return exerciseCaloriesBurned; }
    public void setExerciseCaloriesBurned(Integer exerciseCaloriesBurned) { this.exerciseCaloriesBurned = exerciseCaloriesBurned; }

    public Integer getExerciseRecordsCount() { return exerciseRecordsCount; }
    public void setExerciseRecordsCount(Integer exerciseRecordsCount) { this.exerciseRecordsCount = exerciseRecordsCount; }

    public Integer getDietCaloriesConsumed() { return dietCaloriesConsumed; }
    public void setDietCaloriesConsumed(Integer dietCaloriesConsumed) { this.dietCaloriesConsumed = dietCaloriesConsumed; }

    public Integer getDietRecordsCount() { return dietRecordsCount; }
    public void setDietRecordsCount(Integer dietRecordsCount) { this.dietRecordsCount = dietRecordsCount; }

    public static class TaskItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long detailId;
        private String itemType;
        private String itemName;
        private String targetAmount;
        private Integer status;

        public Long getDetailId() { return detailId; }
        public void setDetailId(Long detailId) { this.detailId = detailId; }

        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }

        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }

        public String getTargetAmount() { return targetAmount; }
        public void setTargetAmount(String targetAmount) { this.targetAmount = targetAmount; }

        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}