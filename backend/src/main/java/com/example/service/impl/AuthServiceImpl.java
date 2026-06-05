package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.BusinessException;
import com.example.convert.UserConvert;
import com.example.dto.ResetPasswordDTO;
import com.example.dto.SendCodeDTO;
import com.example.dto.UserLoginByPhoneDTO;
import com.example.dto.UserLoginDTO;
import com.example.dto.UserRegisterDTO;
import com.example.entity.SysUser;
import com.example.mapper.SysUserMapper;
import com.example.service.AuthService;
import com.example.util.JwtUtil;
import com.example.vo.LoginVO;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final long SMS_CODE_EXPIRE_MINUTES = 5;
    private static final String DEFAULT_ROLE = "user";
    private static final int USER_STATUS_ENABLED = 1;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private UserConvert userConvert;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LoginVO register(UserRegisterDTO dto) {
        verifySmsCode(dto.getPhone(), dto.getVerifyCode());

        SysUser existByUsername = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (existByUsername != null) {
            throw new BusinessException("用户名已存在");
        }

        SysUser existByPhone = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, dto.getPhone()));
        if (existByPhone != null) {
            throw new BusinessException("手机号已被注册");
        }

        SysUser user = new SysUser();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setPhone(dto.getPhone());
        user.setNickname(dto.getUsername());
        user.setRole(DEFAULT_ROLE);
        user.setStatus(USER_STATUS_ENABLED);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.insert(user);

        deleteSmsCode(dto.getPhone());
        return buildLoginVO(user);
    }

    @Override
    public LoginVO login(UserLoginDTO dto) {
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        checkUserEnabled(user);
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException("密码错误");
        }
        return buildLoginVO(user);
    }

    @Override
    public LoginVO loginByPhone(UserLoginByPhoneDTO dto) {
        verifySmsCode(dto.getPhone(), dto.getVerifyCode());

        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, dto.getPhone()));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        checkUserEnabled(user);

        deleteSmsCode(dto.getPhone());
        return buildLoginVO(user);
    }

    @Override
    public void sendCode(SendCodeDTO dto) {
        String code = String.format("%06d", ThreadLocalRandom.current().nextInt(1000000));
        stringRedisTemplate.opsForValue().set(
                SMS_CODE_PREFIX + dto.getPhone(),
                code,
                SMS_CODE_EXPIRE_MINUTES,
                TimeUnit.MINUTES);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordDTO dto) {
        verifySmsCode(dto.getPhone(), dto.getVerifyCode());

        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, dto.getPhone()));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        checkUserEnabled(user);

        user.setPassword(passwordEncoder.encode(dto.getNewPassword()));
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);

        deleteSmsCode(dto.getPhone());
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(401, "未登录或token已过期");
        }
        if (!jwtUtil.validateToken(refreshToken)) {
            throw new BusinessException(401, "未登录或token已过期");
        }

        Claims claims = jwtUtil.parseToken(refreshToken);
        if (!"refresh".equals(claims.get("tokenType", String.class))) {
            throw new BusinessException(401, "未登录或token已过期");
        }

        Long userId = Long.parseLong(claims.getSubject());
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        checkUserEnabled(user);

        return buildLoginVO(user);
    }

    @Override
    public void logout(String authorization) {
        String token = jwtUtil.extractToken(authorization);
        if (token == null || token.isBlank()) {
            throw new BusinessException(401, "未登录或token已过期");
        }
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(401, "未登录或token已过期");
        }

        Claims claims = jwtUtil.parseToken(token);
        long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            stringRedisTemplate.opsForValue().set(
                    TOKEN_BLACKLIST_PREFIX + token,
                    "1",
                    ttl,
                    TimeUnit.MILLISECONDS);
        }
    }

    private LoginVO buildLoginVO(SysUser user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(), user.getRole());

        LoginVO loginVO = new LoginVO();
        loginVO.setToken(accessToken);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setUserInfo(userConvert.toUserInfoVO(user));
        return loginVO;
    }

    private void verifySmsCode(String phone, String code) {
        String cachedCode = stringRedisTemplate.opsForValue().get(SMS_CODE_PREFIX + phone);
        if (cachedCode == null) {
            throw new BusinessException("验证码已过期");
        }
        if (!cachedCode.equals(code)) {
            throw new BusinessException("验证码错误");
        }
    }

    private void deleteSmsCode(String phone) {
        stringRedisTemplate.delete(SMS_CODE_PREFIX + phone);
    }

    private void checkUserEnabled(SysUser user) {
        if (user.getStatus() != null && user.getStatus() != USER_STATUS_ENABLED) {
            throw new BusinessException("账号已被禁用");
        }
    }
}
