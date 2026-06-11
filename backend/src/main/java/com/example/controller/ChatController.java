package com.example.controller;

import com.example.annotation.NoRepeatSubmit;
import com.example.annotation.RateLimit;
import com.example.common.Result;
import com.example.dto.ChatSendDTO;
import com.example.service.ChatService;
import com.example.vo.ChatSessionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
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

    /** 共享线程池，避免每次 SSE 请求创建新线程导致泄漏 */
    private static final ExecutorService SSE_EXECUTOR = new ThreadPoolExecutor(
            4, 20, 60L, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(100),
            new ThreadPoolExecutor.CallerRunsPolicy()
    );

    @Autowired
    private ChatService chatService;

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
        SseEmitter emitter = new SseEmitter(120_000L);

        SSE_EXECUTOR.execute(() -> {
            try {
                chatService.chat(dto.getSessionId(), userId, dto.getContent(),
                        delta -> {
                            try {
                                emitter.send(SseEmitter.event().name("message").data(delta));
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        () -> {
                            try {
                                emitter.send(SseEmitter.event().name("message").data("[DONE]"));
                                emitter.complete();
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        errorMsg -> {
                            try {
                                emitter.send(SseEmitter.event().name("error").data(errorMsg));
                                emitter.send(SseEmitter.event().name("message").data("[ERROR]"));
                                emitter.complete();
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
}