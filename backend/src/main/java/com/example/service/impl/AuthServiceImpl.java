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
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String SMS_CODE_PREFIX = "sms:code:";
    private static final String SMS_RATE_LIMIT_PREFIX = "sms:rate:";
    private static final String CAPTCHA_PREFIX = "captcha:";
    private static final String AUTH_FAIL_PREFIX = "auth:fail:";
    private static final String AUTH_LOCK_PREFIX = "auth:lock:";
    private static final String AUTH_BLACKLIST_PREFIX = "auth:blacklist:";
    private static final long SMS_CODE_EXPIRE_MINUTES = 5;
    private static final long SMS_RATE_LIMIT_SECONDS = 60;
    private static final long LOCK_EXPIRE_MINUTES = 15;
    private static final int MAX_FAIL_COUNT = 5;
    private static final String DEFAULT_ROLE = "user";
    private static final int USER_STATUS_ENABLED = 1;
    private static final String DEV_DEFAULT_CODE = "123456";

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    @Value("${sms.dev-mode:true}")
    private boolean smsDevMode;

    private final SysUserMapper sysUserMapper;
    private final UserConvert userConvert;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final Environment environment;

    @PostConstruct
    public void checkSmsDevMode() {
        if (smsDevMode) {
            String[] activeProfiles = environment.getActiveProfiles();
            boolean isProd = false;
            for (String profile : activeProfiles) {
                if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                    isProd = true;
                    break;
                }
            }
            if (isProd) {
                throw new IllegalStateException(
                        "生产环境不允许开启 SMS 开发模式！请设置 SMS_DEV_MODE=false");
            }
            log.warn("⚠️  SMS 开发模式已开启 — 所有短信验证码固定为 {}，" +
                    "生产环境务必关闭！", DEV_DEFAULT_CODE);
        }
    }

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
        sysUserMapper.insert(user);

        deleteSmsCode(dto.getPhone());
        return buildLoginVO(user, false);
    }

    @Override
    public LoginVO login(UserLoginDTO dto) {
        // 1. 校验验证码
        String cachedCaptcha = stringRedisTemplate.opsForValue().get(CAPTCHA_PREFIX + dto.getCaptchaUuid());
        if (cachedCaptcha == null || !cachedCaptcha.equalsIgnoreCase(dto.getCaptchaCode())) {
            throw new BusinessException("验证码错误或已过期");
        }
        stringRedisTemplate.delete(CAPTCHA_PREFIX + dto.getCaptchaUuid());

        // 2. 账号锁定检查
        String lockKey = AUTH_LOCK_PREFIX + dto.getUsername();
        Boolean isLocked = stringRedisTemplate.hasKey(lockKey);
        if (Boolean.TRUE.equals(isLocked)) {
            throw new BusinessException("账号已锁定，请15分钟后再试");
        }

        // 3. 查询用户
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, dto.getUsername()));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        checkUserEnabled(user);

        // 4. 密码校验
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            String failKey = AUTH_FAIL_PREFIX + dto.getUsername();
            Long failCount = stringRedisTemplate.opsForValue().increment(failKey);
            stringRedisTemplate.expire(failKey, LOCK_EXPIRE_MINUTES, TimeUnit.MINUTES);
            int remaining = MAX_FAIL_COUNT - failCount.intValue();
            if (remaining <= 0) {
                stringRedisTemplate.opsForValue().set(lockKey, "1", LOCK_EXPIRE_MINUTES, TimeUnit.MINUTES);
                stringRedisTemplate.delete(failKey);
                throw new BusinessException("账号已锁定，请15分钟后再试");
            }
            throw new BusinessException("密码错误，还有" + remaining + "次机会");
        }

        // 5. 密码正确：清除失败计数
        stringRedisTemplate.delete(AUTH_FAIL_PREFIX + dto.getUsername());

        return buildLoginVO(user, dto.isRememberMe());
    }

    @Override
    public LoginVO loginByPhone(UserLoginByPhoneDTO dto) {
        // 1. 账号锁定检查（手机号登录统一保护）
        String lockKey = AUTH_LOCK_PREFIX + dto.getPhone();
        Boolean isLocked = stringRedisTemplate.hasKey(lockKey);
        if (Boolean.TRUE.equals(isLocked)) {
            throw new BusinessException("账号已锁定，请15分钟后再试");
        }

        // 2. 查询用户
        SysUser user = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getPhone, dto.getPhone()));
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        checkUserEnabled(user);

        // 3. 验证码校验（含失败计数）
        String cachedCode = stringRedisTemplate.opsForValue().get(SMS_CODE_PREFIX + dto.getPhone());
        if (cachedCode == null) {
            throw new BusinessException("验证码已过期");
        }
        if (!cachedCode.equals(dto.getVerifyCode())) {
            String failKey = AUTH_FAIL_PREFIX + dto.getPhone();
            Long failCount = stringRedisTemplate.opsForValue().increment(failKey);
            stringRedisTemplate.expire(failKey, LOCK_EXPIRE_MINUTES, TimeUnit.MINUTES);
            int remaining = MAX_FAIL_COUNT - failCount.intValue();
            if (remaining <= 0) {
                stringRedisTemplate.opsForValue().set(lockKey, "1", LOCK_EXPIRE_MINUTES, TimeUnit.MINUTES);
                stringRedisTemplate.delete(failKey);
                throw new BusinessException("账号已锁定，请15分钟后再试");
            }
            throw new BusinessException("验证码错误，还有" + remaining + "次机会");
        }

        // 验证码正确：清除失败计数
        stringRedisTemplate.delete(AUTH_FAIL_PREFIX + dto.getPhone());
        deleteSmsCode(dto.getPhone());
        return buildLoginVO(user, dto.isRememberMe());
    }

    @Override
    public void sendCode(SendCodeDTO dto) {
        // 按手机号频率限制：同一手机号 60 秒内只能发一次
        String rateKey = SMS_RATE_LIMIT_PREFIX + dto.getPhone();
        Boolean rateLimited = stringRedisTemplate.hasKey(rateKey);
        if (Boolean.TRUE.equals(rateLimited)) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }

        String code;
        if (smsDevMode) {
            code = DEV_DEFAULT_CODE;
        } else {
            code = generateSmsCode(6);
        }
        stringRedisTemplate.opsForValue().set(
                SMS_CODE_PREFIX + dto.getPhone(),
                code,
                SMS_CODE_EXPIRE_MINUTES,
                TimeUnit.MINUTES);
        // 设置发送频率限制
        stringRedisTemplate.opsForValue().set(
                rateKey, "1", SMS_RATE_LIMIT_SECONDS, TimeUnit.SECONDS);
        log.info("发送验证码 — 手机号: {} mode={}", maskPhone(dto.getPhone()), smsDevMode ? "dev" : "prod");
    }

    private String generateSmsCode(int length) {
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
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
        sysUserMapper.updateById(user);

        deleteSmsCode(dto.getPhone());
    }

    @Override
    public LoginVO refresh(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(401, "未登录或token已过期");
        }

        String token = jwtUtil.extractToken(refreshToken);
        if (token == null || token.isBlank()) {
            throw new BusinessException(401, "未登录或token已过期");
        }

        // 检查refreshToken是否在黑名单
        String blacklistKey = AUTH_BLACKLIST_PREFIX + token;
        Boolean isBlacklisted = stringRedisTemplate.hasKey(blacklistKey);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            throw new BusinessException(401, "Token已失效，请重新登录");
        }

        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(401, "未登录或token已过期");
        }

        Claims claims = jwtUtil.parseToken(token);
        if (!"refresh".equals(claims.get("tokenType", String.class))) {
            throw new BusinessException(401, "未登录或token已过期");
        }

        Long userId = Long.parseLong(claims.getSubject());
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        checkUserEnabled(user);

        // 将旧的refreshToken加入黑名单
        long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
        if (ttl > 0) {
            stringRedisTemplate.opsForValue().set(
                    blacklistKey, "1", ttl, TimeUnit.MILLISECONDS);
        }

        return buildLoginVO(user, false);
    }

    @Override
    public void logout(String authorization, String refreshToken) {
        // 将 accessToken 加入黑名单
        String accessToken = jwtUtil.extractToken(authorization);
        if (accessToken != null && !accessToken.isBlank() && jwtUtil.validateToken(accessToken)) {
            Claims claims = jwtUtil.parseToken(accessToken);
            long ttl = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (ttl > 0) {
                stringRedisTemplate.opsForValue().set(
                        AUTH_BLACKLIST_PREFIX + accessToken, "1", ttl, TimeUnit.MILLISECONDS);
            }
        }

        // 将 refreshToken 也加入黑名单
        if (refreshToken != null && !refreshToken.isBlank()) {
            String rt = jwtUtil.extractToken(refreshToken);
            if (rt != null && !rt.isBlank() && jwtUtil.validateToken(rt)) {
                Claims refreshClaims = jwtUtil.parseToken(rt);
                long refreshTtl = refreshClaims.getExpiration().getTime() - System.currentTimeMillis();
                if (refreshTtl > 0) {
                    stringRedisTemplate.opsForValue().set(
                            AUTH_BLACKLIST_PREFIX + rt, "1", refreshTtl, TimeUnit.MILLISECONDS);
                }
            }
        }
    }

    private LoginVO buildLoginVO(SysUser user, boolean rememberMe) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
        String refreshToken = rememberMe
                ? jwtUtil.generateRefreshToken(user.getId(), 30L * 24 * 60 * 60 * 1000)  // 30天
                : jwtUtil.generateRefreshToken(user.getId());

        LoginVO loginVO = new LoginVO();
        loginVO.setAccessToken(accessToken);
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

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        String prefix = email.substring(0, atIndex);
        if (prefix.length() <= 2) {
            return email.charAt(0) + "****" + email.substring(atIndex);
        }
        return prefix.charAt(0) + "****" + prefix.charAt(prefix.length() - 1) + email.substring(atIndex);
    }
}
