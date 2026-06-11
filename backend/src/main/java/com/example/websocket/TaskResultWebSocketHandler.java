package com.example.websocket;

import com.example.dto.TaskStatusVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket 处理器 — 向已连接客户端推送 AI 任务结果。
 */
@Slf4j
@Component
public class TaskResultWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper objectMapper;

    /** userId -> WebSocketSession */
    private final Map<Long, WebSocketSession> userSessions = new ConcurrentHashMap<>();

    public TaskResultWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = extractUserId(session);
        if (userId != null) {
            userSessions.put(userId, session);
            log.info("WebSocket 连接建立 userId={} sessionId={}", userId, session.getId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = extractUserId(session);
        if (userId != null) {
            userSessions.remove(userId);
            log.info("WebSocket 连接关闭 userId={} sessionId={}", userId, session.getId());
        }
    }

    /**
     * 向指定用户推送任务状态更新。
     */
    public void pushTaskResult(Long userId, TaskStatusVO status) {
        WebSocketSession session = userSessions.get(userId);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(status);
            session.sendMessage(new TextMessage(json));
            log.debug("WebSocket 推送成功 userId={} taskId={}", userId, status.getTaskId());
        } catch (IOException e) {
            log.error("WebSocket 推送失败 userId={} taskId={}", userId, status.getTaskId(), e);
        }
    }

    /**
     * 从 WebSocket 查询参数中提取 userId。
     */
    private Long extractUserId(WebSocketSession session) {
        try {
            String query = session.getUri() != null ? session.getUri().getQuery() : null;
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] kv = param.split("=");
                    if (kv.length == 2 && "userId".equals(kv[0])) {
                        return Long.parseLong(kv[1]);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析 WebSocket userId 失败", e);
        }
        return null;
    }
}