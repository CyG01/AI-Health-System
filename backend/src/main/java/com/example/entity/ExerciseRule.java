package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 运动规则表 —— 用于降级方案智能化
 */
@Data
@TableName("exercise_rules")
public class ExerciseRule {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 健康目标：减重/增肌/保持/康复 */
    private String goal;

    /** BMI 下限 */
    private BigDecimal bmiMin;

    /** BMI 上限 */
    private BigDecimal bmiMax;

    /** 运动类型：有氧/力量/柔韧/平衡 */
    private String exerciseType;

    /** 运动名称 */
    private String exerciseName;

    /** 默认时长（分钟） */
    private Integer defaultDuration;

    /** 默认强度：低/中/高 */
    private String defaultIntensity;

    /** 优先级（越小越高） */
    private Integer priority;

    /** 是否启用 */
    private Boolean isActive;
}