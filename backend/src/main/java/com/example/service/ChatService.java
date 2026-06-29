package com.example.service;

import com.example.vo.ChatSessionVO;

import java.util.List;

public interface ChatService {

    /**
     * 创建新会话
     */
    ChatSessionVO createSession(Long userId);

    /**
     * 获取用户所有会话列表
     */
    List<ChatSessionVO> getSessionList(Long userId);

    /**
     * 获取会话的聊天记录
     */
    List<Object> getMessages(Long sessionId, Long userId);

    /**
     * 发送消息并获取AI流式回复（SSE）
     */
    void chat(Long sessionId, Long userId, String content, java.util.function.Consumer<String> onMessage,
              Runnable onComplete, java.util.function.Consumer<String> onError);

    /**
     * 删除会话
     */
    void deleteSession(Long sessionId, Long userId);
}