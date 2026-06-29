package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(description = "食物识别请求DTO")
public class FoodRecognizeDTO {

    @NotNull(message = "图片不能为空")
    @Schema(description = "食物图片")
    private MultipartFile image;
}