package com.example.controller;

import com.example.common.Result;
import com.example.service.HealthReportService;
import com.example.vo.HealthReportVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AI健康报告")
@RestController
@RequestMapping("/api/health-report")
public class HealthReportController {

    @Autowired
    private HealthReportService healthReportService;

    @Operation(summary = "手动生成健康报告")
    @PostMapping("/generate")
    public Result<HealthReportVO> generate(@RequestParam(defaultValue = "weekly") String reportType,
                                           @RequestAttribute("userId") Long userId) {
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