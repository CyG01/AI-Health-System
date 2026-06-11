package com.example.service;

import com.example.dto.TaskStatusVO;

/**
 * 任务状态查询服务 — 供前端轮询查询 AI 任务处理进度。
 */
public interface TaskStatusService {

    /**
     * 根据 taskId 查询任务状态。
     */
    TaskStatusVO getTaskStatus(String taskId);

    /**
     * 将任务状态写入缓存（Consumer 完成后调用）。
     */
    void updateTaskStatus(String taskId, String status, Object result, String errorMessage);

    /**
     * 初始化任务状态（Producer 发送后调用）。
     */
    void initTaskStatus(String taskId, String taskType);
}