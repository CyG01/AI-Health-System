package com.example.mq.consumer;

import com.example.dto.AiTaskMessage;
import com.example.mq.AiTaskMessageProducer;
import com.example.mq.MqTopics;
import com.example.mq.retry.ExponentialBackoffRetryStrategy;
import com.example.service.impl.TaskStatusServiceImpl;
import com.example.service.impl.ChatServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * LLM 对话响应消费者 — 消费 llm-chat-response Topic。
 * 并发度：8
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true", matchIfMissing = false)
@RocketMQMessageListener(
        topic = MqTopics.LLM_CHAT_RESPONSE,
        consumerGroup = MqTopics.CHAT_CONSUMER_GROUP,
        consumeThreadMax = 20
)
public class ChatResponseConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final TaskStatusServiceImpl taskStatusService;
    private final ExponentialBackoffRetryStrategy retryStrategy;
    private final AiTaskMessageProducer producer;
    private final ChatServiceImpl chatService;

    public ChatResponseConsumer(ObjectMapper objectMapper,
                                 TaskStatusServiceImpl taskStatusService,
                                 ExponentialBackoffRetryStrategy retryStrategy,
                                 AiTaskMessageProducer producer,
                                 ChatServiceImpl chatService) {
        this.objectMapper = objectMapper;
        this.taskStatusService = taskStatusService;
        this.retryStrategy = retryStrategy;
        this.producer = producer;
        this.chatService = chatService;
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
        log.info("开始处理LLM对话响应任务 taskId={} userId={}", taskId, message.getUserId());

        if (taskStatusService.isDuplicate(taskId)) {
            log.warn("重复消息，跳过 taskId={}", taskId);
            return;
        }

        taskStatusService.updateTaskStatus(taskId, "PROCESSING", null, null);

        try {
            Object result = chatService.chatSync(message);
            taskStatusService.updateTaskStatus(taskId, "COMPLETED", result, null);
            log.info("LLM对话响应完成 taskId={}", taskId);
        } catch (Exception e) {
            log.error("LLM对话响应失败 taskId={} retry={}", taskId, message.getRetryCount(), e);
            handleFailure(message, e);
        }
    }

    private void handleFailure(AiTaskMessage message, Exception e) {
        int retryCount = message.getRetryCount();
        if (retryStrategy.shouldRetry(retryCount)) {
            long delayMs = retryStrategy.getDelayMs(retryCount + 1);
            message.setRetryCount(retryCount + 1);
            log.info("LLM对话响应重试 taskId={} retry={}/{} delayMs={}",
                    message.getTaskId(), message.getRetryCount(), retryStrategy.getMaxAttempts(), delayMs);

            new Thread(() -> {
                try {
                    Thread.sleep(delayMs);
                    producer.send(MqTopics.LLM_CHAT_RESPONSE, message);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }, "retry-" + message.getTaskId()).start();
        } else {
            log.error("LLM对话响应重试耗尽 taskId={}，转入死信队列", message.getTaskId());
            taskStatusService.updateTaskStatus(message.getTaskId(), "FAILED", null, e.getMessage());
            producer.sendToDlq(MqTopics.LLM_CHAT_RESPONSE_DLQ, message);
        }
    }
}