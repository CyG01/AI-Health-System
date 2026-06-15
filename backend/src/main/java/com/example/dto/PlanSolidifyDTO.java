package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 计划固化请求 DTO。
 * 将聊天中生成的临时计划（DRAFT 状态）固化为正式计划（ACTIVE 状态）。
 */
@Data
@Schema(description = "计划固化请求")
public class PlanSolidifyDTO {

    @NotNull(message = "临时计划ID不能为空")
    @Schema(description = "临时计划ID", example = "42")
    private Long tempPlanId;

    @Schema(description = "计划版本号（用户在聊天中调整后的最终版本）", example = "1")
    private Integer version;
}
