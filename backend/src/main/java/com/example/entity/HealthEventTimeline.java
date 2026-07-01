package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 健康事件时间线实体类
 */
@TableName("health_event_timeline")
public class HealthEventTimeline implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 事件类型：SYMPTOM(症状)/DIAGNOSIS(诊断)/MEDICATION(用药)/SURGERY(手术)/LIFESTYLE_CHANGE(生活方式改变)/CHECKUP(体检) */
    private String eventType;

    /** 事件标题 */
    private String eventTitle;

    /** 事件详细描述 */
    private String eventDescription;

    /** 事件发生日期 */
    private LocalDate eventDate;

    /** 严重程度：MILD(轻微)/MODERATE(中等)/SEVERE(严重) */
    private String severity;

    /** 相关指标（JSON格式） */
    private String relatedIndicators;

    /** 来源：USER_INPUT/AI_EXTRACTED/MEDICAL_RECORD */
    private String source;

    /** 是否已验证 0=否 1=是 */
    private Integer isVerified;

    /** 向量表示（1536维） */
    private String embedding;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // --- getters/setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }

    public String getEventTitle() { return eventTitle; }
    public void setEventTitle(String eventTitle) { this.eventTitle = eventTitle; }

    public String getEventDescription() { return eventDescription; }
    public void setEventDescription(String eventDescription) { this.eventDescription = eventDescription; }

    public LocalDate getEventDate() { return eventDate; }
    public void setEventDate(LocalDate eventDate) { this.eventDate = eventDate; }

    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }

    public String getRelatedIndicators() { return relatedIndicators; }
    public void setRelatedIndicators(String relatedIndicators) { this.relatedIndicators = relatedIndicators; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Integer getIsVerified() { return isVerified; }
    public void setIsVerified(Integer isVerified) { this.isVerified = isVerified; }

    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
