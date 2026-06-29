package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;

public class ExerciseItemUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "ID不能为空")
    private Long id;

    @NotBlank(message = "运动名称不能为空")
    private String name;

    @NotBlank(message = "运动类型不能为空")
    private String type;

    @NotNull(message = "热量系数不能为空")
    private BigDecimal calorieCoefficient;

    private String targetMuscle;

    private String difficulty;

    private String videoUrl;

    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

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
