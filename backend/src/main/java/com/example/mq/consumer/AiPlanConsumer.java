package com.example.mq.consumer;

import com.example.dto.AiTaskMessage;
import com.example.mq.AiTaskMessageProducer;
import com.example.mq.MqTopics;
import com.example.mq.retry.ExponentialBackoffRetryStrategy;
import com.example.service.impl.TaskStatusServiceImpl;
import com.example.service.impl.AiPlanServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * AI 计划生成消费者 — 消费 ai-plan-generate Topic。
 * 并发度：4
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true", matchIfMissing = false)
@RocketMQMessageListener(
        topic = MqTopics.AI_PLAN_GENERATE,
        consumerGroup = MqTopics.PLAN_CONSUMER_GROUP,
        consumeThreadMax = 20
)
public class AiPlanConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final TaskStatusServiceImpl taskStatusService;
    private final ExponentialBackoffRetryStrategy retryStrategy;
    private final AiTaskMessageProducer producer;
    private final AiPlanServiceImpl aiPlanService;

    public AiPlanConsumer(ObjectMapper objectMapper,
                           TaskStatusServiceImpl taskStatusService,
                           ExponentialBackoffRetryStrategy retryStrategy,
                           AiTaskMessageProducer producer,
                           AiPlanServiceImpl aiPlanService) {
        this.objectMapper = objectMapper;
        this.taskStatusService = taskStatusService;
        this.retryStrategy = retryStrategy;
        this.producer = producer;
        this.aiPlanService = aiPlanService;
    }

    @Override
    public void onMessage(String messageBody) {
        AiTaskMessage message;
        try {
            message = objectMapper.readValue(messageBody, AiTaskMessage.class);
        } catch (Exception e) {
            log.error("消息解析失败 body={}", messageBody, e);
            return;
        }

        String taskId = message.getTaskId();
        log.info("开始处理AI计划生成任务 taskId={} userId={}", taskId, message.getUserId());

        if (taskStatusService.isDuplicate(taskId)) {
            log.warn("重复消息，跳过 taskId={}", taskId);
            return;
        }

        taskStatusService.updateTaskStatus(taskId, "PROCESSING", null, null);

        try {
            Object result = aiPlanService.generatePlanSync(message);
            taskStatusService.updateTaskStatus(taskId, "COMPLETED", result, null);
            log.info("AI计划生成完成 taskId={}", taskId);
        } catch (Exception e) {
            log.error("AI计划生成失败 taskId={} retry={}", taskId, message.getRetryCount(), e);
            handleFailure(message, e);
        }
    }

    private void handleFailure(AiTaskMessage message, Exception e) {
        int retryCount = message.getRetryCount();
        if (retryStrategy.shouldRetry(retryCount)) {
            long delayMs = retryStrategy.getDelayMs(retryCount + 1);
            message.setRetryCount(retryCount + 1);
            log.info("AI计划生成重试 taskId={} retry={}/{} delayMs={}",
                    message.getTaskId(), message.getRetryCount(), retryStrategy.getMaxAttempts(), delayMs);

            new Thread(() -> {
                try {
                    Thread.sleep(delayMs);
                    producer.send(MqTopics.AI_PLAN_GENERATE, message);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }, "retry-" + message.getTaskId()).start();
        } else {
            log.error("AI计划生成重试耗尽 taskId={}，转入死信队列", message.getTaskId());
            taskStatusService.updateTaskStatus(message.getTaskId(), "FAILED", null, e.getMessage());
            producer.sendToDlq(MqTopics.AI_PLAN_GENERATE_DLQ, message);
        }
    }
}