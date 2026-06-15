package com.example.controller;

import com.example.annotation.NoRepeatSubmit;
import com.example.annotation.RateLimit;
import com.example.common.Result;
import com.example.dto.ChatSendDTO;
import com.example.service.ChatService;
import com.example.util.PromptSanitizer;
import com.example.vo.ChatSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Tag(name = "AI健康咨询机器人")
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    /** 共享线程池，避免每次 SSE 请求创建新线程导致泄漏 */
    private static final ExecutorService SSE_EXECUTOR = new ThreadPoolExecutor(
            4, 20, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    /** SSE 响应缓存 Redis Key 前缀 */
    private static final String SSE_CACHE_PREFIX = "sse:resume:";

    /** SSE 响应缓存 TTL（秒） */
    private static final long SSE_CACHE_TTL = 120;

    @Autowired
    private ChatService chatService;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @PreDestroy
    public void shutdown() {
        SSE_EXECUTOR.shutdown();
    }

    @Operation(summary = "创建新会话")
    @PostMapping("/session/create")
    public Result<ChatSessionVO> createSession(@RequestAttribute("userId") Long userId) {
        return Result.success(chatService.createSession(userId));
    }

    @Operation(summary = "获取会话列表")
    @GetMapping("/session/list")
    public Result<List<ChatSessionVO>> sessionList(@RequestAttribute("userId") Long userId) {
        return Result.success(chatService.getSessionList(userId));
    }

    @Operation(summary = "获取会话聊天记录")
    @GetMapping("/session/{sessionId}/messages")
    public Result<List<Object>> messages(@PathVariable Long sessionId,
                                         @RequestAttribute("userId") Long userId) {
        return Result.success(chatService.getMessages(sessionId, userId));
    }

    @Operation(summary = "删除会话")
    @DeleteMapping("/session/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId,
                                      @RequestAttribute("userId") Long userId) {
        chatService.deleteSession(sessionId, userId);
        return Result.success();
    }

    @RateLimit(time = 60, count = 10)
    @NoRepeatSubmit
    @Operation(summary = "发送消息（SSE流式）")
    @PostMapping(value = "/send", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter send(@Validated @RequestBody ChatSendDTO dto,
                           @RequestAttribute("userId") Long userId) {

        // 断点续传模式：从 cursor 位置开始续传
        if (dto.getCursor() != null && dto.getCursor() > 0) {
            return resumeSSE(dto, userId);
        }

        return doSend(dto, userId);
    }

    @RateLimit(time = 60, count = 10)
    @NoRepeatSubmit
    @Operation(summary = "带页面上下文的AI对话（SSE流式）")
    @PostMapping(value = "/send-with-context", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendWithContext(@Validated @RequestBody ChatSendDTO dto,
                                       @RequestAttribute("userId") Long userId) {
        // 将页面上下文注入到消息内容中，让后端 AI 感知当前页面
        if (dto.getContext() != null && !dto.getContext().isEmpty()) {
            String page = (String) dto.getContext().getOrDefault("page", "");
            String entityId = dto.getContext().get("entityId") != null
                    ? dto.getContext().get("entityId").toString() : "";
            String contextPrefix = String.format("[用户当前在「%s」页面%s]\n",
                    page, entityId.isEmpty() ? "" : "，实体ID=" + entityId);
            dto.setContent(contextPrefix + dto.getContent());
        }
        return doSend(dto, userId);
    }

    /**
     * 通用 SSE 发送逻辑，抽取为公共方法供 /send 和 /send-with-context 复用。
     * 包含 Prompt 注入防护和审计日志。
     */
    private SseEmitter doSend(ChatSendDTO dto, Long userId) {
        // === Prompt 注入防护 ===
        String originalContent = dto.getContent();
        String sanitizedContent = PromptSanitizer.sanitizeChatMessage(originalContent);
        if (PromptSanitizer.containsInjection(originalContent)) {
            log.warn("[PromptInjection] userId={}, originalLength={}, filteredLength={}",
                    userId, originalContent.length(), sanitizedContent.length());
        }
        dto.setContent(sanitizedContent);

        SseEmitter emitter = new SseEmitter(120_000L);
        String cacheKey = SSE_CACHE_PREFIX + dto.getSessionId() + ":" + userId;

        SSE_EXECUTOR.execute(() -> {
            try {
                chatService.chat(dto.getSessionId(), userId, dto.getContent(),
                        delta -> {
                            try {
                                // 发送增量数据
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data(delta));
                                // 缓存到 Redis 支持断点续传
                                cacheDelta(cacheKey, delta);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data("[DONE]"));
                                emitter.complete();
                                // 清理缓存
                                clearCache(cacheKey);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        errorMsg -> {
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(errorMsg));
                                emitter.send(SseEmitter.event()
                                        .name("message")
                                        .data("[ERROR]"));
                                emitter.complete();
                                clearCache(cacheKey);
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        });
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 断点续传：从指定 cursor 位置开始重新发送 SSE 数据。
     */
    private SseEmitter resumeSSE(ChatSendDTO dto, Long userId) {
        SseEmitter emitter = new SseEmitter(60_000L);
        String cacheKey = SSE_CACHE_PREFIX + dto.getSessionId() + ":" + userId;
        int cursor = dto.getCursor();

        SSE_EXECUTOR.execute(() -> {
            try {
                if (redisTemplate == null) {
                    emitter.send(SseEmitter.event().name("error").data("断点续传服务不可用"));
                    emitter.send(SseEmitter.event().name("message").data("[ERROR]"));
                    emitter.complete();
                    return;
                }

                // 从 Redis 读取缓存的完整响应
                String cached = redisTemplate.opsForValue().get(cacheKey);
                if (cached == null || cached.isEmpty()) {
                    emitter.send(SseEmitter.event().name("error").data("缓存已过期，请重新发送"));
                    emitter.send(SseEmitter.event().name("message").data("[ERROR]"));
                    emitter.complete();
                    return;
                }

                // 从 cursor 位置开始发送
                String remaining = cached.substring(Math.min(cursor, cached.length()));
                if (remaining.isEmpty()) {
                    emitter.send(SseEmitter.event().name("message").data("[DONE]"));
                    emitter.complete();
                    return;
                }

                // 发送剩余部分的分块
                int chunkSize = 10;
                for (int i = 0; i < remaining.length(); i += chunkSize) {
                    int end = Math.min(i + chunkSize, remaining.length());
                    String chunk = remaining.substring(i, end);
                    emitter.send(SseEmitter.event().name("message").data(chunk));
                    // 模拟快速追赶（无延迟，比正常流更快）
                    Thread.sleep(5);
                }

                emitter.send(SseEmitter.event().name("message").data("[DONE]"));
                emitter.complete();
            } catch (Exception e) {
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * 缓存 SSE 增量到 Redis（追加模式）。
     */
    private void cacheDelta(String cacheKey, String delta) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.opsForValue().append(cacheKey, delta);
            redisTemplate.expire(cacheKey, SSE_CACHE_TTL, TimeUnit.SECONDS);
        } catch (Exception e) {
            // 缓存失败不影响主流程
        }
    }

    /**
     * 清理 SSE 缓存。
     */
    private void clearCache(String cacheKey) {
        if (redisTemplate == null) return;
        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception ignored) {}
    }
}