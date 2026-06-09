package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public class SendNotificationDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long userId;

    @NotBlank(message = "标题不能为空")
    @Size(max = 100, message = "标题最长100个字符")
    private String title;

    @NotBlank(message = "内容不能为空")
    @Size(max = 500, message = "内容最长500个字符")
    private String content;

    private String type;

    private boolean sendToAll;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public boolean isSendToAll() { return sendToAll; }
    public void setSendToAll(boolean sendToAll) { this.sendToAll = sendToAll; }
}
