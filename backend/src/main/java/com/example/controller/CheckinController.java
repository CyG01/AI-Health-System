package com.example.controller;

import com.example.annotation.NoRepeatSubmit;
import com.example.common.Result;
import com.example.dto.CheckinSubmitDTO;
import com.example.dto.CheckinSupplementDTO;
import com.example.service.CheckinService;
import com.example.vo.CheckinStatsVO;
import com.example.vo.CheckinVO;
import io.swagger.v3.oas.annotations.Operation;

import java.util.List;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "每日健康打卡")
@RestController
@RequestMapping("/api/checkin")
public class CheckinController {

    @Autowired
    private CheckinService checkinService;

    @NoRepeatSubmit
    @Operation(summary = "提交今日打卡")
    @PostMapping("/submit")
    public Result<CheckinVO> submit(@Validated @RequestBody CheckinSubmitDTO dto,
                                    @RequestAttribute("userId") Long userId) {
        return Result.success(checkinService.submitCheckin(userId, dto));
    }

    @Operation(summary = "查询打卡记录列表")
    @GetMapping("/list")
    public Result<List<CheckinVO>> list(@RequestAttribute("userId") Long userId) {
        return Result.success(checkinService.getCheckinList(userId));
    }

    @NoRepeatSubmit
    @Operation(summary = "补卡")
    @PostMapping("/supplement")
    public Result<CheckinVO> supplement(@Validated @RequestBody CheckinSupplementDTO dto,
                                        @RequestAttribute("userId") Long userId) {
        return Result.success(checkinService.supplementCheckin(userId, dto));
    }

    @Operation(summary = "打卡统计")
    @GetMapping("/stats")
    public Result<CheckinStatsVO> stats(@RequestAttribute("userId") Long userId) {
        return Result.success(checkinService.getStats(userId));
    }
}
