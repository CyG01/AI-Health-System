package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

@Data
@Schema(description = "AI动态计划调整请求DTO")
public class PlanAdjustDTO {

    @NotNull(message = "计划ID不能为空")
    @Schema(description = "原计划ID")
    private Long originalPlanId;

    @Schema(description = "用户反馈说明", example = "强度太大了，膝盖有点痛")
    private String feedback;
}