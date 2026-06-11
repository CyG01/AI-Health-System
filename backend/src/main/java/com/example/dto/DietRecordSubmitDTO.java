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

    private java.math.BigDecimal protein;
    private java.math.BigDecimal fat;
    private java.math.BigDecimal carbs;
    private String foodName;
    private String category;
    private String note;
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

    public java.math.BigDecimal getProtein() { return protein; }
    public void setProtein(java.math.BigDecimal protein) { this.protein = protein; }
    public java.math.BigDecimal getFat() { return fat; }
    public void setFat(java.math.BigDecimal fat) { this.fat = fat; }
    public java.math.BigDecimal getCarbs() { return carbs; }
    public void setCarbs(java.math.BigDecimal carbs) { this.carbs = carbs; }
    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}