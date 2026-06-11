package com.example.mq.consumer;

import com.example.dto.AiTaskMessage;
import com.example.mq.AiTaskMessageProducer;
import com.example.mq.MqTopics;
import com.example.mq.retry.ExponentialBackoffRetryStrategy;
import com.example.service.impl.TaskStatusServiceImpl;
import com.example.service.impl.KnowledgeServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

/**
 * 知识库索引构建消费者 — 消费 knowledge-index-build Topic。
 * 并发度：2
 */
@Slf4j
@Component
@RocketMQMessageListener(
        topic = MqTopics.KNOWLEDGE_INDEX_BUILD,
        consumerGroup = MqTopics.KNOWLEDGE_CONSUMER_GROUP,
        consumeThreadMax = 2
)
public class KnowledgeIndexConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final TaskStatusServiceImpl taskStatusService;
    private final ExponentialBackoffRetryStrategy retryStrategy;
    private final AiTaskMessageProducer producer;
    private final KnowledgeServiceImpl knowledgeService;

    public KnowledgeIndexConsumer(ObjectMapper objectMapper,
                                   TaskStatusServiceImpl taskStatusService,
                                   ExponentialBackoffRetryStrategy retryStrategy,
                                   AiTaskMessageProducer producer,
                                   KnowledgeServiceImpl knowledgeService) {
        this.objectMapper = objectMapper;
        this.taskStatusService = taskStatusService;
        this.retryStrategy = retryStrategy;
        this.producer = producer;
        this.knowledgeService = knowledgeService;
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
        log.info("开始处理知识库索引构建任务 taskId={}", taskId);

        if (taskStatusService.isDuplicate(taskId)) {
            log.warn("重复消息，跳过 taskId={}", taskId);
            return;
        }

        taskStatusService.updateTaskStatus(taskId, "PROCESSING", null, null);

        try {
            knowledgeService.importDocumentSync(message);
            taskStatusService.updateTaskStatus(taskId, "COMPLETED", null, null);
            log.info("知识库索引构建完成 taskId={}", taskId);
        } catch (Exception e) {
            log.error("知识库索引构建失败 taskId={} retry={}", taskId, message.getRetryCount(), e);
            handleFailure(message, e);
        }
    }

    private void handleFailure(AiTaskMessage message, Exception e) {
        int retryCount = message.getRetryCount();
        if (retryStrategy.shouldRetry(retryCount)) {
            long delayMs = retryStrategy.getDelayMs(retryCount + 1);
            message.setRetryCount(retryCount + 1);
            log.info("知识库索引构建重试 taskId={} retry={}/{} delayMs={}",
                    message.getTaskId(), message.getRetryCount(), retryStrategy.getMaxAttempts(), delayMs);

            new Thread(() -> {
                try {
                    Thread.sleep(delayMs);
                    producer.send(MqTopics.KNOWLEDGE_INDEX_BUILD, message);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }, "retry-" + message.getTaskId()).start();
        } else {
            log.error("知识库索引构建重试耗尽 taskId={}，转入死信队列", message.getTaskId());
            taskStatusService.updateTaskStatus(message.getTaskId(), "FAILED", null, e.getMessage());
            producer.sendToDlq(MqTopics.KNOWLEDGE_INDEX_BUILD_DLQ, message);
        }
    }
}