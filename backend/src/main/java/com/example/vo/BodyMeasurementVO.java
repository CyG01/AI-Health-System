package com.example.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "身体围度VO")
public class BodyMeasurementVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "记录日期")
    private LocalDate recordDate;

    @Schema(description = "腰围(cm)")
    private BigDecimal waist;

    @Schema(description = "臀围(cm)")
    private BigDecimal hip;

    @Schema(description = "腰臀比")
    private BigDecimal waistHipRatio;

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

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}