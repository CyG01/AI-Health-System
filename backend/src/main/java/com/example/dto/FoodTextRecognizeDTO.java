package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 自然语言食物识别请求 DTO（一句话记账）。
 */
@Data
@Schema(description = "自然语言食物识别请求")
public class FoodTextRecognizeDTO {

    @NotBlank(message = "文字描述不能为空")
    @Schema(description = "自然语言描述", example = "中午吃了一碗兰州拉面加煎蛋")
    private String text;
}
