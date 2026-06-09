package com.example.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "饮水记录VO")
public class WaterRecordVO {

    @Schema(description = "记录ID")
    private Long id;

    @Schema(description = "记录日期")
    private LocalDate recordDate;

    @Schema(description = "饮水量(ml)")
    private Integer amountMl;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}