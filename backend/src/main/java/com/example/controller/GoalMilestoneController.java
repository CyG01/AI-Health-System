package com.example.controller;

import com.example.annotation.NoRepeatSubmit;
import com.example.annotation.RateLimit;
import com.example.common.Result;
import com.example.dto.GoalMilestoneDTO;
import com.example.service.GoalMilestoneService;
import com.example.vo.GoalMilestoneVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "目标里程碑")
@RestController
@RequestMapping("/api/goal")
public class GoalMilestoneController {

    @Autowired
    private GoalMilestoneService goalMilestoneService;

    @RateLimit(time = 60, count = 5)
    @NoRepeatSubmit
    @Operation(summary = "创建目标")
    @PostMapping("/create")
    public Result<GoalMilestoneVO> create(@Validated @RequestBody GoalMilestoneDTO dto,
                                           @RequestAttribute("userId") Long userId) {
        return Result.success(goalMilestoneService.create(userId, dto));
    }

    @RateLimit(time = 60, count = 5)
    @NoRepeatSubmit
    @Operation(summary = "更新目标")
    @PutMapping("/update")
    public Result<GoalMilestoneVO> update(@Validated @RequestBody GoalMilestoneDTO dto,
                                           @RequestAttribute("userId") Long userId) {
        return Result.success(goalMilestoneService.update(userId, dto));
    }

    @RateLimit(time = 60, count = 5)
    @NoRepeatSubmit
    @Operation(summary = "删除目标")
    @DeleteMapping("/{goalId}")
    public Result<Void> delete(@PathVariable Long goalId,
                                @RequestAttribute("userId") Long userId) {
        goalMilestoneService.delete(userId, goalId);
        return Result.success();
    }

    @Operation(summary = "获取目标列表")
    @GetMapping("/list")
    public Result<List<GoalMilestoneVO>> list(@RequestAttribute("userId") Long userId) {
        return Result.success(goalMilestoneService.list(userId));
    }

    @Operation(summary = "获取目标详情")
    @GetMapping("/{goalId}")
    public Result<GoalMilestoneVO> getById(@PathVariable Long goalId,
                                            @RequestAttribute("userId") Long userId) {
        return Result.success(goalMilestoneService.getById(userId, goalId));
    }

    @RateLimit(time = 60, count = 5)
    @Operation(summary = "更新目标状态 (完成/放弃)")
    @PutMapping("/{goalId}/status")
    public Result<GoalMilestoneVO> updateStatus(@PathVariable Long goalId,
                                                 @RequestParam Integer status,
                                                 @RequestAttribute("userId") Long userId) {
        return Result.success(goalMilestoneService.updateStatus(userId, goalId, status));
    }
}