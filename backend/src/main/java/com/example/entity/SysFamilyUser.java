package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 家庭成员关系实体类
 */
@TableName("sys_family_user")
public class SysFamilyUser implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 家庭ID */
    private Long familyId;

    /** 用户ID */
    private Long userId;

    /** 成员角色：OWNER(主账号)/ADMIN(管理员)/MEMBER(普通成员)/CHILD(儿童)/ELDER(老人) */
    private String memberRole;

    /** 在家庭中的昵称 */
    private String nicknameInFamily;

    /** 数据可见性：PRIVATE(仅自己)/FAMILY(全家可见)/REPORT_ONLY(仅周报) */
    private String dataVisibility;

    /** 可查看的成员ID列表（JSON数组，用于细粒度控制） */
    private String canViewMembers;

    /** 加入时间 */
    private LocalDateTime joinTime;

    /** 邀请人用户ID */
    private Long invitedBy;

    /** 状态 0=已退出 1=正常 2=待确认 */
    private Integer status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // --- getters/setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getFamilyId() { return familyId; }
    public void setFamilyId(Long familyId) { this.familyId = familyId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getMemberRole() { return memberRole; }
    public void setMemberRole(String memberRole) { this.memberRole = memberRole; }

    public String getNicknameInFamily() { return nicknameInFamily; }
    public void setNicknameInFamily(String nicknameInFamily) { this.nicknameInFamily = nicknameInFamily; }

    public String getDataVisibility() { return dataVisibility; }
    public void setDataVisibility(String dataVisibility) { this.dataVisibility = dataVisibility; }

    public String getCanViewMembers() { return canViewMembers; }
    public void setCanViewMembers(String canViewMembers) { this.canViewMembers = canViewMembers; }

    public LocalDateTime getJoinTime() { return joinTime; }
    public void setJoinTime(LocalDateTime joinTime) { this.joinTime = joinTime; }

    public Long getInvitedBy() { return invitedBy; }
    public void setInvitedBy(Long invitedBy) { this.invitedBy = invitedBy; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
