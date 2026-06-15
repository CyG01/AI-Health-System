package com.example.controller;

import com.example.common.Result;
import com.example.service.DashboardService;
import com.example.vo.DashboardGreetingVO;
import com.example.vo.DashboardMonthVO;
import com.example.vo.DashboardTodayVO;
import com.example.vo.DashboardWeekVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "用户仪表盘")
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    @Operation(summary = "今日概览（含连续打卡天数）")
    @GetMapping("/today")
    public Result<DashboardTodayVO> today(@RequestAttribute("userId") Long userId) {
        return Result.success(dashboardService.getTodayOverview(userId));
    }

    @Operation(summary = "本周概览（每日明细）")
    @GetMapping("/week")
    public Result<DashboardWeekVO> week(@RequestAttribute("userId") Long userId) {
        return Result.success(dashboardService.getWeekOverview(userId));
    }

    @Operation(summary = "本月概览（按周汇总、打卡率）")
    @GetMapping("/month")
    public Result<DashboardMonthVO> month(@RequestAttribute("userId") Long userId) {
        return Result.success(dashboardService.getMonthOverview(userId));
    }

    @Operation(summary = "AI 预测性问候卡片（规则引擎，非大模型）")
    @GetMapping("/greeting")
    public Result<DashboardGreetingVO> greeting(@RequestAttribute("userId") Long userId) {
        return Result.success(dashboardService.generateGreeting(userId));
    }
}