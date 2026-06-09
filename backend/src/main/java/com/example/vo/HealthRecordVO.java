package com.example.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class HealthRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Integer height;
    private Integer weight;
    private Integer targetWeight;
    private BigDecimal bmi;
    private Integer bmr;
    private Integer dailyCalorie;
    private String goal;
    private String diseaseHistory;
    private String allergyHistory;
    private String exerciseHabit;
    private String dietHabit;
    private Integer isLatest;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    public Integer getTargetWeight() { return targetWeight; }
    public void setTargetWeight(Integer targetWeight) { this.targetWeight = targetWeight; }

    public BigDecimal getBmi() { return bmi; }
    public void setBmi(BigDecimal bmi) { this.bmi = bmi; }

    public Integer getBmr() { return bmr; }
    public void setBmr(Integer bmr) { this.bmr = bmr; }

    public Integer getDailyCalorie() { return dailyCalorie; }
    public void setDailyCalorie(Integer dailyCalorie) { this.dailyCalorie = dailyCalorie; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getDiseaseHistory() { return diseaseHistory; }
    public void setDiseaseHistory(String diseaseHistory) { this.diseaseHistory = diseaseHistory; }

    public String getAllergyHistory() { return allergyHistory; }
    public void setAllergyHistory(String allergyHistory) { this.allergyHistory = allergyHistory; }

    public String getExerciseHabit() { return exerciseHabit; }
    public void setExerciseHabit(String exerciseHabit) { this.exerciseHabit = exerciseHabit; }

    public String getDietHabit() { return dietHabit; }
    public void setDietHabit(String dietHabit) { this.dietHabit = dietHabit; }

    public Integer getIsLatest() { return isLatest; }
    public void setIsLatest(Integer isLatest) { this.isLatest = isLatest; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}