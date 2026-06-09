package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.annotation.NoRepeatSubmit;
import com.example.common.Result;
import com.example.dto.ExerciseRecordSubmitDTO;
import com.example.service.ExerciseService;
import com.example.vo.ExerciseItemVO;
import com.example.vo.ExerciseRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
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

@Tag(name = "运动管理")
@RestController
@RequestMapping("/api/exercise")
public class ExerciseController {

    @Autowired
    private ExerciseService exerciseService;

    @Operation(summary = "运动项目列表")
    @GetMapping("/items")
    public Result<List<ExerciseItemVO>> listItems(
            @RequestParam(required = false) String type) {
        if (type != null && !type.isBlank()) {
            return Result.success(exerciseService.listItemsByType(type));
        }
        return Result.success(exerciseService.listActiveItems());
    }

    @NoRepeatSubmit
    @Operation(summary = "提交运动记录")
    @PostMapping("/record")
    public Result<ExerciseRecordVO> submitRecord(
            @Validated @RequestBody ExerciseRecordSubmitDTO dto,
            @RequestAttribute("userId") Long userId) {
        return Result.success(exerciseService.submitRecord(userId, dto));
    }

    @Operation(summary = "查询某次打卡的运动记录")
    @GetMapping("/record/checkin/{checkinId}")
    public Result<List<ExerciseRecordVO>> getRecordsByCheckinId(@PathVariable Long checkinId) {
        return Result.success(exerciseService.getRecordsByCheckinId(checkinId));
    }

    @Operation(summary = "查询用户运动记录")
    @GetMapping("/record/user")
    public Result<List<ExerciseRecordVO>> getRecordsByUserId(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "30") Integer limit) {
        return Result.success(exerciseService.getRecordsByUserId(userId, limit));
    }

    @Operation(summary = "分页查询运动记录")
    @GetMapping("/records")
    public Result<Page<ExerciseRecordVO>> getRecordsPage(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(exerciseService.getRecordsPage(userId, page, size));
    }

    @Operation(summary = "按日期查询运动记录")
    @GetMapping("/records/{date}")
    public Result<Page<ExerciseRecordVO>> getRecordsByDate(
            @RequestAttribute("userId") Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(exerciseService.getRecordsByDate(userId, date, page, size));
    }
}