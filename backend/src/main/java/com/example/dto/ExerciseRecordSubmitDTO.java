package com.example.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public class ExerciseRecordSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "打卡ID不能为空")
    private Long checkinId;

    @NotNull(message = "运动项目ID不能为空")
    private Long itemId;

    @NotNull(message = "运动时长不能为空")
    @Min(value = 1, message = "运动时长至少1分钟")
    @Max(value = 1440, message = "运动时长不能超过1440分钟")
    private Integer durationMinutes;

    @NotNull(message = "消耗热量不能为空")
    @Min(value = 0, message = "消耗热量不能为负")
    @Max(value = 10000, message = "消耗热量不能超过10000大卡")
    private Integer caloriesBurned;

    public Long getCheckinId() { return checkinId; }
    public void setCheckinId(Long checkinId) { this.checkinId = checkinId; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Integer getDurationMinutes() { return durationMinutes; }
    public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

    public Integer getCaloriesBurned() { return caloriesBurned; }
    public void setCaloriesBurned(Integer caloriesBurned) { this.caloriesBurned = caloriesBurned; }
}