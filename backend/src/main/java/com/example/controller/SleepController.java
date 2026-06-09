package com.example.controller;

import com.example.common.Result;
import com.example.dto.SleepRecordSubmitDTO;
import com.example.service.SleepService;
import com.example.vo.SleepRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Tag(name = "睡眠管理")
@RestController
@RequestMapping("/api/sleep")
public class SleepController {

    @Autowired
    private SleepService sleepService;

    @Operation(summary = "提交睡眠记录")
    @PostMapping("/submit")
    public Result<SleepRecordVO> submit(@Validated @RequestBody SleepRecordSubmitDTO dto,
                                        @RequestAttribute("userId") Long userId) {
        return Result.success(sleepService.submit(userId, dto));
    }

    @Operation(summary = "获取今日睡眠记录")
    @GetMapping("/today")
    public Result<SleepRecordVO> today(@RequestAttribute("userId") Long userId) {
        return Result.success(sleepService.getByDate(userId, LocalDate.now()));
    }

    @Operation(summary = "获取近N天睡眠记录")
    @GetMapping("/list")
    public Result<List<SleepRecordVO>> list(@RequestParam(defaultValue = "30") int days,
                                             @RequestAttribute("userId") Long userId) {
        return Result.success(sleepService.getList(userId, days));
    }

    @Operation(summary = "AI睡眠分析")
    @GetMapping("/analyze")
    public Result<Map<String, String>> analyze(@RequestAttribute("userId") Long userId) {
        String analysis = sleepService.analyzeSleep(userId);
        return Result.success(Map.of("analysis", analysis));
    }
}