package com.example.config;

import org.apache.rocketmq.spring.support.RocketMQMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;

import java.util.List;

/**
 * RocketMQ 配置 — Producer / Consumer Bean 定义。
 */
@Configuration
public class RocketMQConfig {

    /**
     * 确保消息转换器支持 JSON 序列化。
     */
    @Bean
    public RocketMQMessageConverter rocketMQMessageConverter() {
        RocketMQMessageConverter converter = new RocketMQMessageConverter();
        MappingJackson2MessageConverter jacksonConverter = new MappingJackson2MessageConverter();
        jacksonConverter.setSerializedPayloadClass(String.class);
        CompositeMessageConverter composite = new CompositeMessageConverter(
                List.of(jacksonConverter));
        converter.setMessageConverter(composite);
        return converter;
    }
}