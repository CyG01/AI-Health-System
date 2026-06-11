package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

@TableName("user_memory")
public class UserMemory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 记忆类型：PREFERENCE/INJURY/FEEDBACK/HABIT/ONBOARDING/HEALTH */
    private String memoryType;

    /** 记忆内容（自然语言文本） */
    private String content;

    /** 向量表示（1536维），由 DeepSeek Embedding API 生成 */
    private String embedding;

    /** 重要性 1-10，≥7 永不删除 */
    private Integer importance;

    /** 来源：USER_INPUT/AI_GENERATED/SYSTEM_RECORD/ONBOARDING */
    private String source;

    /** 访问次数 */
    private Integer accessCount;

    /** 最后访问时间 */
    private LocalDateTime lastAccessedAt;

    private LocalDateTime createdAt;

    // --- getters/setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMemoryType() { return memoryType; }
    public void setMemoryType(String memoryType) { this.memoryType = memoryType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getEmbedding() { return embedding; }
    public void setEmbedding(String embedding) { this.embedding = embedding; }

    public Integer getImportance() { return importance; }
    public void setImportance(Integer importance) { this.importance = importance; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Integer getAccessCount() { return accessCount; }
    public void setAccessCount(Integer accessCount) { this.accessCount = accessCount; }

    public LocalDateTime getLastAccessedAt() { return lastAccessedAt; }
    public void setLastAccessedAt(LocalDateTime lastAccessedAt) { this.lastAccessedAt = lastAccessedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}