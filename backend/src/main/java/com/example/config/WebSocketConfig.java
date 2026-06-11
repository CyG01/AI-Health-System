package com.example.config;

import com.example.websocket.TaskResultWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * WebSocket 配置 — 用于 AI 任务异步结果推送。
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final TaskResultWebSocketHandler taskResultHandler;

    public WebSocketConfig(TaskResultWebSocketHandler taskResultHandler) {
        this.taskResultHandler = taskResultHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(taskResultHandler, "/ws/task-result")
                .setAllowedOrigins("*");
    }
}