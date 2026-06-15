package com.example.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "AI运动指导VO")
public class ExerciseGuidanceVO {

    @Schema(description = "运动名称")
    private String exerciseName;

    @Schema(description = "基本信息")
    private BasicInfo basicInfo;

    @Schema(description = "呼吸节奏")
    private String breathing;

    @Schema(description = "动作要领（分步骤）")
    private List<String> steps;

    @Schema(description = "常见错误")
    private List<String> commonMistakes;

    @Schema(description = "安全提示")
    private String tips;

    @Data
    @Schema(description = "运动基本信息")
    public static class BasicInfo {
        @Schema(description = "运动类型")
        private String type;

        @Schema(description = "目标肌群")
        private String targetMuscle;

        @Schema(description = "难度等级")
        private String difficulty;
    }
}
