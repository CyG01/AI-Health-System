package com.example.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public class CheckinSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long planId;

    @NotNull(message = "运动完成状态不能为空")
    @Min(value = 0, message = "运动状态取值范围为0~2")
    @Max(value = 2, message = "运动状态取值范围为0~2")
    private Integer exerciseStatus;

    @NotNull(message = "饮食完成状态不能为空")
    @Min(value = 0, message = "饮食状态取值范围为0~2")
    @Max(value = 2, message = "饮食状态取值范围为0~2")
    private Integer dietStatus;

    private Integer currentWeight;

    private String mood;

    private String note;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public Integer getExerciseStatus() { return exerciseStatus; }
    public void setExerciseStatus(Integer exerciseStatus) { this.exerciseStatus = exerciseStatus; }

    public Integer getDietStatus() { return dietStatus; }
    public void setDietStatus(Integer dietStatus) { this.dietStatus = dietStatus; }

    public Integer getCurrentWeight() { return currentWeight; }
    public void setCurrentWeight(Integer currentWeight) { this.currentWeight = currentWeight; }

    public String getMood() { return mood; }
    public void setMood(String mood) { this.mood = mood; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}