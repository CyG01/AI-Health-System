package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 多模态上传记录实体类
 */
@TableName("multimodal_upload")
public class MultimodalUpload implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 上传类型：IMAGE(图片)/AUDIO(语音)/VIDEO(视频) */
    private String uploadType;

    /** 文件存储URL */
    private String fileUrl;

    /** 文件大小（字节） */
    private Long fileSize;

    /** MIME类型 */
    private String mimeType;

    /** 时长（秒，音视频） */
    private Integer durationSeconds;

    /** 处理状态：PENDING/PROCESSING/SUCCESS/FAILED */
    private String processingStatus;

    /** AI解析结果（JSON格式） */
    private String processingResult;

    /** 关联的记录类型：DIET/EXERCISE/SYMPTOM/BLOOD_SUGAR */
    private String recordType;

    /** 关联的记录ID */
    private Long recordId;

    /** 使用的模型（如 qwen-vl-max） */
    private String modelUsed;

    /** 解析置信度 0.00-1.00 */
    private java.math.BigDecimal confidence;

    /** 用户是否修正过 0=否 1=是 */
    private Integer userCorrected;

    /** 错误信息（处理失败时） */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 处理完成时间 */
    private LocalDateTime processedAt;

    // --- getters/setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUploadType() { return uploadType; }
    public void setUploadType(String uploadType) { this.uploadType = uploadType; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public Integer getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Integer durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(String processingStatus) { this.processingStatus = processingStatus; }

    public String getProcessingResult() { return processingResult; }
    public void setProcessingResult(String processingResult) { this.processingResult = processingResult; }

    public String getRecordType() { return recordType; }
    public void setRecordType(String recordType) { this.recordType = recordType; }

    public Long getRecordId() { return recordId; }
    public void setRecordId(Long recordId) { this.recordId = recordId; }

    public String getModelUsed() { return modelUsed; }
    public void setModelUsed(String modelUsed) { this.modelUsed = modelUsed; }

    public java.math.BigDecimal getConfidence() { return confidence; }
    public void setConfidence(java.math.BigDecimal confidence) { this.confidence = confidence; }

    public Integer getUserCorrected() { return userCorrected; }
    public void setUserCorrected(Integer userCorrected) { this.userCorrected = userCorrected; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
