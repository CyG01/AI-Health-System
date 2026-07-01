package com.example.service;

import com.example.entity.SysFamily;
import com.example.entity.SysFamilyInvitation;
import com.example.entity.SysFamilyUser;

import java.util.List;
import java.util.Map;

/**
 * 家庭组服务接口
 */
public interface FamilyService {

    /**
     * 创建家庭组
     *
     * @param userId     创建人用户ID
     * @param familyName 家庭名称
     * @return 创建的家庭信息
     */
    SysFamily createFamily(Long userId, String familyName);

    /**
     * 获取用户所属的家庭列表
     *
     * @param userId 用户ID
     * @return 家庭列表
     */
    List<SysFamily> getUserFamilies(Long userId);

    /**
     * 获取用户的主家庭
     *
     * @param userId 用户ID
     * @return 主家庭信息
     */
    SysFamily getPrimaryFamily(Long userId);

    /**
     * 获取家庭成员列表
     *
     * @param familyId 家庭ID
     * @param userId   当前用户ID（用于权限校验）
     * @return 成员列表
     */
    List<SysFamilyUser> getFamilyMembers(Long familyId, Long userId);

    /**
     * 邀请成员加入家庭
     *
     * @param familyId  家庭ID
     * @param inviterId 邀请人ID
     * @param phone     被邀请人手机号
     * @param role      成员角色
     * @return 邀请信息
     */
    SysFamilyInvitation inviteMember(Long familyId, Long inviterId, String phone, String role);

    /**
     * 通过邀请码加入家庭
     *
     * @param userId     用户ID
     * @param inviteCode 邀请码
     * @return 加入结果
     */
    Map<String, Object> joinFamilyByCode(Long userId, String inviteCode);

    /**
     * 移除家庭成员
     *
     * @param familyId   家庭ID
     * @param operatorId 操作人ID
     * @param memberId   要移除的成员用户ID
     * @return 是否成功
     */
    boolean removeMember(Long familyId, Long operatorId, Long memberId);

    /**
     * 更新成员角色
     *
     * @param familyId   家庭ID
     * @param operatorId 操作人ID
     * @param memberId   成员用户ID
     * @param newRole    新角色
     * @return 是否成功
     */
    boolean updateMemberRole(Long familyId, Long operatorId, Long memberId, String newRole);

    /**
     * 检查用户是否有家庭订阅权限
     *
     * @param userId       用户ID
     * @param requiredTier 所需订阅等级
     * @return 是否有权限
     */
    boolean hasFamilySubscriptionAccess(Long userId, String requiredTier);

    /**
     * 获取用户可查看的家庭成员ID列表（用于数据隔离）
     *
     * @param userId 用户ID
     * @return 可查看的用户ID列表
     */
    List<Long> getViewableMemberIds(Long userId);

    /**
     * 退出家庭
     *
     * @param familyId 家庭ID
     * @param userId   用户ID
     * @return 是否成功
     */
    boolean leaveFamily(Long familyId, Long userId);
}
