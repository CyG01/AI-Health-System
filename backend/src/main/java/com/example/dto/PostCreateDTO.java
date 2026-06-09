package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "发布帖子DTO")
public class PostCreateDTO {

    @Schema(description = "内容", example = "今天跑步5公里，感觉很好！")
    private String content;

    @Schema(description = "图片列表(JSON)")
    private String images;

    @Schema(description = "运动类型")
    private String exerciseType;

    @Schema(description = "运动时长(分钟)")
    private Integer exerciseDuration;

    @Schema(description = "消耗热量")
    private Integer caloriesBurned;
}