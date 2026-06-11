package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OnboardingRequest {

    @NotBlank(message = "健康目标不能为空")
    private String healthGoal;

    @NotBlank(message = "运动基础不能为空")
    private String fitnessLevel;

    /** 慢性疾病或运动损伤（多个用逗号分隔，无则填"无"） */
    private String conditions;

    /** 饮食偏好或忌口（多个用逗号分隔，无则填"无"） */
    private String dietPreferences;

    @NotNull(message = "每日可用运动时间不能为空")
    private Integer dailyAvailableMin;

    /** 睡眠质量自评：POOR/AVERAGE/GOOD */
    private String sleepQuality;

    /** 压力水平：LOW/MEDIUM/HIGH */
    private String stressLevel;

    // --- getters/setters ---

    public String getHealthGoal() { return healthGoal; }
    public void setHealthGoal(String healthGoal) { this.healthGoal = healthGoal; }

    public String getFitnessLevel() { return fitnessLevel; }
    public void setFitnessLevel(String fitnessLevel) { this.fitnessLevel = fitnessLevel; }

    public String getConditions() { return conditions; }
    public void setConditions(String conditions) { this.conditions = conditions; }

    public String getDietPreferences() { return dietPreferences; }
    public void setDietPreferences(String dietPreferences) { this.dietPreferences = dietPreferences; }

    public Integer getDailyAvailableMin() { return dailyAvailableMin; }
    public void setDailyAvailableMin(Integer dailyAvailableMin) { this.dailyAvailableMin = dailyAvailableMin; }

    public String getSleepQuality() { return sleepQuality; }
    public void setSleepQuality(String sleepQuality) { this.sleepQuality = sleepQuality; }

    public String getStressLevel() { return stressLevel; }
    public void setStressLevel(String stressLevel) { this.stressLevel = stressLevel; }
}