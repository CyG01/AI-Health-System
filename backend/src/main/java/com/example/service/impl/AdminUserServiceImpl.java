package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.convert.UserConvert;
import com.example.entity.SysUser;
import com.example.mapper.SysUserMapper;
import com.example.service.AdminUserService;
import com.example.vo.UserInfoVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private UserConvert userConvert;

    @Override
    public Page<UserInfoVO> listUsers(int page, int size, String keyword) {
        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getPhone, keyword));
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = sysUserMapper.selectPage(pageParam, wrapper);
        Page<UserInfoVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(userConvert::toUserInfoVO).toList());
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void banUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if ("admin".equals(user.getRole())) {
            throw new BusinessException("不能禁用管理员账号");
        }
        user.setStatus(0);
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        log.info("管理员禁用用户 userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbanUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setStatus(1);
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        log.info("管理员启用用户 userId={}", userId);
    }
}
