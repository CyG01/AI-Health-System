package com.example.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;

public class HealthUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @DecimalMin(value = "100.0", message = "身高不能低于100cm")
    @DecimalMax(value = "250.0", message = "身高不能超过250cm")
    private BigDecimal height;

    @DecimalMin(value = "30.0", message = "体重不能低于30kg")
    @DecimalMax(value = "300.0", message = "体重不能超过300kg")
    private BigDecimal weight;

    @Size(max = 200, message = "健康目标不能超过200个字符")
    private String goal;

    @Size(max = 500, message = "既往病史不能超过500个字符")
    private String diseaseHistory;

    @Size(max = 500, message = "过敏史不能超过500个字符")
    private String allergyHistory;

    @Size(max = 500, message = "运动习惯不能超过500个字符")
    private String exerciseHabit;

    @Size(max = 500, message = "饮食习惯不能超过500个字符")
    private String dietHabit;

    public BigDecimal getHeight() {
        return height;
    }

    public void setHeight(BigDecimal height) {
        this.height = height;
    }

    public BigDecimal getWeight() {
        return weight;
    }

    public void setWeight(BigDecimal weight) {
        this.weight = weight;
    }

    public String getGoal() {
        return goal;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public String getDiseaseHistory() {
        return diseaseHistory;
    }

    public void setDiseaseHistory(String diseaseHistory) {
        this.diseaseHistory = diseaseHistory;
    }

    public String getAllergyHistory() {
        return allergyHistory;
    }

    public void setAllergyHistory(String allergyHistory) {
        this.allergyHistory = allergyHistory;
    }

    public String getExerciseHabit() {
        return exerciseHabit;
    }

    public void setExerciseHabit(String exerciseHabit) {
        this.exerciseHabit = exerciseHabit;
    }

    public String getDietHabit() {
        return dietHabit;
    }

    public void setDietHabit(String dietHabit) {
        this.dietHabit = dietHabit;
    }
}
