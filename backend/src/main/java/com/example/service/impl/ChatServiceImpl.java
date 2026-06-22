package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.agent.model.RoutingDecision;
import com.example.agent.orchestrator.AgentOrchestrator;
import com.example.agent.orchestrator.IntentRouter;
import com.example.common.BusinessException;
import com.example.entity.*;
import com.example.mapper.*;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.sdui.AiAgentResponse;
import com.example.service.ChatService;
import com.example.service.HealthService;
import com.example.service.KnowledgeService;
import com.example.service.MemoryService;
import com.example.util.EmotionAnalyzer;
import com.example.util.MedicalDisclaimerFilter;
import com.example.util.PromptSanitizer;
import com.example.vo.ChatMessageVO;
import com.example.vo.ChatSessionVO;
import com.example.dto.AiTaskMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 聊天服务实现 — Multi-Agent 架构版本。
 * 通过 IntentRouter 分析用户意图，分发到 AgentOrchestrator 调度
 * HealthCoachAgent / NutritionAgent / PsychologyAgent 协作回复。
 */
@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private static final int MAX_CONTEXT_MESSAGES = 20;
    private static final int STREAM_CHUNK_SIZE = 10;

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final HealthService healthService;
    private final DeepSeekCostMonitor costMonitor;
    private final ObjectMapper objectMapper;
    private final MedicalDisclaimerFilter disclaimerFilter;
    private final AiCallAuditLogMapper auditLogMapper;
    private final EmotionAnalyzer emotionAnalyzer;
    private final KnowledgeService knowledgeService;
    private final MemoryService memoryService;
    private final IntentRouter intentRouter;
    private final AgentOrchestrator agentOrchestrator;
    private final ChatServiceImpl self;

    public ChatServiceImpl(ChatSessionMapper chatSessionMapper,
                           ChatMessageMapper chatMessageMapper,
                           HealthService healthService,
                           DeepSeekCostMonitor costMonitor,
                           ObjectMapper objectMapper,
                           MedicalDisclaimerFilter disclaimerFilter,
                           AiCallAuditLogMapper auditLogMapper,
                           EmotionAnalyzer emotionAnalyzer,
                           KnowledgeService knowledgeService,
                           MemoryService memoryService,
                           IntentRouter intentRouter,
                           AgentOrchestrator agentOrchestrator,
                           @Lazy ChatServiceImpl self) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.healthService = healthService;
        this.costMonitor = costMonitor;
        this.objectMapper = objectMapper;
        this.disclaimerFilter = disclaimerFilter;
        this.auditLogMapper = auditLogMapper;
        this.emotionAnalyzer = emotionAnalyzer;
        this.knowledgeService = knowledgeService;
        this.memoryService = memoryService;
        this.intentRouter = intentRouter;
        this.agentOrchestrator = agentOrchestrator;
        this.self = self;
    }

    @Override
    @Transactional
    public ChatSessionVO createSession(Long userId) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle("新对话");
        chatSessionMapper.insert(session);

        ChatSessionVO vo = new ChatSessionVO();
        vo.setId(session.getId());
        vo.setTitle(session.getTitle());
        vo.setCreateTime(session.getCreateTime());
        vo.setUpdateTime(session.getUpdateTime());
        return vo;
    }

    @Override
    public List<ChatSessionVO> getSessionList(Long userId) {
        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatSession::getUserId, userId)
                .orderByDesc(ChatSession::getUpdateTime);
        return chatSessionMapper.selectList(wrapper).stream().map(s -> {
            ChatSessionVO vo = new ChatSessionVO();
            vo.setId(s.getId());
            vo.setTitle(s.getTitle());
            vo.setCreateTime(s.getCreateTime());
            vo.setUpdateTime(s.getUpdateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<Object> getMessages(Long sessionId, Long userId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(404, "会话不存在");
        }

        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByAsc(ChatMessage::getCreateTime);
        return chatMessageMapper.selectList(wrapper).stream().map(m -> {
            ChatMessageVO vo = new ChatMessageVO();
            vo.setId(m.getId());
            vo.setRole(m.getRole());
            vo.setContent(m.getContent());
            vo.setCreateTime(m.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public void chat(Long sessionId, Long userId, String content,
                     Consumer<String> onMessage, Runnable onComplete, Consumer<String> onError) {
        long startTime = System.currentTimeMillis();
        AiCallAuditLog auditLog = new AiCallAuditLog();
        auditLog.setUserId(userId);
        auditLog.setCallType("chat_multi_agent");
        auditLog.setCreatedAt(LocalDateTime.now());

        try {
            // 额度检查 — 全局日预算
            if (costMonitor.isGlobalCostExceeded()) {
                auditLog.setSuccess(false);
                auditLog.setErrorMessage("今日AI调用额度已用尽");
                auditLogMapper.insert(auditLog);
                onError.accept("今日AI调用额度已用尽，请明天再试");
                return;
            }

            // 额度检查 — 单用户日预算
            if (costMonitor.isUserCostExceeded(userId)) {
                auditLog.setSuccess(false);
                auditLog.setErrorMessage("用户日预算超限 userId=" + userId);
                auditLogMapper.insert(auditLog);
                onError.accept("您今日的AI使用额度已用完，请明天再来~");
                return;
            }

            // 用户输入注入防护
            String sanitizedContent = PromptSanitizer.sanitize(content);
            auditLog.setRequestParams("sessionId=" + sessionId + ",content_length="
                    + (sanitizedContent != null ? sanitizedContent.length() : 0));

            ChatSession session = chatSessionMapper.selectById(sessionId);
            if (session == null || !session.getUserId().equals(userId)) {
                onError.accept("会话不存在");
                return;
            }

            // Pre-flight DB write: save user message + update session title (short transaction)
            self.saveUserMessageAndSession(sessionId, userId, sanitizedContent, session);

            // Step 1: 意图路由 — 决定调用哪些专家 Agent (AI call, outside transaction)
            RoutingDecision decision = intentRouter.route(sanitizedContent);
            log.info("意图路由 userId={} intent={} agents={} parallel={} confidence={}",
                    userId, decision.getIntent(), decision.getTargetAgents(),
                    decision.isParallel(), decision.getConfidence());
            auditLog.setModelName("multi-agent:" + String.join(",", decision.getTargetAgents()));

            // Step 2: 注入记忆 + 知识上下文到用户输入（增强语义）
            String enrichedInput = enrichWithContext(userId, sessionId, sanitizedContent, decision);
            auditLog.setPromptUsed("intent=" + decision.getIntent() + " emotion=" + decision.getEmotionLabel());

            // Step 3: 调用 AgentOrchestrator 执行多 Agent 工作流 (AI call, outside transaction)
            AiAgentResponse agentResponse = agentOrchestrator.execute(decision, userId, enrichedInput);

            // Step 4: 提取最终文本
            String responseText = agentResponse.getText();
            if (responseText == null || responseText.isBlank()) {
                responseText = "抱歉，我暂时无法处理您的请求，请稍后再试。";
            }

            // 追加免责声明
            String responseWithDisclaimer = disclaimerFilter.appendDisclaimer(responseText);

            // Step 5: 模拟流式输出（将完整响应分块发送，兼容 SSE 协议）
            simulateStreaming(responseWithDisclaimer, onMessage, onComplete);

            // Post-flight DB write: save assistant message + update session + audit log (short transaction)
            if (!responseWithDisclaimer.isEmpty()) {
                auditLog.setAiRawResponse(responseText.length() > 4000
                        ? responseText.substring(0, 4000) : responseText);
                auditLog.setLatencyMs((int) (System.currentTimeMillis() - startTime));
                auditLog.setSuccess(true);
                self.saveAssistantMessageAndSession(sessionId, userId,
                        responseWithDisclaimer, session, auditLog);
            }

            // 自动采集记忆 (non-transactional, best-effort)
            if (isConversationWorthy(sanitizedContent)) {
                try {
                    memoryService.autoCollect(userId, sanitizedContent, "USER_INPUT");
                } catch (Exception e) {
                    log.warn("自动采集用户记忆失败 userId={}", userId, e);
                }
            }
            if (isConversationWorthy(responseText)) {
                try {
                    memoryService.autoCollect(userId, responseText, "AI_GENERATED");
                } catch (Exception e) {
                    log.warn("自动采集AI记忆失败 userId={}", userId, e);
                }
            }

        } catch (Exception e) {
            log.error("Multi-Agent聊天处理异常 userId={}", userId, e);
            auditLog.setSuccess(false);
            auditLog.setErrorMessage("Multi-Agent聊天异常: " + e.getMessage());
            try {
                auditLogMapper.insert(auditLog);
            } catch (Exception ignored) {}
            onError.accept("AI服务暂时不可用，请稍后重试");
        }
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null || !session.getUserId().equals(userId)) {
            throw new BusinessException(404, "会话不存在");
        }
        chatSessionMapper.deleteById(sessionId);
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);
        chatMessageMapper.delete(wrapper);
    }

    /**
     * 在用户输入中注入记忆和知识库上下文，增强 Agent 理解。
     */
    private String enrichWithContext(Long userId, Long sessionId,
                                      String userQuery, RoutingDecision decision) {
        StringBuilder enriched = new StringBuilder();

        // 1. 注入长期记忆
        try {
            String memoryContext = memoryService.buildMemoryContext(userId, userQuery, 5);
            if (!memoryContext.isEmpty()) {
                enriched.append(memoryContext).append("\n\n");
            }
        } catch (Exception e) {
            log.warn("记忆检索失败 userId={}", userId, e);
        }

        // 2. 注入知识库引用
        try {
            boolean isMedicalCore = knowledgeService.isMedicalCoreQuestion(userQuery);
            List<KnowledgeDoc> docs = knowledgeService.searchRelevant(userQuery, isMedicalCore, 3);
            if (!docs.isEmpty()) {
                String knowledgeContext = knowledgeService.buildKnowledgeContext(docs);
                if (!knowledgeContext.isEmpty()) {
                    enriched.append(knowledgeContext).append("\n\n");
                }
            }
        } catch (Exception e) {
            log.warn("知识检索失败 userId={}", userId, e);
        }

        // 3. 情绪标签
        if (!"neutral".equals(decision.getEmotionLabel())) {
            enriched.append("[用户当前情绪状态: ").append(decision.getEmotionLabel()).append("]\n");
        }

        enriched.append(userQuery);
        return enriched.toString();
    }

    /**
     * 模拟流式输出：将完整响应按字符分块发送，兼容 SSE 协议。
     * 使用单线程异步执行，不阻塞主流程。
     */
    private void simulateStreaming(String text, Consumer<String> onMessage, Runnable onComplete) {
        if (text == null || text.isBlank()) {
            onComplete.run();
            return;
        }

        new Thread(() -> {
            try {
                for (int i = 0; i < text.length(); i += STREAM_CHUNK_SIZE) {
                    int end = Math.min(i + STREAM_CHUNK_SIZE, text.length());
                    String chunk = text.substring(i, end);
                    onMessage.accept(chunk);
                    // 模拟打字延迟
                    Thread.sleep(20 + (long) (Math.random() * 30));
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                log.warn("流式模拟异常", e);
            } finally {
                onComplete.run();
            }
        }, "chat-stream-simulator").start();
    }

    /**
     * Short transactional helper: save user message + update session title.
     * Called from chat() BEFORE the long-running AI processing.
     */
    @Transactional
    public void saveUserMessageAndSession(Long sessionId, Long userId,
                                          String sanitizedContent, ChatSession session) {
        ChatMessage userMsg = new ChatMessage();
        userMsg.setUserId(userId);
        userMsg.setSessionId(sessionId);
        userMsg.setRole("user");
        userMsg.setContent(sanitizedContent);
        chatMessageMapper.insert(userMsg);

        boolean isFirstMessage = isSessionFirstMessage(sessionId);
        if (isFirstMessage && sanitizedContent.length() > 20) {
            session.setTitle(sanitizedContent.substring(0, 20) + "...");
        } else if (isFirstMessage) {
            session.setTitle(sanitizedContent);
        }
        session.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(session);
    }

    /**
     * Short transactional helper: save assistant message + update session + insert success audit log.
     * Called from chat() AFTER the AI processing completes outside any transaction.
     */
    @Transactional
    public void saveAssistantMessageAndSession(Long sessionId, Long userId,
                                                String responseWithDisclaimer,
                                                ChatSession session, AiCallAuditLog auditLog) {
        ChatMessage assistantMsg = new ChatMessage();
        assistantMsg.setUserId(userId);
        assistantMsg.setSessionId(sessionId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(responseWithDisclaimer);
        chatMessageMapper.insert(assistantMsg);

        session.setUpdateTime(LocalDateTime.now());
        chatSessionMapper.updateById(session);

        auditLogMapper.insert(auditLog);
    }

    private boolean isSessionFirstMessage(Long sessionId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);
        return chatMessageMapper.selectCount(wrapper) <= 1;
    }

    private boolean isConversationWorthy(String content) {
        if (content == null || content.isBlank() || content.length() < 15) {
            return false;
        }
        String[] trivialPatterns = {"你好", "谢谢", "好的", "收到", "明白了", "OK", "ok", "嗯", "哦", "哈哈"};
        String trimmed = content.trim();
        for (String pattern : trivialPatterns) {
            if (trimmed.equals(pattern)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 同步对话响应（MQ Consumer 调用）。
     * 返回完整的 AI 回复文本。
     */
    public String chatSync(AiTaskMessage message) {
        Long sessionId = Long.parseLong(
                message.getParams() != null ? message.getParams().getOrDefault("sessionId", "0") : "0");
        String content = message.getPayload();
        if (content == null || content.isBlank()) {
            throw new RuntimeException("对话内容为空");
        }

        // 使用同步方式收集回复
        StringBuilder fullResponse = new StringBuilder();
        chat(sessionId, message.getUserId(), content,
                fullResponse::append,           // onMessage
                () -> {},                        // onComplete
                error -> { throw new RuntimeException(error); }  // onError
        );
        return fullResponse.toString();
    }
}