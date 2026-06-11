package com.example.service.impl;

import com.example.dto.TaskStatusVO;
import com.example.service.TaskStatusService;
import com.example.websocket.TaskResultWebSocketHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 任务状态查询服务实现 — 基于 Redis 存储 + WebSocket 推送。
 */
@Slf4j
@Service
public class TaskStatusServiceImpl implements TaskStatusService {

    private static final String TASK_STATUS_PREFIX = "task:status:";
    private static final String TASK_RESULT_PREFIX = "task:result:";
    private static final String TASK_DEDUP_PREFIX = "mq:dedup:";

    private final RedisTemplate<String, Object> redisTemplate;
    private final TaskResultWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;

    @Value("${ai-task.result.ttl-hours:24}")
    private int resultTtlHours;

    public TaskStatusServiceImpl(RedisTemplate<String, Object> redisTemplate,
                                  TaskResultWebSocketHandler webSocketHandler,
                                  ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
    }

    @Override
    public TaskStatusVO getTaskStatus(String taskId) {
        String statusKey = TASK_STATUS_PREFIX + taskId;
        Object cached = redisTemplate.opsForValue().get(statusKey);
        if (cached == null) {
            TaskStatusVO notFound = new TaskStatusVO();
            notFound.setTaskId(taskId);
            notFound.setStatus("NOT_FOUND");
            return notFound;
        }
        return objectMapper.convertValue(cached, TaskStatusVO.class);
    }

    @Override
    public void updateTaskStatus(String taskId, String status, Object result, String errorMessage) {
        String statusKey = TASK_STATUS_PREFIX + taskId;
        TaskStatusVO vo = getTaskStatus(taskId);
        if (vo.getStatus() != null && "COMPLETED".equals(vo.getStatus())) {
            return; // 已处理完成，幂等跳过
        }

        vo.setStatus(status);
        vo.setResult(result);
        vo.setErrorMessage(errorMessage);
        vo.setCompletedAt(LocalDateTime.now());

        redisTemplate.opsForValue().set(statusKey, vo, resultTtlHours, TimeUnit.HOURS);

        // WebSocket 推送
        if (vo.getUserId() != null) {
            webSocketHandler.pushTaskResult(vo.getUserId(), vo);
        }
    }

    @Override
    public void initTaskStatus(String taskId, String taskType) {
        String statusKey = TASK_STATUS_PREFIX + taskId;
        TaskStatusVO vo = new TaskStatusVO();
        vo.setTaskId(taskId);
        vo.setTaskType(taskType);
        vo.setStatus("PENDING");
        vo.setProgress(0);
        vo.setCreatedAt(LocalDateTime.now());
        redisTemplate.opsForValue().set(statusKey, vo, resultTtlHours, TimeUnit.HOURS);
    }

    /**
     * 初始化任务状态（含 userId）。
     */
    public void initTaskStatus(String taskId, String taskType, Long userId) {
        String statusKey = TASK_STATUS_PREFIX + taskId;
        TaskStatusVO vo = new TaskStatusVO();
        vo.setTaskId(taskId);
        vo.setUserId(userId);
        vo.setTaskType(taskType);
        vo.setStatus("PENDING");
        vo.setProgress(0);
        vo.setCreatedAt(LocalDateTime.now());
        redisTemplate.opsForValue().set(statusKey, vo, resultTtlHours, TimeUnit.HOURS);
    }

    /**
     * 幂等性校验：检查 taskId 是否已消费过。
     */
    public boolean isDuplicate(String taskId) {
        String dedupKey = TASK_DEDUP_PREFIX + taskId;
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(dedupKey, "1", resultTtlHours, TimeUnit.HOURS);
        return Boolean.FALSE.equals(locked);
    }

    /**
     * 获取任务状态 VO（包含 userId，用于 WebSocket 推送）。
     */
    public TaskStatusVO getTaskStatusWithUser(String taskId) {
        return getTaskStatus(taskId);
    }
}