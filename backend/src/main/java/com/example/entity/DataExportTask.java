package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据导出任务实体类
 */
@TableName("data_export_task")
public class DataExportTask implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 导出类型：FULL(全量)/HEALTH(健康数据)/MEMORY(记忆数据)/ACTIVITY(活动数据) */
    private String exportType;

    /** 导出范围（JSON格式，包含时间范围等） */
    private String exportScope;

    /** 任务状态：PENDING/PROCESSING/COMPLETED/FAILED */
    private String status;

    /** 导出文件URL */
    private String fileUrl;

    /** 文件大小（字节） */
    private Long fileSize;

    /** 记录数量 */
    private Integer recordCount;

    /** 错误信息（失败时） */
    private String errorMessage;

    /** 过期时间 */
    private LocalDateTime expireAt;

    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    // --- getters/setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getExportType() { return exportType; }
    public void setExportType(String exportType) { this.exportType = exportType; }

    public String getExportScope() { return exportScope; }
    public void setExportScope(String exportScope) { this.exportScope = exportScope; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public Integer getRecordCount() { return recordCount; }
    public void setRecordCount(Integer recordCount) { this.recordCount = recordCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getExpireAt() { return expireAt; }
    public void setExpireAt(LocalDateTime expireAt) { this.expireAt = expireAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
}
