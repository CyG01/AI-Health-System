package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 隐私操作审计日志实体类
 */
@TableName("privacy_audit_log")
public class PrivacyAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 操作类型：DATA_EXPORT(数据导出)/DATA_PURGE(数据焚毁)/CONSENT_CHANGE(授权变更)/MEMORY_SANDBOX(记忆沙盒)/ACCESS_LOG(访问日志) */
    private String actionType;

    /** 操作描述 */
    private String actionDescription;

    /** 操作详情（JSON格式） */
    private String actionDetails;

    /** 操作IP */
    private String ipAddress;

    /** 操作设备 */
    private String userAgent;

    /** 操作结果：SUCCESS/FAILED */
    private String result;

    /** 错误信息（失败时） */
    private String errorMessage;

    /** 创建时间 */
    private LocalDateTime createdAt;

    // --- getters/setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getActionDescription() { return actionDescription; }
    public void setActionDescription(String actionDescription) { this.actionDescription = actionDescription; }

    public String getActionDetails() { return actionDetails; }
    public void setActionDetails(String actionDetails) { this.actionDetails = actionDetails; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
