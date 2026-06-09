package com.example.controller;

import com.example.common.Result;
import com.example.dto.WaterRecordSubmitDTO;
import com.example.service.WaterService;
import com.example.vo.WaterRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "饮水管理")
@RestController
@RequestMapping("/api/water")
public class WaterController {

    @Autowired
    private WaterService waterService;

    @Operation(summary = "提交饮水记录")
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
}