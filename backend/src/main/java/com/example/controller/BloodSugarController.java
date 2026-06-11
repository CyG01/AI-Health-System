package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.annotation.NoRepeatSubmit;
import com.example.annotation.RateLimit;
import com.example.common.Result;
import com.example.dto.BloodSugarSubmitDTO;
import com.example.service.BloodSugarService;
import com.example.vo.BloodSugarVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "血糖监测")
@RestController
@RequestMapping("/api/blood-sugar")
public class BloodSugarController {

    @Autowired
    private BloodSugarService bloodSugarService;

    @RateLimit(time = 60, count = 5)
    @NoRepeatSubmit
    @Operation(summary = "提交血糖记录")
    @PostMapping("/record")
    public Result<BloodSugarVO> submitRecord(
            @Validated @RequestBody BloodSugarSubmitDTO dto,
            @RequestAttribute("userId") Long userId) {
        return Result.success(bloodSugarService.submitRecord(userId, dto));
    }

    @Operation(summary = "分页查询血糖记录")
    @GetMapping("/records")
    public Result<Page<BloodSugarVO>> getRecordsPage(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(bloodSugarService.getRecordsPage(userId, page, size));
    }

    @Operation(summary = "按日期查询血糖记录")
    @GetMapping("/records/{date}")
    public Result<List<BloodSugarVO>> getRecordsByDate(
            @RequestAttribute("userId") Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return Result.success(bloodSugarService.getRecordsByDate(userId, date));
    }

    @Operation(summary = "血糖趋势（近N天）")
    @GetMapping("/trend")
    public Result<List<BloodSugarVO>> getTrend(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "14") int days) {
        return Result.success(bloodSugarService.getTrend(userId, days));
    }

    @RateLimit(time = 60, count = 5)
    @NoRepeatSubmit
    @Operation(summary = "删除血糖记录")
    @DeleteMapping("/record/{id}")
    public Result<Void> deleteRecord(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        bloodSugarService.deleteRecord(userId, id);
        return Result.success();
    }
}