package com.example.controller;

import com.example.common.Result;
import com.example.service.ExerciseGuidanceService;
import com.example.vo.ExerciseGuidanceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI运动指导")
@RestController
@RequestMapping("/api/exercise")
public class ExerciseGuidanceController {

    @Autowired
    private ExerciseGuidanceService exerciseGuidanceService;

    @Operation(summary = "获取运动项目的AI动作指导")
    @GetMapping("/{exerciseId}/guidance")
    public Result<ExerciseGuidanceVO> guidance(@PathVariable Long exerciseId) {
        return Result.success(exerciseGuidanceService.getGuidance(exerciseId));
    }
}