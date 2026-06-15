package com.example.controller;

import com.example.annotation.NoRepeatSubmit;
import com.example.annotation.RateLimit;
import com.example.annotation.RequiresSubscription;
import com.example.common.Result;
import com.example.service.HealthReportService;
import com.example.vo.HealthReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "AI健康报告")
@RestController
@RequestMapping("/api/health-report")
public class HealthReportController {

    @Autowired
    private HealthReportService healthReportService;

    @RequiresSubscription(value = "pro", feature = "AI健康报告")
    @RateLimit(time = 60, count = 1)
    @NoRepeatSubmit
    @Operation(summary = "手动生成健康报告")
    @PostMapping("/generate")
    public Result<HealthReportVO> generate(@RequestBody(required = false) Map<String, String> body,
                                           @RequestAttribute("userId") Long userId) {
        String reportType = body != null ? body.getOrDefault("reportType", "weekly") : "weekly";
        if (!"weekly".equals(reportType) && !"monthly".equals(reportType)) {
            return Result.error(400, "报告类型只能为 weekly 或 monthly");
        }
        return Result.success(healthReportService.generateReport(userId, reportType));
    }

    @Operation(summary = "获取报告列表")
    @GetMapping("/list")
    public Result<List<HealthReportVO>> list(@RequestParam(defaultValue = "1") int page,
                                              @RequestParam(defaultValue = "10") int size,
                                              @RequestAttribute("userId") Long userId) {
        return Result.success(healthReportService.getReportList(userId, page, size));
    }

    @Operation(summary = "获取报告详情")
    @GetMapping("/{reportId}")
    public Result<HealthReportVO> detail(@PathVariable Long reportId,
                                          @RequestAttribute("userId") Long userId) {
        HealthReportVO vo = healthReportService.getReportDetail(reportId, userId);
        healthReportService.markAsRead(reportId, userId);
        return Result.success(vo);
    }
}