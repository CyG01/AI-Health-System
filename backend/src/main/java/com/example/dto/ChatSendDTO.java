package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
@Schema(description = "发送聊天消息DTO")
public class ChatSendDTO {

    @NotNull(message = "会话ID不能为空")
    @Schema(description = "会话ID", example = "1")
    private Long sessionId;

    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "消息内容", example = "减脂期可以吃水果吗？")
    private String content;
}