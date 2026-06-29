package com.example.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Schema(description = "目标里程碑VO")
public class GoalMilestoneVO {

    @Schema(description = "目标ID")
    private Long id;

    @Schema(description = "目标类型")
    private String goalType;

    @Schema(description = "目标类型标签")
    private String goalTypeLabel;

    @Schema(description = "目标名称")
    private String goalName;

    @Schema(description = "目标值")
    private BigDecimal targetValue;

    @Schema(description = "当前值")
    private BigDecimal currentValue;

    @Schema(description = "单位")
    private String unit;

    @Schema(description = "进度百分比")
    private Integer progressPercent;

    @Schema(description = "起始日期")
    private LocalDate startDate;

    @Schema(description = "目标日期")
    private LocalDate targetDate;

    @Schema(description = "剩余天数")
    private Long remainingDays;

    @Schema(description = "状态: 0-进行中 1-已完成 2-已放弃")
    private Integer status;

    @Schema(description = "状态标签")
    private String statusLabel;

    @Schema(description = "完成日期")
    private LocalDate completedDate;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;
}