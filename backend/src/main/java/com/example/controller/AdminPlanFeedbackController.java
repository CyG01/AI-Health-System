package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.annotation.AdminOnly;
import com.example.common.Result;
import com.example.service.AuditLogService;
import com.example.service.PlanFeedbackService;
import com.example.vo.PlanFeedbackVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理员计划反馈管理")
@AdminOnly
@RestController
@RequestMapping("/api/admin/plan-feedback")
public class AdminPlanFeedbackController {

    @Autowired
    private PlanFeedbackService planFeedbackService;

    @Autowired
    private AuditLogService auditLogService;

    @Operation(summary = "分页查看所有反馈")
    @GetMapping("/list")
    public Result<Page<PlanFeedbackVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(planFeedbackService.getAllFeedbacks(page, size));
    }

    @Operation(summary = "查看反馈详情")
    @GetMapping("/{id}")
    public Result<PlanFeedbackVO> detail(@PathVariable Long id) {
        return Result.success(planFeedbackService.getFeedbackById(id));
    }

    @Operation(summary = "触发AI调整生成新计划")
    @PostMapping("/{id}/adjust")
    public Result<PlanFeedbackVO> adjust(@PathVariable Long id,
                                         @RequestAttribute("userId") Long userId,
                                         HttpServletRequest request) {
        PlanFeedbackVO vo = planFeedbackService.triggerAdjust(id);
        auditLogService.log(userId, null, "ADJUST_PLAN", "plan_feedback", id,
                "触发AI计划调整 feedbackId=" + id + " newPlanId=" + (vo != null ? vo.getNewPlanId() : "N/A"),
                request.getRemoteAddr());
        return Result.success(vo);
    }
}