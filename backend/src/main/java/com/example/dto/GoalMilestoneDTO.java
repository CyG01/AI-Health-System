package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "目标里程碑创建/更新DTO")
public class GoalMilestoneDTO {

    @Schema(description = "目标ID (更新时传)")
    private Long id;

    @NotBlank(message = "目标类型不能为空")
    @Schema(description = "目标类型: weight_loss/weight_gain/muscle_gain/exercise_days/checkin_days/water_target/custom")
    private String goalType;

    @NotBlank(message = "目标名称不能为空")
    @Schema(description = "目标名称")
    private String goalName;

    @NotNull(message = "目标值不能为空")
    @Positive(message = "目标值必须大于0")
    @Schema(description = "目标值")
    private BigDecimal targetValue;

    @Schema(description = "单位: kg/天/ml/次")
    private String unit;

    @Schema(description = "起始日期")
    private LocalDate startDate;

    @Schema(description = "目标日期")
    private LocalDate targetDate;
}