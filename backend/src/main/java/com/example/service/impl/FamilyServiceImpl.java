package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.common.BusinessException;
import com.example.entity.SysFamily;
import com.example.entity.SysFamilyInvitation;
import com.example.entity.SysFamilyUser;
import com.example.entity.Subscription;
import com.example.mapper.SysFamilyInvitationMapper;
import com.example.mapper.SysFamilyMapper;
import com.example.mapper.SysFamilyUserMapper;
import com.example.mapper.SubscriptionMapper;
import com.example.service.FamilyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 家庭组服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FamilyServiceImpl implements FamilyService {

    private static final String FAMILY_CACHE_PREFIX = "cache:family:";
    private static final String FAMILY_MEMBERS_CACHE_PREFIX = "cache:family:members:";
    private static final String USER_FAMILY_CACHE_PREFIX = "cache:user:family:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    private final SysFamilyMapper familyMapper;
    private final SysFamilyUserMapper familyUserMapper;
    private final SysFamilyInvitationMapper invitationMapper;
    private final SubscriptionMapper subscriptionMapper;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFamily createFamily(Long userId, String familyName) {
        // 检查用户是否已有主家庭
        Long primaryFamilyId = familyUserMapper.selectPrimaryFamilyId(userId);
        if (primaryFamilyId != null) {
            throw new BusinessException(400, "您已创建或加入家庭，不能重复创建");
        }

        // 创建家庭
        SysFamily family = new SysFamily();
        family.setFamilyName(familyName);
        family.setCreatorId(userId);
        family.setMaxMembers(6);
        family.setShareHealthData(0);
        family.setShareReports(1);
        family.setStatus(1);
        family.setCreatedAt(LocalDateTime.now());
        family.setUpdatedAt(LocalDateTime.now());
        familyMapper.insert(family);

        // 添加创建者为OWNER
        SysFamilyUser familyUser = new SysFamilyUser();
        familyUser.setFamilyId(family.getId());
        familyUser.setUserId(userId);
        familyUser.setMemberRole("OWNER");
        familyUser.setDataVisibility("PRIVATE");
        familyUser.setJoinTime(LocalDateTime.now());
        familyUser.setStatus(1);
        familyUser.setCreatedAt(LocalDateTime.now());
        familyUser.setUpdatedAt(LocalDateTime.now());
        familyUserMapper.insert(familyUser);

        // 清除缓存
        evictUserFamilyCache(userId);

        log.info("创建家庭成功 userId={} familyId={} familyName={}", userId, family.getId(), familyName);
        return family;
    }

    @Override
    public List<SysFamily> getUserFamilies(Long userId) {
        // 尝试从缓存获取
        String cacheKey = USER_FAMILY_CACHE_PREFIX + userId;
        // 简化处理：直接查数据库，实际项目可使用Redis缓存序列化对象
        return familyMapper.selectFamiliesByUserId(userId);
    }

    @Override
    public SysFamily getPrimaryFamily(Long userId) {
        Long familyId = familyUserMapper.selectPrimaryFamilyId(userId);
        if (familyId == null) {
            return null;
        }
        return familyMapper.selectById(familyId);
    }

    @Override
    public List<SysFamilyUser> getFamilyMembers(Long familyId, Long userId) {
        // 校验用户是否属于该家庭
        String memberRole = familyUserMapper.selectMemberRole(familyId, userId);
        if (memberRole == null) {
            throw new BusinessException(403, "您不是该家庭成员，无权查看");
        }

        return familyUserMapper.selectMembersByFamilyId(familyId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SysFamilyInvitation inviteMember(Long familyId, Long inviterId, String phone, String role) {
        // 校验权限：只有OWNER和ADMIN可以邀请
        String inviterRole = familyUserMapper.selectMemberRole(familyId, inviterId);
        if (inviterRole == null || (!"OWNER".equals(inviterRole) && !"ADMIN".equals(inviterRole))) {
            throw new BusinessException(403, "您没有邀请成员的权限");
        }

        // 校验角色合法性
        List<String> validRoles = Arrays.asList("MEMBER", "CHILD", "ELDER");
        if (!validRoles.contains(role)) {
            role = "MEMBER";
        }

        // 检查家庭人数是否已满
        int memberCount = familyUserMapper.countMembersByFamilyId(familyId);
        SysFamily family = familyMapper.selectById(familyId);
        if (memberCount >= family.getMaxMembers()) {
            throw new BusinessException(400, "家庭人数已达上限");
        }

        // 生成邀请码
        String inviteCode = generateInviteCode();

        // 创建邀请记录
        SysFamilyInvitation invitation = new SysFamilyInvitation();
        invitation.setFamilyId(familyId);
        invitation.setInviterId(inviterId);
        invitation.setInviteePhone(phone);
        invitation.setInviteCode(inviteCode);
        invitation.setMemberRole(role);
        invitation.setExpireTime(LocalDateTime.now().plusDays(7)); // 7天有效
        invitation.setStatus(0);
        invitation.setCreatedAt(LocalDateTime.now());
        invitationMapper.insert(invitation);

        log.info("创建家庭邀请 familyId={} inviterId={} phone={} role={}", familyId, inviterId, phone, role);
        return invitation;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> joinFamilyByCode(Long userId, String inviteCode) {
        // 查询邀请记录
        SysFamilyInvitation invitation = invitationMapper.selectOne(
                new LambdaQueryWrapper<SysFamilyInvitation>()
                        .eq(SysFamilyInvitation::getInviteCode, inviteCode)
        );

        if (invitation == null) {
            throw new BusinessException(400, "邀请码无效");
        }

        if (invitation.getStatus() != 0) {
            throw new BusinessException(400, "邀请码已使用或已过期");
        }

        if (invitation.getExpireTime().isBefore(LocalDateTime.now())) {
            // 标记为过期
            invitation.setStatus(2);
            invitationMapper.updateById(invitation);
            throw new BusinessException(400, "邀请码已过期");
        }

        // 检查用户是否已在该家庭
        String existingRole = familyUserMapper.selectMemberRole(invitation.getFamilyId(), userId);
        if (existingRole != null) {
            throw new BusinessException(400, "您已在该家庭中");
        }

        // 检查家庭人数是否已满
        int memberCount = familyUserMapper.countMembersByFamilyId(invitation.getFamilyId());
        SysFamily family = familyMapper.selectById(invitation.getFamilyId());
        if (memberCount >= family.getMaxMembers()) {
            throw new BusinessException(400, "家庭人数已达上限");
        }

        // 添加成员
        SysFamilyUser familyUser = new SysFamilyUser();
        familyUser.setFamilyId(invitation.getFamilyId());
        familyUser.setUserId(userId);
        familyUser.setMemberRole(invitation.getMemberRole());
        familyUser.setDataVisibility("PRIVATE");
        familyUser.setInvitedBy(invitation.getInviterId());
        familyUser.setJoinTime(LocalDateTime.now());
        familyUser.setStatus(1);
        familyUser.setCreatedAt(LocalDateTime.now());
        familyUser.setUpdatedAt(LocalDateTime.now());
        familyUserMapper.insert(familyUser);

        // 更新邀请状态
        invitation.setStatus(1);
        invitation.setAcceptedUserId(userId);
        invitation.setAcceptedAt(LocalDateTime.now());
        invitationMapper.updateById(invitation);

        // 清除缓存
        evictUserFamilyCache(userId);
        evictFamilyMembersCache(invitation.getFamilyId());

        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("familyId", invitation.getFamilyId());
        result.put("familyName", family.getFamilyName());
        result.put("role", invitation.getMemberRole());

        log.info("用户加入家庭成功 userId={} familyId={}", userId, invitation.getFamilyId());
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeMember(Long familyId, Long operatorId, Long memberId) {
        // 校验权限：只有OWNER可以移除成员，且不能移除自己
        String operatorRole = familyUserMapper.selectMemberRole(familyId, operatorId);
        if (!"OWNER".equals(operatorRole)) {
            throw new BusinessException(403, "您没有移除成员的权限");
        }

        if (operatorId.equals(memberId)) {
            throw new BusinessException(400, "不能移除自己，请转让主账号后再退出");
        }

        // 软删除成员
        int rows = familyUserMapper.update(null,
                new LambdaUpdateWrapper<SysFamilyUser>()
                        .eq(SysFamilyUser::getFamilyId, familyId)
                        .eq(SysFamilyUser::getUserId, memberId)
                        .eq(SysFamilyUser::getStatus, 1)
                        .set(SysFamilyUser::getStatus, 0)
                        .set(SysFamilyUser::getUpdatedAt, LocalDateTime.now())
        );

        if (rows > 0) {
            evictUserFamilyCache(memberId);
            evictFamilyMembersCache(familyId);
            log.info("移除家庭成员成功 familyId={} operatorId={} memberId={}", familyId, operatorId, memberId);
        }

        return rows > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateMemberRole(Long familyId, Long operatorId, Long memberId, String newRole) {
        // 校验权限
        String operatorRole = familyUserMapper.selectMemberRole(familyId, operatorId);
        if (!"OWNER".equals(operatorRole)) {
            throw new BusinessException(403, "您没有修改成员角色的权限");
        }

        // 校验角色合法性
        List<String> validRoles = Arrays.asList("OWNER", "ADMIN", "MEMBER", "CHILD", "ELDER");
        if (!validRoles.contains(newRole)) {
            throw new BusinessException(400, "无效的角色类型");
        }

        // 转让主账号
        if ("OWNER".equals(newRole)) {
            // 原主账号降级为ADMIN
            familyUserMapper.update(null,
                    new LambdaUpdateWrapper<SysFamilyUser>()
                            .eq(SysFamilyUser::getFamilyId, familyId)
                            .eq(SysFamilyUser::getMemberRole, "OWNER")
                            .set(SysFamilyUser::getMemberRole, "ADMIN")
                            .set(SysFamilyUser::getUpdatedAt, LocalDateTime.now())
            );
        }

        int rows = familyUserMapper.update(null,
                new LambdaUpdateWrapper<SysFamilyUser>()
                        .eq(SysFamilyUser::getFamilyId, familyId)
                        .eq(SysFamilyUser::getUserId, memberId)
                        .eq(SysFamilyUser::getStatus, 1)
                        .set(SysFamilyUser::getMemberRole, newRole)
                        .set(SysFamilyUser::getUpdatedAt, LocalDateTime.now())
        );

        if (rows > 0) {
            evictFamilyMembersCache(familyId);
            log.info("更新成员角色成功 familyId={} memberId={} newRole={}", familyId, memberId, newRole);
        }

        return rows > 0;
    }

    @Override
    public boolean hasFamilySubscriptionAccess(Long userId, String requiredTier) {
        // 家庭版/企业版功能才需要检查家庭订阅
        if ("pro".equalsIgnoreCase(requiredTier) || "free".equalsIgnoreCase(requiredTier)) {
            return false;
        }

        // 检查用户是否有有效的家庭/企业订阅
        int count = familyMapper.countActiveFamilySubscription(userId);
        return count > 0;
    }

    @Override
    public List<Long> getViewableMemberIds(Long userId) {
        List<Long> viewableIds = new ArrayList<>();
        viewableIds.add(userId); // 自己肯定可以看自己

        // 获取用户所属的家庭
        List<SysFamily> families = getUserFamilies(userId);
        if (families.isEmpty()) {
            return viewableIds;
        }

        for (SysFamily family : families) {
            // 获取用户在家庭中的角色和数据可见性设置
            SysFamilyUser userMember = familyUserMapper.selectOne(
                    new LambdaQueryWrapper<SysFamilyUser>()
                            .eq(SysFamilyUser::getFamilyId, family.getId())
                            .eq(SysFamilyUser::getUserId, userId)
                            .eq(SysFamilyUser::getStatus, 1)
            );

            if (userMember == null) continue;

            // 如果家庭开启了健康数据共享，或者用户是OWNER/ADMIN，可以查看所有成员
            if ((family.getShareHealthData() != null && family.getShareHealthData() == 1)
                    || "OWNER".equals(userMember.getMemberRole())
                    || "ADMIN".equals(userMember.getMemberRole())) {
                List<SysFamilyUser> members = familyUserMapper.selectMembersByFamilyId(family.getId());
                for (SysFamilyUser member : members) {
                    if (!viewableIds.contains(member.getUserId())) {
                        viewableIds.add(member.getUserId());
                    }
                }
            }
        }

        return viewableIds;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean leaveFamily(Long familyId, Long userId) {
        // 检查是否是主账号
        String role = familyUserMapper.selectMemberRole(familyId, userId);
        if ("OWNER".equals(role)) {
            throw new BusinessException(400, "主账号不能直接退出家庭，请先转让主账号或解散家庭");
        }

        int rows = familyUserMapper.update(null,
                new LambdaUpdateWrapper<SysFamilyUser>()
                        .eq(SysFamilyUser::getFamilyId, familyId)
                        .eq(SysFamilyUser::getUserId, userId)
                        .eq(SysFamilyUser::getStatus, 1)
                        .set(SysFamilyUser::setStatus, 0)
                        .set(SysFamilyUser::getUpdatedAt, LocalDateTime.now())
        );

        if (rows > 0) {
            evictUserFamilyCache(userId);
            evictFamilyMembersCache(familyId);
            log.info("用户退出家庭 userId={} familyId={}", userId, familyId);
        }

        return rows > 0;
    }

    // ==================== 私有方法 ====================

    /**
     * 生成邀请码（8位随机字符串）
     */
    private String generateInviteCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * 清除用户家庭缓存
     */
    private void evictUserFamilyCache(Long userId) {
        stringRedisTemplate.delete(USER_FAMILY_CACHE_PREFIX + userId);
    }

    /**
     * 清除家庭成员缓存
     */
    private void evictFamilyMembersCache(Long familyId) {
        stringRedisTemplate.delete(FAMILY_MEMBERS_CACHE_PREFIX + familyId);
    }
}
