package com.example.controller;

import com.example.common.Result;
import com.example.entity.HealthEventTimeline;
import com.example.entity.UserMemory;
import com.example.service.UserMemoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 用户记忆控制器
 * 三层记忆架构：瞬时记忆、核心画像、时间线线索
 */
@Tag(name = "用户记忆管理", description = "AI长程记忆、健康事件时间线等功能")
@RestController
@RequestMapping("/api/v1/memory")
@RequiredArgsConstructor
public class MemoryController {

    private final UserMemoryService userMemoryService;

    @Operation(summary = "获取用户核心画像记忆")
    @GetMapping("/profile")
    public Result<List<UserMemory>> getProfileMemories(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<UserMemory> memories = userMemoryService.getProfileMemories(userId);
        return Result.success(memories);
    }

    @Operation(summary = "获取用户时间线记忆（带时间衰减排序）")
    @GetMapping("/timeline")
    public Result<List<UserMemory>> getTimelineMemories(
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<UserMemory> memories = userMemoryService.getTimelineMemoriesWithDecay(userId, limit);
        return Result.success(memories);
    }

    @Operation(summary = "添加用户记忆")
    @PostMapping("/add")
    public Result<UserMemory> addMemory(
            @RequestParam String memoryType,
            @RequestParam(defaultValue = "TIMELINE") String memoryLayer,
            @RequestParam String content,
            @RequestParam(defaultValue = "5") Integer importance,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String tags,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserMemory memory = userMemoryService.addMemory(
                userId, memoryType, memoryLayer, content, importance, source, tags);
        return Result.success(memory);
    }

    @Operation(summary = "获取AI记忆上下文（用于对话）")
    @GetMapping("/context")
    public Result<String> getMemoryContext(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        String context = userMemoryService.buildMemoryContext(userId);
        return Result.success(context);
    }

    @Operation(summary = "添加健康事件到时间线")
    @PostMapping("/health-event")
    public Result<HealthEventTimeline> addHealthEvent(
            @RequestParam String eventType,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) LocalDate eventDate,
            @RequestParam(defaultValue = "MODERATE") String severity,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        HealthEventTimeline event = userMemoryService.addHealthEvent(
                userId, eventType, title, description, eventDate, severity);
        return Result.success(event);
    }

    @Operation(summary = "获取健康事件时间线")
    @GetMapping("/health-events")
    public Result<List<HealthEventTimeline>> getHealthEventTimeline(
            @RequestParam(defaultValue = "30") int days,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        List<HealthEventTimeline> events = userMemoryService.getHealthEventTimeline(userId, days);
        return Result.success(events);
    }

    @Operation(summary = "清理过期瞬时记忆")
    @PostMapping("/cleanup-session")
    public Result<Integer> cleanupSessionMemories(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        int count = userMemoryService.cleanupSessionMemories(userId);
        return Result.success(count);
    }

    @Operation(summary = "混合检索记忆（语义+时间衰减）")
    @PostMapping("/search")
    public Result<List<Map<String, Object>>> hybridSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int topK,
            HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        // 简化版：实际项目中需要传入向量
        List<Map<String, Object>> results = userMemoryService.hybridSearchMemories(
                userId, query, null, topK);
        return Result.success(results);
    }
}
