package com.example.controller;

import com.example.common.Result;
import com.example.dto.TaskStatusVO;
import com.example.service.TaskStatusService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * 任务状态查询控制器 — 前端轮询查询 AI 任务处理进度。
 */
@Tag(name = "AI 任务状态查询", description = "前端轮询查询 AI 异步任务的处理进度")
@RestController
@RequestMapping("/api/task")
public class TaskStatusController {

    private final TaskStatusService taskStatusService;

    public TaskStatusController(TaskStatusService taskStatusService) {
        this.taskStatusService = taskStatusService;
    }

    @Operation(summary = "查询任务状态", description = "根据 taskId 查询 AI 任务的处理状态。前端建议每 2-3 秒轮询一次。")
    @GetMapping("/status/{taskId}")
    public Result<TaskStatusVO> getTaskStatus(
            @Parameter(description = "任务ID", required = true)
            @PathVariable String taskId) {
        TaskStatusVO status = taskStatusService.getTaskStatus(taskId);
        return Result.success(status);
    }
}