package com.example.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.annotation.NoRepeatSubmit;
import com.example.annotation.RateLimit;
import com.example.common.Result;
import com.example.dto.DietRecordSubmitDTO;
import com.example.service.FoodService;
import com.example.vo.DietRecordVO;
import com.example.vo.FoodItemVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "饮食管理")
@RestController
@RequestMapping("/api/food")
public class FoodController {

    @Autowired
    private FoodService foodService;

    @Operation(summary = "食物项目列表")
    @GetMapping("/items")
    public Result<List<FoodItemVO>> listItems(
            @RequestParam(required = false) String category) {
        if (category != null && !category.isBlank()) {
            return Result.success(foodService.listItemsByCategory(category));
        }
        return Result.success(foodService.listActiveItems());
    }

    @RateLimit(time = 60, count = 10)
    @NoRepeatSubmit
    @Operation(summary = "提交饮食记录")
    @PostMapping("/record")
    public Result<DietRecordVO> submitRecord(
            @Validated @RequestBody DietRecordSubmitDTO dto,
            @RequestAttribute("userId") Long userId) {
        return Result.success(foodService.submitRecord(userId, dto));
    }

    @Operation(summary = "查询某次打卡的饮食记录")
    @GetMapping("/record/checkin/{checkinId}")
    public Result<List<DietRecordVO>> getRecordsByCheckinId(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long checkinId) {
        return Result.success(foodService.getRecordsByCheckinId(userId, checkinId));
    }

    @Operation(summary = "查询用户饮食记录")
    @GetMapping("/record/user")
    public Result<List<DietRecordVO>> getRecordsByUserId(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "30") Integer limit) {
        return Result.success(foodService.getRecordsByUserId(userId, limit));
    }

    @Operation(summary = "分页查询饮食记录")
    @GetMapping("/records")
    public Result<Page<DietRecordVO>> getRecordsPage(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(foodService.getRecordsPage(userId, page, size));
    }

    @Operation(summary = "按日期查询饮食记录")
    @GetMapping("/records/{date}")
    public Result<Page<DietRecordVO>> getRecordsByDate(
            @RequestAttribute("userId") Long userId,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        return Result.success(foodService.getRecordsByDate(userId, date, page, size));
    }

    @RateLimit(time = 60, count = 10)
    @NoRepeatSubmit
    @Operation(summary = "更新饮食记录")
    @PutMapping("/record/{id}")
    public Result<DietRecordVO> updateRecord(
            @PathVariable Long id,
            @Validated @RequestBody DietRecordSubmitDTO dto,
            @RequestAttribute("userId") Long userId) {
        return Result.success(foodService.updateRecord(userId, id, dto));
    }

    @RateLimit(time = 60, count = 10)
    @NoRepeatSubmit
    @Operation(summary = "删除饮食记录")
    @DeleteMapping("/record/{id}")
    public Result<Void> deleteRecord(
            @PathVariable Long id,
            @RequestAttribute("userId") Long userId) {
        foodService.deleteRecord(userId, id);
        return Result.success();
    }

    @Operation(summary = "查询用户常用食物")
    @GetMapping("/items/frequent")
    public Result<List<FoodItemVO>> getFrequentItems(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "8") int limit) {
        return Result.success(foodService.getFrequentItems(userId, limit));
    }

    @Operation(summary = "文字快捷录入食物")
    @GetMapping("/items/parse")
    public Result<FoodItemVO> parseFoodText(
            @RequestAttribute("userId") Long userId,
            @RequestParam String text) {
        FoodItemVO result = foodService.parseFoodText(userId, text);
        if (result == null) {
            return Result.error(404, "未匹配到对应食物，请尝试更具体的名称");
        }
        return Result.success(result);
    }
}