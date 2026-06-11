package com.example.controller;

import com.example.annotation.NoRepeatSubmit;
import com.example.annotation.RateLimit;
import com.example.common.Result;
import com.example.dto.PlanFeedbackDTO;
import com.example.service.PlanFeedbackService;
import com.example.vo.PlanFeedbackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "计划反馈")
@RestController
@RequestMapping("/api/plan-feedback")
public class PlanFeedbackController {

    @Autowired
    private PlanFeedbackService planFeedbackService;

    @NoRepeatSubmit
    @RateLimit(time = 60, count = 3, message = "反馈提交过于频繁，请1分钟后再试")
    @Operation(summary = "提交计划反馈")
    @PostMapping
    public Result<PlanFeedbackVO> submit(
            @Validated @RequestBody PlanFeedbackDTO dto,
            @RequestAttribute("userId") Long userId) {
        return Result.success(planFeedbackService.submitFeedback(userId, dto));
    }

    @Operation(summary = "查询某计划的反馈记录")
    @GetMapping("/plan/{planId}")
    public Result<List<PlanFeedbackVO>> getByPlanId(@RequestAttribute("userId") Long userId,
                                                     @PathVariable Long planId) {
        return Result.success(planFeedbackService.getFeedbacksByUserIdAndPlanId(userId, planId));
    }

    @Operation(summary = "查询本人的反馈记录")
    @GetMapping("/my")
    public Result<List<PlanFeedbackVO>> getMyFeedbacks(@RequestAttribute("userId") Long userId) {
        return Result.success(planFeedbackService.getFeedbacksByUserId(userId));
    }
}