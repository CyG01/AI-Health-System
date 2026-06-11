package com.example.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("diet_record")
public class DietRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long checkinId;

    private String mealType;

    private Long itemId;

    private Integer weightGrams;

    private Integer caloriesConsumed;

    private java.math.BigDecimal protein;

    private java.math.BigDecimal fat;

    private java.math.BigDecimal carbs;

    private String foodName;

    private String category;

    private String note;

    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

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

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}