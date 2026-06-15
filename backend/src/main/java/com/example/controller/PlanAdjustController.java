package com.example.controller;

import com.example.annotation.RequiresSubscription;
import com.example.common.Result;
import com.example.dto.PlanAdjustDTO;
import com.example.sdui.AiAgentResponse;
import com.example.service.PlanAdjustService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

@Tag(name = "AI动态计划调整")
@RestController
@RequestMapping("/api/ai-plan")
public class PlanAdjustController {

    @Autowired
    private PlanAdjustService planAdjustService;

    @RequiresSubscription(value = "pro", feature = "AI动态计划调整")
    @Operation(summary = "AI动态调整计划（SDUI协议）")
    @PostMapping("/adjust")
    public Result<AiAgentResponse> adjust(@Validated @RequestBody PlanAdjustDTO dto,
                                          @RequestAttribute("userId") Long userId) {
        return Result.success(planAdjustService.adjustPlan(dto.getOriginalPlanId(), userId, dto.getFeedback()));
    }

    @RequiresSubscription(value = "pro", feature = "AI流式计划调整")
    @Operation(summary = "流式调整计划（SSE，返回 tool_calls 供前端热更新）")
    @PostMapping(value = "/adjust-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter adjustStream(@Validated @RequestBody PlanAdjustDTO dto,
                                    @RequestAttribute("userId") Long userId) {
        SseEmitter emitter = new SseEmitter(60_000L);
        try {
            // 调用现有的调整逻辑获取结果
            AiAgentResponse response = planAdjustService.adjustPlan(dto.getOriginalPlanId(), userId, dto.getFeedback());

            // 将 SDUI 响应序列化为 JSON 通过 SSE 推送
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(response);
            emitter.send(SseEmitter.event().name("message").data(json));
            emitter.send(SseEmitter.event().name("message").data("[DONE]"));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                emitter.send(SseEmitter.event().name("message").data("[ERROR]"));
                emitter.complete();
            } catch (IOException ex) {
                emitter.completeWithError(ex);
            }
        }
        return emitter;
    }
}