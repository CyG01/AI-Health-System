package com.example.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public class DietRecordSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "打卡ID不能为空")
    private Long checkinId;

    @NotBlank(message = "餐次不能为空")
    private String mealType;

    @NotNull(message = "食物ID不能为空")
    private Long itemId;

    @NotNull(message = "食用重量不能为空")
    @Min(value = 1, message = "食用重量至少1克")
    @Max(value = 10000, message = "食用重量不能超过10000克")
    private Integer weightGrams;

    @NotNull(message = "摄入热量不能为空")
    @Min(value = 0, message = "摄入热量不能为负")
    @Max(value = 10000, message = "摄入热量不能超过10000大卡")
    private Integer caloriesConsumed;

    private String remark;

    public Long getCheckinId() { return checkinId; }
    public void setCheckinId(Long checkinId) { this.checkinId = checkinId; }

    public String getMealType() { return mealType; }
    public void setMealType(String mealType) { this.mealType = mealType; }

    public Long getItemId() { return itemId; }
    public void setItemId(Long itemId) { this.itemId = itemId; }

    public Integer getWeightGrams() { return weightGrams; }
    public void setWeightGrams(Integer weightGrams) { this.weightGrams = weightGrams; }

    public Integer getCaloriesConsumed() { return caloriesConsumed; }
    public void setCaloriesConsumed(Integer caloriesConsumed) { this.caloriesConsumed = caloriesConsumed; }

    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}