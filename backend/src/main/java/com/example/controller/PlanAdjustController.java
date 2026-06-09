package com.example.controller;

import com.example.common.Result;
import com.example.dto.PlanAdjustDTO;
import com.example.service.PlanAdjustService;
import com.example.vo.AiPlanDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AI动态计划调整")
@RestController
@RequestMapping("/api/ai-plan")
public class PlanAdjustController {

    @Autowired
    private PlanAdjustService planAdjustService;

    @Operation(summary = "AI动态调整计划")
    @PostMapping("/adjust")
    public Result<AiPlanDetailVO> adjust(@Validated @RequestBody PlanAdjustDTO dto,
                                          @RequestAttribute("userId") Long userId) {
        return Result.success(planAdjustService.adjustPlan(dto.getOriginalPlanId(), userId, dto.getFeedback()));
    }
}