package com.example.config;

import org.apache.rocketmq.spring.support.RocketMQMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RocketMQ 配置 — Producer / Consumer Bean 定义。
 */
@Configuration
public class RocketMQConfig {

    /**
     * 消息转换器。RocketMQMessageConverter 内部已默认集成 Jackson JSON 序列化，
     * 无需手动设置 CompositeMessageConverter。
     */
    @Bean
    public RocketMQMessageConverter rocketMQMessageConverter() {
        return new RocketMQMessageConverter();
    }
}