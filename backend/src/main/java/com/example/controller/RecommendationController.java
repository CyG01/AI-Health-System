package com.example.controller;

import com.example.common.Result;
import com.example.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "个性化智能推荐")
@RestController
@RequestMapping("/api/recommend")
public class RecommendationController {

    @Autowired
    private RecommendationService recommendationService;

    @Operation(summary = "获取个性化推荐")
    @GetMapping("/personalized")
    public Result<Map<String, Object>> personalized(@RequestAttribute("userId") Long userId) {
        return Result.success(recommendationService.getRecommendations(userId));
    }
}