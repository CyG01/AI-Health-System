package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
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
    @DecimalMax(value = "300", message = "腰围不能超过300cm")
    @Schema(description = "腰围(cm)")
    private BigDecimal waist;

    @Positive(message = "臀围必须大于0")
    @DecimalMax(value = "300", message = "臀围不能超过300cm")
    @Schema(description = "臀围(cm)")
    private BigDecimal hip;

    @Positive(message = "胸围必须大于0")
    @DecimalMax(value = "300", message = "胸围不能超过300cm")
    @Schema(description = "胸围(cm)")
    private BigDecimal chest;

    @DecimalMin(value = "0", message = "大腿围不能小于0cm")
    @DecimalMax(value = "200", message = "大腿围不能超过200cm")
    @Schema(description = "大腿围(cm)")
    private BigDecimal thigh;

    @DecimalMin(value = "0", message = "臂围不能小于0cm")
    @DecimalMax(value = "200", message = "臂围不能超过200cm")
    @Schema(description = "臂围(cm)")
    private BigDecimal arm;

    @DecimalMin(value = "3", message = "体脂率不能低于3%")
    @DecimalMax(value = "70", message = "体脂率不能超过70%")
    @Schema(description = "体脂率(%)")
    private BigDecimal bodyFatRate;

    @Schema(description = "备注")
    private String note;
}