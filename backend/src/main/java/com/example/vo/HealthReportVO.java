package com.example.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "健康报告VO")
public class HealthReportVO {

    @Schema(description = "报告ID")
    private Long id;

    @Schema(description = "报告类型 weekly/monthly")
    private String reportType;

    @Schema(description = "报告周期")
    private String reportPeriod;

    @Schema(description = "AI报告内容(JSON)")
    private String aiContent;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "是否已读")
    private Integer isRead;
}