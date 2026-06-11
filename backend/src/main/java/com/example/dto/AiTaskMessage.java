package com.example.dto;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * AI 任务消息体 — Producer 与 Consumer 之间传递的标准化消息。
 */
public class AiTaskMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 任务唯一标识 */
    private String taskId;

    /** 任务类型：health-report / ai-plan / knowledge-index / chat-response */
    private String taskType;

    /** 用户ID */
    private Long userId;

    /** 业务载荷（JSON 字符串） */
    private String payload;

    /** 业务扩展参数 */
    private Map<String, String> params;

    /** 重试次数 */
    private int retryCount;

    /** 创建时间 */
    private LocalDateTime createdAt;

    public AiTaskMessage() {
        this.createdAt = LocalDateTime.now();
        this.retryCount = 0;
    }

    public String getTaskId() { return taskId; }
    public void setTaskId(String taskId) { this.taskId = taskId; }

    public String getTaskType() { return taskType; }
    public void setTaskType(String taskType) { this.taskType = taskType; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }

    public Map<String, String> getParams() { return params; }
    public void setParams(Map<String, String> params) { this.params = params; }

    public int getRetryCount() { return retryCount; }
    public void setRetryCount(int retryCount) { this.retryCount = retryCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /**
     * 消息去重键（用于幂等性校验）。
     */
    public String dedupKey() {
        return "mq:dedup:" + taskId;
    }
}