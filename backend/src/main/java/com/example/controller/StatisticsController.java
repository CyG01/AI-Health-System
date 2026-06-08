package com.example.controller;

import com.example.common.Result;
import com.example.service.StatisticsService;
import com.example.vo.BmiTrendVO;
import com.example.vo.CalorieTrendVO;
import com.example.vo.CheckinTrendVO;
import com.example.vo.ExerciseTrendVO;
import com.example.vo.ProgressVO;
import com.example.vo.WeightTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "数据统计与分析")
@RestController
@RequestMapping("/api/statistics")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @Operation(summary = "体重变化趋势")
    @GetMapping("/weight")
    public Result<WeightTrendVO> weight(@RequestAttribute("userId") Long userId,
                                        @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(statisticsService.getWeightTrend(userId, days));
    }

    @Operation(summary = "BMI变化趋势")
    @GetMapping("/bmi")
    public Result<BmiTrendVO> bmi(@RequestAttribute("userId") Long userId,
                                  @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(statisticsService.getBmiTrend(userId, days));
    }

    @Operation(summary = "打卡完成率")
    @GetMapping("/checkin")
    public Result<CheckinTrendVO> checkin(@RequestAttribute("userId") Long userId,
                                          @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(statisticsService.getCheckinTrend(userId, days));
    }

    @Operation(summary = "运动完成率统计")
    @GetMapping("/exercise")
    public Result<ExerciseTrendVO> exercise(@RequestAttribute("userId") Long userId,
                                            @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(statisticsService.getExerciseTrend(userId, days));
    }

    @Operation(summary = "热量摄入统计")
    @GetMapping("/calorie")
    public Result<CalorieTrendVO> calorie(@RequestAttribute("userId") Long userId,
                                          @RequestParam(defaultValue = "30") Integer days) {
        return Result.success(statisticsService.getCalorieTrend(userId, days));
    }

    @Operation(summary = "健康目标进度")
    @GetMapping("/progress")
    public Result<ProgressVO> progress(@RequestAttribute("userId") Long userId) {
        return Result.success(statisticsService.getProgress(userId));
    }
}
