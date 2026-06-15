package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

@Data
@Schema(description = "发送聊天消息DTO")
public class ChatSendDTO {

    @NotNull(message = "会话ID不能为空")
    @Schema(description = "会话ID", example = "1")
    private Long sessionId;

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "消息内容", example = "减脂期可以吃水果吗？")
    private String content;

    @Schema(description = "SSE 断点续传游标（从该位置开始续传）", example = "42")
    private Integer cursor;

    @Schema(description = "是否为重新生成请求", example = "false")
    private Boolean regenerate;

    @Schema(description = "页面上下文（由 GlobalCopilotDrawer 传递，包含当前页面和实体信息）")
    private Map<String, Object> context;
}