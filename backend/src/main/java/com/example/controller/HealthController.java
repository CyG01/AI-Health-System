package com.example.controller;

import com.example.annotation.NoRepeatSubmit;
import com.example.common.Result;
import com.example.dto.HealthCreateDTO;
import com.example.dto.HealthUpdateDTO;
import com.example.service.HealthService;
import com.example.vo.HealthAssessmentVO;
import com.example.vo.HealthHistoryVO;
import com.example.vo.HealthProgressVO;
import com.example.vo.HealthRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "健康档案管理")
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @Autowired
    private HealthService healthService;

    @NoRepeatSubmit
    @Operation(summary = "创建健康档案")
    @PostMapping("/create")
    public Result<HealthRecordVO> create(@Validated @RequestBody HealthCreateDTO dto,
                                         @RequestAttribute("userId") Long userId) {
        return Result.success(healthService.createHealthRecord(userId, dto));
    }

    @NoRepeatSubmit
    @Operation(summary = "更新健康档案")
    @PutMapping("/update")
    public Result<HealthRecordVO> update(@Validated @RequestBody HealthUpdateDTO dto,
                                         @RequestAttribute("userId") Long userId) {
        return Result.success(healthService.updateHealthRecord(userId, dto));
    }

    @Operation(summary = "查询最新健康档案")
    @GetMapping("/get-latest")
    public Result<HealthRecordVO> getLatest(@RequestAttribute("userId") Long userId) {
        return Result.success(healthService.getLatestHealthRecord(userId));
    }

    @Operation(summary = "查询健康档案历史版本")
    @GetMapping("/history")
    public Result<List<HealthHistoryVO>> history(@RequestAttribute("userId") Long userId,
                                                  @RequestParam(defaultValue = "1") Integer page,
                                                  @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(healthService.getHealthHistory(userId, page, size));
    }

    @Operation(summary = "健康风险评估")
    @GetMapping("/assessment")
    public Result<HealthAssessmentVO> assessment(@RequestAttribute("userId") Long userId) {
        return Result.success(healthService.getHealthAssessment(userId));
    }

    @Operation(summary = "体重目标进度")
    @GetMapping("/progress")
    public Result<HealthProgressVO> progress(@RequestAttribute("userId") Long userId) {
        return Result.success(healthService.getHealthProgress(userId));
    }
}
