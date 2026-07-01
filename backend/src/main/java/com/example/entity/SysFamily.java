package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家庭组实体类
 */
@TableName("sys_family")
public class SysFamily implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 家庭名称 */
    private String familyName;

    /** 家庭头像URL */
    private String familyAvatar;

    /** 创建人用户ID */
    private Long creatorId;

    /** 关联的家庭订阅ID */
    private Long subscriptionId;

    /** 最大成员数（默认6人） */
    private Integer maxMembers;

    /** 是否共享健康数据 0=否 1=是 */
    private Integer shareHealthData;

    /** 是否共享周报 0=否 1=是 */
    private Integer shareReports;

    /** 状态 0=禁用 1=正常 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // --- getters/setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFamilyName() { return familyName; }
    public void setFamilyName(String familyName) { this.familyName = familyName; }

    public String getFamilyAvatar() { return familyAvatar; }
    public void setFamilyAvatar(String familyAvatar) { this.familyAvatar = familyAvatar; }

    public Long getCreatorId() { return creatorId; }
    public void setCreatorId(Long creatorId) { this.creatorId = creatorId; }

    public Long getSubscriptionId() { return subscriptionId; }
    public void setSubscriptionId(Long subscriptionId) { this.subscriptionId = subscriptionId; }

    public Integer getMaxMembers() { return maxMembers; }
    public void setMaxMembers(Integer maxMembers) { this.maxMembers = maxMembers; }

    public Integer getShareHealthData() { return shareHealthData; }
    public void setShareHealthData(Integer shareHealthData) { this.shareHealthData = shareHealthData; }

    public Integer getShareReports() { return shareReports; }
    public void setShareReports(Integer shareReports) { this.shareReports = shareReports; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
