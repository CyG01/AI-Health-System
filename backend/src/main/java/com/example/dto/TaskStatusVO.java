package com.example.dto;

import java.time.LocalDateTime;

/**
 * 任务状态响应 VO — 前端轮询查询任务处理进度。
 */
public class TaskStatusVO {

    /** 任务ID */
    private String taskId;

    /** 用户ID */
    private Long userId;

    /** 任务类型 */
    private String taskType;

    /** 状态：PENDING / PROCESSING / COMPLETED / FAILED / TIMEOUT */
    private String status;

    /** 进度百分比 0-100 */
    private Integer progress;

    /** 结果数据（COMPLETED 时有值） */
    private Object result;

    /** 错误信息（FAILED 时有值） */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 完成时间 */
    private LocalDateTime completedAt;

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getProgress() { return progress; }
    public void setProgress(Integer progress) { this.progress = progress; }

    public Object getResult() { return result; }
    public void setResult(Object result) { this.result = result; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}