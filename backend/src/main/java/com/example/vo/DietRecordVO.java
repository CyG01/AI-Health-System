package com.example.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class DietRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private Long checkinId;
    private String mealType;
    private Long itemId;
    private String itemName;
    private Integer weightGrams;
    private Integer caloriesConsumed;
    private BigDecimal protein;
    private BigDecimal fat;
    private BigDecimal carbs;
    private String foodName;
    private String category;
    private String note;
    private String remark;
    private LocalDateTime createTime;
}