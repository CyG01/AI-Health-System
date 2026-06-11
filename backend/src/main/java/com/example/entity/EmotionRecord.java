package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("emotion_record")
public class EmotionRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 关联的聊天会话ID */
    private Long sessionId;

    /** 情绪类型：TIRED/FRUSTRATED/EXCITED/ANXIOUS/PAIN/NEUTRAL */
    private String emotionType;

    /** 置信度 0.00-1.00 */
    private BigDecimal confidence;

    /** 原始用户输入 — 字段级加密 */
    @TableField(typeHandler = com.example.util.EncryptedStringTypeHandler.class)
    private String originalText;

    /** 触发切换的语气 */
    private String triggeredTone;

    /** 采取的动作：NONE/ADJUST_PLAN/REDUCE_INTENSITY/PUSH_ENCOURAGEMENT */
    private String actionTaken;

    private LocalDateTime createdAt;

    // --- getters/setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getSessionId() { return sessionId; }
    public void setSessionId(Long sessionId) { this.sessionId = sessionId; }

    public String getEmotionType() { return emotionType; }
    public void setEmotionType(String emotionType) { this.emotionType = emotionType; }

    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

    public String getOriginalText() { return originalText; }
    public void setOriginalText(String originalText) { this.originalText = originalText; }

    public String getTriggeredTone() { return triggeredTone; }
    public void setTriggeredTone(String triggeredTone) { this.triggeredTone = triggeredTone; }

    public String getActionTaken() { return actionTaken; }
    public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}