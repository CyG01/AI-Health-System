package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Schema(description = "睡眠记录提交DTO")
public class SleepRecordSubmitDTO {

    @NotNull(message = "记录日期不能为空")
    @Schema(description = "记录日期")
    private LocalDate recordDate;

    @NotNull(message = "入睡时间不能为空")
    @Schema(description = "入睡时间")
    private LocalTime sleepTime;

    @NotNull(message = "起床时间不能为空")
    @Schema(description = "起床时间")
    private LocalTime wakeTime;

    @NotNull(message = "睡眠质量不能为空")
    @Min(1)
    @Max(5)
    @Schema(description = "睡眠质量 1-很差 5-很好")
    private Integer quality;

    @Schema(description = "梦境/备注")
    private String dreamNotes;
}