package com.example.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "AI食物识别结果VO")
public class FoodRecognizeVO {

    @Schema(description = "食物名称")
    private String foodName;

    @Schema(description = "置信度 0-100")
    private Integer confidence;

    @Schema(description = "每100g热量(kcal)")
    private Integer caloriePer100g;

    @Schema(description = "蛋白质(g/100g)")
    private BigDecimal proteinPer100g;

    @Schema(description = "碳水化合物(g/100g)")
    private BigDecimal carbsPer100g;

    @Schema(description = "脂肪(g/100g)")
    private BigDecimal fatPer100g;

    @Schema(description = "建议食用量(克)")
    private Integer recommendedGrams;

    @Schema(description = "所属食物分类")
    private String category;
}