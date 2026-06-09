package com.example.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Data
@Schema(description = "睡眠记录VO")
public class SleepRecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "记录日期")
    private LocalDate recordDate;

    @Schema(description = "入睡时间")
    private LocalTime sleepTime;

    @Schema(description = "起床时间")
    private LocalTime wakeTime;

    @Schema(description = "睡眠时长(分钟)")
    private Integer durationMin;

    @Schema(description = "睡眠质量 1-5")
    private Integer quality;

    @Schema(description = "梦境/备注")
    private String dreamNotes;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}