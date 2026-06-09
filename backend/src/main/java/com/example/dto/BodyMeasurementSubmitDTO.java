package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "身体围度提交DTO")
public class BodyMeasurementSubmitDTO {

    @NotNull(message = "记录日期不能为空")
    @Schema(description = "记录日期")
    private LocalDate recordDate;

    @Positive(message = "腰围必须大于0")
    @Schema(description = "腰围(cm)")
    private BigDecimal waist;

    @Positive(message = "臀围必须大于0")
    @Schema(description = "臀围(cm)")
    private BigDecimal hip;

    @Positive(message = "胸围必须大于0")
    @Schema(description = "胸围(cm)")
    private BigDecimal chest;

    @Schema(description = "大腿围(cm)")
    private BigDecimal thigh;

    @Schema(description = "臂围(cm)")
    private BigDecimal arm;

    @Schema(description = "体脂率(%)")
    private BigDecimal bodyFatRate;

    @Schema(description = "备注")
    private String note;
}