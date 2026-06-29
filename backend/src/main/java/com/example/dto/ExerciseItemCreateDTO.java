package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.math.BigDecimal;

public class ExerciseItemCreateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "运动名称不能为空")
    @Size(max = 50, message = "运动名称最长50个字符")
    private String name;

    @NotBlank(message = "运动类型不能为空")
    @Size(max = 20, message = "运动类型最长20个字符")
    private String type;

    @NotNull(message = "热量系数不能为空")
    private BigDecimal calorieCoefficient;

    @Size(max = 50, message = "目标肌群最长50个字符")
    private String targetMuscle;

    @Size(max = 20, message = "难度等级最长20个字符")
    private String difficulty;

    private String videoUrl;

    private Integer status;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public BigDecimal getCalorieCoefficient() { return calorieCoefficient; }
    public void setCalorieCoefficient(BigDecimal calorieCoefficient) { this.calorieCoefficient = calorieCoefficient; }

    public String getTargetMuscle() { return targetMuscle; }
    public void setTargetMuscle(String targetMuscle) { this.targetMuscle = targetMuscle; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
