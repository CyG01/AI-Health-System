package com.example.service.impl;

import com.example.common.BusinessException;
import com.example.convert.UserConvert;
import com.example.dto.NotificationPreferenceDTO;
import com.example.dto.UpdatePasswordDTO;
import com.example.dto.UpdateProfileDTO;
import com.example.entity.SysUser;
import com.example.mapper.SysUserMapper;
import com.example.service.UserService;
import com.example.vo.UserInfoVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    private static final int USER_STATUS_ENABLED = 1;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private UserConvert userConvert;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updatePassword(Long userId, UpdatePasswordDTO dto) {
        SysUser user = getEnabledUser(userId);
        if (!passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码错误");
        }

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        sysUserMapper.updateById(user);
    }

    @Override
    public UserInfoVO getProfile(Long userId) {
        SysUser user = getEnabledUser(userId);
        return userConvert.toUserInfoVO(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserInfoVO updateProfile(Long userId, UpdateProfileDTO dto) {
        SysUser user = getEnabledUser(userId);

        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getGender() != null) {
            user.setGender(dto.getGender());
        }
        if (dto.getAge() != null) {
            user.setAge(dto.getAge());
        }
        sysUserMapper.updateById(user);

        return userConvert.toUserInfoVO(user);
    }

    private SysUser getEnabledUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (user.getStatus() != null && user.getStatus() != USER_STATUS_ENABLED) {
            throw new BusinessException("账号已被禁用");
        }
        return user;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAvatar(Long userId, String avatarUrl) {
        SysUser user = getEnabledUser(userId);
        user.setAvatar(avatarUrl);
        sysUserMapper.updateById(user);
    }

    @Override
    public String getAvatar(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        return user != null ? user.getAvatar() : null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deactivateAccount(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setStatus(0);
        sysUserMapper.updateById(user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNotificationPreference(Long userId, NotificationPreferenceDTO dto) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        if (dto.getNotificationEnabled() != null) {
            user.setNotificationEnabled(dto.getNotificationEnabled());
        }
        if (dto.getReminderTime() != null) {
            user.setReminderTime(dto.getReminderTime());
        }
        if (dto.getNotifyExercise() != null) user.setNotifyExercise(dto.getNotifyExercise());
        if (dto.getNotifyDiet() != null) user.setNotifyDiet(dto.getNotifyDiet());
        if (dto.getNotifyCheckin() != null) user.setNotifyCheckin(dto.getNotifyCheckin());
        if (dto.getQuietStart() != null) user.setQuietStart(dto.getQuietStart());
        if (dto.getQuietEnd() != null) user.setQuietEnd(dto.getQuietEnd());
        sysUserMapper.updateById(user);
    }
}
