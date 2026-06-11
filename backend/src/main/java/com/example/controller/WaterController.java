package com.example.controller;

import com.example.annotation.NoRepeatSubmit;
import com.example.annotation.RateLimit;
import com.example.common.Result;
import com.example.dto.WaterRecordSubmitDTO;
import com.example.service.WaterService;
import com.example.vo.WaterRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "饮水管理")
@RestController
@RequestMapping("/api/water")
public class WaterController {

    @Autowired
    private WaterService waterService;

    @RateLimit(time = 60, count = 10)
    @Operation(summary = "提交饮水记录")
    @NoRepeatSubmit
    @PostMapping("/submit")
    public Result<WaterRecordVO> submit(@Validated @RequestBody WaterRecordSubmitDTO dto,
                                        @RequestAttribute("userId") Long userId) {
        return Result.success(waterService.submit(userId, dto));
    }

    @Operation(summary = "获取今日饮水记录")
    @GetMapping("/today")
    public Result<WaterRecordVO> today(@RequestAttribute("userId") Long userId) {
        return Result.success(waterService.getToday(userId));
    }

    @Operation(summary = "获取近N天饮水记录")
    @GetMapping("/list")
    public Result<List<WaterRecordVO>> list(@RequestParam(defaultValue = "7") int days,
                                             @RequestAttribute("userId") Long userId) {
        return Result.success(waterService.getList(userId, days));
    }

    @Operation(summary = "获取指定日期饮水总量")
    @GetMapping("/total")
    public Result<Map<String, Object>> dailyTotal(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestAttribute("userId") Long userId) {
        int total = waterService.getDailyTotal(userId, date);
        return Result.success(Map.of("date", date, "totalMl", total));
    }

    @RateLimit(time = 60, count = 10)
    @NoRepeatSubmit
    @Operation(summary = "删除饮水记录")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id,
                               @RequestAttribute("userId") Long userId) {
        waterService.delete(userId, id);
        return Result.success();
    }
}