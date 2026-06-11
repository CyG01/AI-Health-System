package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;

@TableName("exercise_item")
public class ExerciseItem implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String type;

    private BigDecimal calorieCoefficient;

    private String targetMuscle;

    private String difficulty;

    private String videoUrl;

    /** AI 生成的指导说明 */
    private String aiGuidance;

    private Integer status;

    // ... getters/setters ...

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

    public String getAiGuidance() { return aiGuidance; }
    public void setAiGuidance(String aiGuidance) { this.aiGuidance = aiGuidance; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}