package com.example.controller;

import com.example.annotation.NoRepeatSubmit;
import com.example.annotation.RateLimit;
import com.example.common.Result;
import com.example.dto.AiFeedbackDTO;
import com.example.service.AiFeedbackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * AI反馈控制器。
 * 用户提交对AI建议的评价，管理员审核反馈。
 */
@Tag(name = "AI反馈", description = "用户对AI建议的评价与反馈")
@RestController
@RequestMapping("/api/ai/feedback")
public class AiFeedbackController {

    private final AiFeedbackService feedbackService;

    public AiFeedbackController(AiFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @RateLimit(time = 60, count = 5)
    @NoRepeatSubmit
    @Operation(summary = "提交AI反馈")
    @PostMapping
    public Result<Void> submitFeedback(@Valid @RequestBody AiFeedbackDTO dto,
                                       @RequestAttribute("userId") Long userId) {
        feedbackService.submitFeedback(userId, dto);
        return Result.success();
    }

    @Operation(summary = "获取待审核反馈列表（管理员）")
    @GetMapping("/pending")
    public Result<?> getPendingReview() {
        return Result.success(feedbackService.getPendingReviewList());
    }

    @Operation(summary = "审核反馈（管理员）")
    @PostMapping("/review/{id}")
    public Result<Void> review(@PathVariable Long id,
                               @RequestParam String result,
                               @RequestAttribute("userId") Long reviewerId) {
        feedbackService.reviewFeedback(id, result, reviewerId);
        return Result.success();
    }
}