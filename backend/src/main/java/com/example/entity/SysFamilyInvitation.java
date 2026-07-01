package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家庭邀请实体类
 */
@TableName("sys_family_invitation")
public class SysFamilyInvitation implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 家庭ID */
    private Long familyId;

    /** 邀请人ID */
    private Long inviterId;

    /** 被邀请人手机号 */
    private String inviteePhone;

    /** 被邀请人邮箱 */
    private String inviteeEmail;

    /** 邀请码 */
    private String inviteCode;

    /** 邀请的角色 */
    private String memberRole;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 状态 0=待接受 1=已接受 2=已过期 3=已取消 */
    private Integer status;

    /** 接受邀请的用户ID */
    private Long acceptedUserId;

    /** 接受时间 */
    private LocalDateTime acceptedAt;

    private LocalDateTime createdAt;

    // --- getters/setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFamilyId() { return familyId; }
    public void setFamilyId(Long familyId) { this.familyId = familyId; }

    public Long getInviterId() { return inviterId; }
    public void setInviterId(Long inviterId) { this.inviterId = inviterId; }

    public String getInviteePhone() { return inviteePhone; }
    public void setInviteePhone(String inviteePhone) { this.inviteePhone = inviteePhone; }

    public String getInviteeEmail() { return inviteeEmail; }
    public void setInviteeEmail(String inviteeEmail) { this.inviteeEmail = inviteeEmail; }

    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }

    public String getMemberRole() { return memberRole; }
    public void setMemberRole(String memberRole) { this.memberRole = memberRole; }

    public LocalDateTime getExpireTime() { return expireTime; }
    public void setExpireTime(LocalDateTime expireTime) { this.expireTime = expireTime; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public Long getAcceptedUserId() { return acceptedUserId; }
    public void setAcceptedUserId(Long acceptedUserId) { this.acceptedUserId = acceptedUserId; }

    public LocalDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(LocalDateTime acceptedAt) { this.acceptedAt = acceptedAt; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
