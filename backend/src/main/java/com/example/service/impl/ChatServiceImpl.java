package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.BusinessException;
import com.example.entity.*;
import com.example.mapper.*;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.properties.DeepSeekProperties;
import com.example.service.ChatService;
import com.example.service.HealthService;
import com.example.vo.ChatMessageVO;
import com.example.vo.ChatSessionVO;
import com.example.vo.HealthRecordVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

    private static final String DONE_MARKER = "[DONE]";
    private static final int MAX_CONTEXT_MESSAGES = 20;

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final HealthService healthService;
    private final DeepSeekCostMonitor costMonitor;
    private final DeepSeekProperties deepSeekProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public ChatServiceImpl(ChatSessionMapper chatSessionMapper,
                           ChatMessageMapper chatMessageMapper,
                           HealthService healthService,
                           DeepSeekCostMonitor costMonitor,
                           DeepSeekProperties deepSeekProperties,
                           RedisTemplate<String, Object> redisTemplate,
                           ObjectMapper objectMapper) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.healthService = healthService;
        this.costMonitor = costMonitor;
        this.deepSeekProperties = deepSeekProperties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(deepSeekProperties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + deepSeekProperties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
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
        // 验证会话属于该用户
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
    @Transactional
    public void chat(Long sessionId, Long userId, String content,
                     Consumer<String> onMessage, Runnable onComplete, Consumer<String> onError) {
        try {
            if (costMonitor.isGlobalCostExceeded()) {
                onError.accept("今日AI调用额度已用尽，请明天再试");
                return;
            }

            // 验证会话
            ChatSession session = chatSessionMapper.selectById(sessionId);
            if (session == null || !session.getUserId().equals(userId)) {
                onError.accept("会话不存在");
                return;
            }

            // 保存用户消息
            ChatMessage userMsg = new ChatMessage();
            userMsg.setSessionId(sessionId);
            userMsg.setRole("user");
            userMsg.setContent(content);
            chatMessageMapper.insert(userMsg);

            // 首次对话自动设置标题
            boolean isFirstMessage = isSessionFirstMessage(sessionId);
            if (isFirstMessage && content.length() > 20) {
                session.setTitle(content.substring(0, 20) + "...");
            } else if (isFirstMessage) {
                session.setTitle(content);
            }
            session.setUpdateTime(LocalDateTime.now());
            chatSessionMapper.updateById(session);

            // 构建上下文消息
            List<Map<String, String>> messages = buildContextMessages(sessionId, userId);

            // 调用DeepSeek流式API
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", deepSeekProperties.getModel());
            requestBody.put("messages", messages);
            requestBody.put("stream", true);

            StringBuilder fullResponse = new StringBuilder();

            webClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToFlux(String.class)
                    .timeout(Duration.ofMillis(deepSeekProperties.getTimeout()))
                    .filter(line -> line != null && !line.isBlank())
                    .filter(line -> !DONE_MARKER.equals(line.trim()))
                    .doOnNext(line -> {
                        String delta = extractDelta(line);
                        if (delta != null && !delta.isEmpty()) {
                            fullResponse.append(delta);
                            onMessage.accept(delta);
                        }
                    })
                    .doOnComplete(() -> {
                        // 保存AI回复
                        String responseText = fullResponse.toString();
                        if (!responseText.isEmpty()) {
                            ChatMessage assistantMsg = new ChatMessage();
                            assistantMsg.setSessionId(sessionId);
                            assistantMsg.setRole("assistant");
                            assistantMsg.setContent(responseText);
                            chatMessageMapper.insert(assistantMsg);
                        }
                        session.setUpdateTime(LocalDateTime.now());
                        chatSessionMapper.updateById(session);
                        onComplete.run();
                    })
                    .doOnError(e -> {
                        log.error("AI聊天API异常", e);
                        onError.accept("AI服务暂时不可用，请稍后重试");
                    })
                    .subscribe();

        } catch (Exception e) {
            log.error("聊天处理异常", e);
            onError.accept("系统异常，请稍后重试");
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
        // 删除关联消息
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);
        chatMessageMapper.delete(wrapper);
    }

    private List<Map<String, String>> buildContextMessages(Long sessionId, Long userId) {
        List<Map<String, String>> messages = new ArrayList<>();

        // 添加系统提示
        HealthRecordVO health = healthService.getLatestHealthRecord(userId);
        StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("你是一位专业的AI健康顾问，擅长运动科学、营养学和健康管理。");
        if (health != null) {
            systemPrompt.append(String.format("用户当前数据：身高%.1fcm，体重%.1fkg，BMI%.1f，",
                    health.getHeight(), health.getWeight(), health.getBmi()));
            systemPrompt.append(String.format("基础代谢率%dkcal，每日推荐热量%dkcal。",
                    health.getBmr(), health.getDailyCalorie()));
            if (health.getGoal() != null) {
                systemPrompt.append("健康目标：" + health.getGoal() + "。");
            }
            if (health.getDiseaseHistory() != null) {
                systemPrompt.append("病史：" + health.getDiseaseHistory() + "。");
            }
            if (health.getAllergyHistory() != null) {
                systemPrompt.append("过敏史：" + health.getAllergyHistory() + "。");
            }
        }
        systemPrompt.append("请用中文回答，回复要专业、具体、有可操作性。");

        messages.add(Map.of("role", "system", "content", systemPrompt.toString()));

        // 加载历史消息（最多最近20条）
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId)
                .orderByDesc(ChatMessage::getCreateTime)
                .last("LIMIT " + MAX_CONTEXT_MESSAGES);

        List<ChatMessage> history = chatMessageMapper.selectList(wrapper);
        // 反转回时间升序
        Collections.reverse(history);

        for (ChatMessage msg : history) {
            messages.add(Map.of("role", msg.getRole(), "content", msg.getContent()));
        }

        return messages;
    }

    private boolean isSessionFirstMessage(Long sessionId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getSessionId, sessionId);
        return chatMessageMapper.selectCount(wrapper) <= 1;
    }

    private String extractDelta(String line) {
        try {
            String jsonStr = line.startsWith("data:") ? line.substring(5).trim() : line;
            if (jsonStr.isEmpty() || DONE_MARKER.equals(jsonStr)) return null;
            JsonNode root = objectMapper.readTree(jsonStr);
            JsonNode choices = root.path("choices");
            if (choices.isArray() && !choices.isEmpty()) {
                JsonNode delta = choices.get(0).path("delta");
                JsonNode content = delta.path("content");
                return content.isNull() ? null : content.asText();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}