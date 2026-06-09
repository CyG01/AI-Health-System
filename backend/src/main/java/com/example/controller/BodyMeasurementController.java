package com.example.controller;

import com.example.common.Result;
import com.example.dto.BodyMeasurementSubmitDTO;
import com.example.service.BodyMeasurementService;
import com.example.vo.BodyMeasurementVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "身体围度测量")
@RestController
@RequestMapping("/api/body-measurement")
public class BodyMeasurementController {

    @Autowired
    private BodyMeasurementService bodyMeasurementService;

    @Operation(summary = "提交围度记录")
    @PostMapping("/submit")
    public Result<BodyMeasurementVO> submit(@Validated @RequestBody BodyMeasurementSubmitDTO dto,
                                            @RequestAttribute("userId") Long userId) {
        return Result.success(bodyMeasurementService.submit(userId, dto));
    }

    @Operation(summary = "获取最新围度记录")
    @GetMapping("/latest")
    public Result<BodyMeasurementVO> latest(@RequestAttribute("userId") Long userId) {
        return Result.success(bodyMeasurementService.getLatest(userId));
    }

    @Operation(summary = "获取围度历史")
    @GetMapping("/history")
    public Result<List<BodyMeasurementVO>> history(@RequestParam(defaultValue = "10") int limit,
                                                    @RequestAttribute("userId") Long userId) {
        return Result.success(bodyMeasurementService.getHistory(userId, limit));
    }

    @Operation(summary = "获取围度趋势")
    @GetMapping("/trend")
    public Result<List<BodyMeasurementVO>> trend(@RequestParam(defaultValue = "6") int months,
                                                  @RequestAttribute("userId") Long userId) {
        return Result.success(bodyMeasurementService.getTrend(userId, months));
    }
}