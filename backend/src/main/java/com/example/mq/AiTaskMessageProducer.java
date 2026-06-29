package com.example.mq;

import com.example.dto.AiTaskMessage;
import com.example.mq.retry.ExponentialBackoffRetryStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * AI 任务消息生产者 — 统一封装 RocketMQ 消息发送。
 */
@Slf4j
@Component
public class AiTaskMessageProducer {

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;
    private final ExponentialBackoffRetryStrategy retryStrategy;

    @Value("${ai-task.mq.enabled:true}")
    private boolean mqEnabled;

    public AiTaskMessageProducer(RocketMQTemplate rocketMQTemplate,
                                  ObjectMapper objectMapper,
                                  ExponentialBackoffRetryStrategy retryStrategy) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.objectMapper = objectMapper;
        this.retryStrategy = retryStrategy;
    }

    /**
     * 发送 AI 任务消息到指定 Topic。
     *
     * @param topic   目标 Topic
     * @param message 任务消息
     * @return 发送是否成功
     */
    public boolean send(String topic, AiTaskMessage message) {
        if (!mqEnabled) {
            log.warn("RocketMQ 已禁用，消息未发送 taskId={}", message.getTaskId());
            return false;
        }

        if (message.getTaskId() == null || message.getTaskId().isEmpty()) {
            message.setTaskId(UUID.randomUUID().toString().replace("-", ""));
        }

        try {
            String jsonStr = objectMapper.writeValueAsString(message);
            SendResult result = rocketMQTemplate.syncSend(
                    topic,
                    MessageBuilder.withPayload(jsonStr).build(),
                    3000);
            log.info("MQ 消息发送成功 topic={} taskId={} msgId={}",
                    topic, message.getTaskId(), result.getMsgId());
            return true;
        } catch (Exception e) {
            log.error("MQ 消息发送失败 topic={} taskId={}", topic, message.getTaskId(), e);
            return false;
        }
    }

    /**
     * 发送消息到死信队列。
     */
    public boolean sendToDlq(String dlqTopic, AiTaskMessage message) {
        try {
            String jsonStr = objectMapper.writeValueAsString(message);
            rocketMQTemplate.syncSend(dlqTopic, MessageBuilder.withPayload(jsonStr).build(), 3000);
            log.warn("消息转入死信队列 dlqTopic={} taskId={}", dlqTopic, message.getTaskId());
            return true;
        } catch (Exception e) {
            log.error("死信队列发送失败 dlqTopic={} taskId={}", dlqTopic, message.getTaskId(), e);
            return false;
        }
    }
}