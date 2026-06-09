package com.example.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "AI运动指导VO")
public class ExerciseGuidanceVO {

    @Schema(description = "运动名称")
    private String exerciseName;

    @Schema(description = "动作要领")
    private String technique;

    @Schema(description = "呼吸节奏")
    private String breathing;

    @Schema(description = "常见错误")
    private String commonMistakes;

    @Schema(description = "安全提示")
    private String safetyTips;
}