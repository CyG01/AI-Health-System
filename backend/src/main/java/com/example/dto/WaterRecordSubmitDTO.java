package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
@Schema(description = "饮水记录提交DTO")
public class WaterRecordSubmitDTO {

    @NotNull(message = "记录日期不能为空")
    @Schema(description = "记录日期")
    private LocalDate recordDate;

    @NotNull(message = "饮水量不能为空")
    @Min(value = 1, message = "饮水量至少1ml")
    @Schema(description = "饮水量(ml)")
    private Integer amountMl;
}