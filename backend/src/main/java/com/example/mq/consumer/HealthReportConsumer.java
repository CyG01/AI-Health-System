package com.example.mq.consumer;

import com.example.dto.AiTaskMessage;
import com.example.mq.AiTaskMessageProducer;
import com.example.mq.MqTopics;
import com.example.mq.retry.ExponentialBackoffRetryStrategy;
import com.example.service.impl.TaskStatusServiceImpl;
import com.example.service.impl.HealthReportServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 健康报告生成消费者 — 消费 health-report-generate Topic。
 * 并发度：4
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "rocketmq.enabled", havingValue = "true", matchIfMissing = false)
@RocketMQMessageListener(
        topic = MqTopics.HEALTH_REPORT_GENERATE,
        consumerGroup = MqTopics.REPORT_CONSUMER_GROUP,
        consumeThreadMax = 20
)
public class HealthReportConsumer implements RocketMQListener<String> {

    private final ObjectMapper objectMapper;
    private final TaskStatusServiceImpl taskStatusService;
    private final ExponentialBackoffRetryStrategy retryStrategy;
    private final AiTaskMessageProducer producer;
    private final HealthReportServiceImpl healthReportService;

    public HealthReportConsumer(ObjectMapper objectMapper,
                                 TaskStatusServiceImpl taskStatusService,
                                 ExponentialBackoffRetryStrategy retryStrategy,
                                 AiTaskMessageProducer producer,
                                 HealthReportServiceImpl healthReportService) {
        this.objectMapper = objectMapper;
        this.taskStatusService = taskStatusService;
        this.retryStrategy = retryStrategy;
        this.producer = producer;
        this.healthReportService = healthReportService;
    }

    @Override
    public void onMessage(String messageBody) {
        AiTaskMessage message = null;
        try {
            message = objectMapper.readValue(messageBody, AiTaskMessage.class);
        } catch (Exception e) {
            log.error("消息解析失败 body={}", messageBody, e);
            return;
        }

        String taskId = message.getTaskId();
        log.info("开始处理健康报告生成任务 taskId={} userId={}", taskId, message.getUserId());

        // 幂等性校验
        if (taskStatusService.isDuplicate(taskId)) {
            log.warn("重复消息，跳过 taskId={}", taskId);
            return;
        }

        // 更新状态为处理中
        taskStatusService.updateTaskStatus(taskId, "PROCESSING", null, null);

        try {
            // 执行健康报告生成
            Object result = healthReportService.generateReportSync(message);
            taskStatusService.updateTaskStatus(taskId, "COMPLETED", result, null);
            log.info("健康报告生成完成 taskId={}", taskId);
        } catch (Exception e) {
            log.error("健康报告生成失败 taskId={} retry={}", taskId, message.getRetryCount(), e);
            handleFailure(message, e);
        }
    }

    private void handleFailure(AiTaskMessage message, Exception e) {
        int retryCount = message.getRetryCount();
        if (retryStrategy.shouldRetry(retryCount)) {
            // 指数退避重试
            long delayMs = retryStrategy.getDelayMs(retryCount + 1);
            message.setRetryCount(retryCount + 1);
            log.info("健康报告生成重试 taskId={} retry={}/{} delayMs={}",
                    message.getTaskId(), message.getRetryCount(), retryStrategy.getMaxAttempts(), delayMs);

            // 延迟重试（通过重新发送到原 Topic 实现）
            new Thread(() -> {
                try {
                    Thread.sleep(delayMs);
                    producer.send(MqTopics.HEALTH_REPORT_GENERATE, message);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }, "retry-" + message.getTaskId()).start();
        } else {
            // 达到最大重试次数，转入死信队列
            log.error("健康报告生成重试耗尽 taskId={}，转入死信队列", message.getTaskId());
            taskStatusService.updateTaskStatus(message.getTaskId(), "FAILED", null, e.getMessage());
            producer.sendToDlq(MqTopics.HEALTH_REPORT_GENERATE_DLQ, message);
        }
    }
}