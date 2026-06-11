package com.example.controller;

import com.example.annotation.RequiresSubscription;
import com.example.common.Result;
import com.example.dto.PlanAdjustDTO;
import com.example.sdui.AiAgentResponse;
import com.example.service.PlanAdjustService;
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

    @RequiresSubscription(value = "pro", feature = "AI动态计划调整")
    @Operation(summary = "AI动态调整计划（SDUI协议）")
    @PostMapping("/adjust")
    public Result<AiAgentResponse> adjust(@Validated @RequestBody PlanAdjustDTO dto,
                                          @RequestAttribute("userId") Long userId) {
        return Result.success(planAdjustService.adjustPlan(dto.getOriginalPlanId(), userId, dto.getFeedback()));
    }
}