package com.example.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public class HealthCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "身高不能为空")
    @Min(value = 100, message = "身高不能低于100cm")
    @Max(value = 250, message = "身高不能超过250cm")
    private Integer height;

    @NotNull(message = "体重不能为空")
    @Min(value = 30, message = "体重不能低于30kg")
    @Max(value = 300, message = "体重不能超过300kg")
    private Integer weight;

    @Min(value = 30, message = "目标体重不能低于30kg")
    @Max(value = 300, message = "目标体重不能超过300kg")
    private Integer targetWeight;

    @NotBlank(message = "健康目标不能为空")
    @Size(max = 200, message = "健康目标不能超过200个字符")
    private String goal;

    @Size(max = 500, message = "既往病史不能超过500个字符")
    private String diseaseHistory;

    @Size(max = 500, message = "过敏史不能超过500个字符")
    private String allergyHistory;

    @Size(max = 200, message = "过敏类型不能超过200个字符")
    private String allergyType;

    @Size(max = 500, message = "家族病史不能超过500个字符")
    private String familyHistory;

    @Size(max = 500, message = "用药情况不能超过500个字符")
    private String medication;

    @Size(max = 500, message = "运动习惯不能超过500个字符")
    private String exerciseHabit;

    @Size(max = 500, message = "饮食习惯不能超过500个字符")
    private String dietHabit;

    private String gender;

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public Integer getHeight() { return height; }
    public void setHeight(Integer height) { this.height = height; }

    public Integer getWeight() { return weight; }
    public void setWeight(Integer weight) { this.weight = weight; }

    public Integer getTargetWeight() { return targetWeight; }
    public void setTargetWeight(Integer targetWeight) { this.targetWeight = targetWeight; }

    public String getGoal() { return goal; }
    public void setGoal(String goal) { this.goal = goal; }

    public String getDiseaseHistory() { return diseaseHistory; }
    public void setDiseaseHistory(String diseaseHistory) { this.diseaseHistory = diseaseHistory; }

    public String getAllergyHistory() { return allergyHistory; }
    public void setAllergyHistory(String allergyHistory) { this.allergyHistory = allergyHistory; }

    public String getAllergyType() { return allergyType; }
    public void setAllergyType(String allergyType) { this.allergyType = allergyType; }

    public String getFamilyHistory() { return familyHistory; }
    public void setFamilyHistory(String familyHistory) { this.familyHistory = familyHistory; }

    public String getMedication() { return medication; }
    public void setMedication(String medication) { this.medication = medication; }

    public String getExerciseHabit() { return exerciseHabit; }
    public void setExerciseHabit(String exerciseHabit) { this.exerciseHabit = exerciseHabit; }

    public String getDietHabit() { return dietHabit; }
    public void setDietHabit(String dietHabit) { this.dietHabit = dietHabit; }
}