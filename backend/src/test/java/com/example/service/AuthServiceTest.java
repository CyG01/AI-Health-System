package com.example.service;

import com.example.BaseTest;
import com.example.common.BusinessException;
import com.example.dto.*;
import com.example.entity.SysUser;
import com.example.mapper.SysUserMapper;
import com.example.util.JwtUtil;
import com.example.vo.LoginVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest extends BaseTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @MockBean
    private SysUserMapper sysUserMapper;

    private static final String TEST_PHONE = "13800000001";
    private static final String TEST_USERNAME = "testuser";
    private static final String TEST_PASSWORD = "Abcd1234";
    private static final String TEST_CAPTCHA = "1234";
    private static final String TEST_CAPTCHA_UUID = "uuid-123";
    private static final String TEST_CODE = "123456";

    private SysUser buildUser() {
        SysUser user = new SysUser();
        user.setId(1L);
        user.setUsername(TEST_USERNAME);
        user.setPassword(passwordEncoder.encode(TEST_PASSWORD));
        user.setPhone(TEST_PHONE);
        user.setRole("user");
        user.setStatus(1);
        return user;
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        // 默认 mock Redis: captcha 通过, sms 通过, 无锁定, 无黑名单
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        HashOperations<String, Object, Object> hashOps = mock(HashOperations.class);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(stringRedisTemplate.opsForHash()).thenReturn(hashOps);

        // captcha: 返回正确的验证码
        when(valueOps.get("auth:captcha:" + TEST_CAPTCHA_UUID)).thenReturn(TEST_CAPTCHA);
        // 登录失败次数: 0
        when(valueOps.get("auth:login:fail:" + TEST_USERNAME)).thenReturn("0");
        // sms code: 返回正确的验证码
        when(valueOps.get("auth:sms:code:" + TEST_PHONE)).thenReturn(TEST_CODE);
        // sms rate: 放行 (setIfAbsent = true)
        when(valueOps.setIfAbsent(contains("auth:sms:rate:"), eq("1"), any(Duration.class)))
                .thenReturn(true);
        // 没有重复提交
        when(valueOps.setIfAbsent(contains("auth:repeat:"), eq("1"), any(Duration.class)))
                .thenReturn(true);

        doNothing().when(stringRedisTemplate).delete(anyString());
    }

    @Nested
    @DisplayName("注册")
    class RegisterTests {

        @Test
        @DisplayName("正常注册 -> 成功返回 LoginVO")
        void shouldRegisterSuccessfully() {
            UserRegisterDTO dto = new UserRegisterDTO();
            dto.setUsername("newuser");
            dto.setPassword(TEST_PASSWORD);
            dto.setPhone("13800000002");
            dto.setVerifyCode(TEST_CODE);

            when(sysUserMapper.selectCount(any())).thenReturn(0L);
            when(sysUserMapper.insert(any(com.example.entity.SysUser.class))).thenReturn(1);

            LoginVO result = authService.register(dto);

            assertNotNull(result);
            assertNotNull(result.getAccessToken());
            assertNotNull(result.getRefreshToken());
            assertNotNull(result.getUserInfo());
            assertEquals("newuser", result.getUserInfo().getUsername());
        }

        @Test
        @DisplayName("手机号已注册 -> 抛出 BusinessException")
        void shouldThrowWhenPhoneExists() {
            UserRegisterDTO dto = new UserRegisterDTO();
            dto.setUsername("newuser");
            dto.setPassword(TEST_PASSWORD);
            dto.setPhone(TEST_PHONE);
            dto.setVerifyCode(TEST_CODE);

            when(sysUserMapper.selectCount(any())).thenReturn(1L);

            assertThrows(BusinessException.class, () -> authService.register(dto));
        }
    }

    @Nested
    @DisplayName("账号密码登录")
    class LoginTests {

        @Test
        @DisplayName("正确密码登录 -> 成功")
        void shouldLoginSuccessfully() {
            UserLoginDTO dto = new UserLoginDTO();
            dto.setUsername(TEST_USERNAME);
            dto.setPassword(TEST_PASSWORD);
            dto.setCaptchaCode(TEST_CAPTCHA);
            dto.setCaptchaUuid(TEST_CAPTCHA_UUID);

            when(sysUserMapper.selectOne(any())).thenReturn(buildUser());

            LoginVO result = authService.login(dto);

            assertNotNull(result);
            assertEquals(TEST_USERNAME, result.getUserInfo().getUsername());
        }

        @Test
        @DisplayName("错误密码登录 -> 抛出 BusinessException")
        void shouldThrowOnWrongPassword() {
            UserLoginDTO dto = new UserLoginDTO();
            dto.setUsername(TEST_USERNAME);
            dto.setPassword("WrongPass1");
            dto.setCaptchaCode(TEST_CAPTCHA);
            dto.setCaptchaUuid(TEST_CAPTCHA_UUID);

            when(sysUserMapper.selectOne(any())).thenReturn(buildUser());

            assertThrows(BusinessException.class, () -> authService.login(dto));
        }

        @Test
        @DisplayName("账号已禁用 -> 抛出 BusinessException")
        void shouldThrowOnDisabledAccount() {
            UserLoginDTO dto = new UserLoginDTO();
            dto.setUsername(TEST_USERNAME);
            dto.setPassword(TEST_PASSWORD);
            dto.setCaptchaCode(TEST_CAPTCHA);
            dto.setCaptchaUuid(TEST_CAPTCHA_UUID);

            SysUser disabled = buildUser();
            disabled.setStatus(0);
            when(sysUserMapper.selectOne(any())).thenReturn(disabled);

            assertThrows(BusinessException.class, () -> authService.login(dto));
        }
    }

    @Nested
    @DisplayName("短信验证码登录")
    class PhoneLoginTests {

        @Test
        @DisplayName("正确验证码登录 -> 成功")
        void shouldLoginByPhoneSuccessfully() {
            UserLoginByPhoneDTO dto = new UserLoginByPhoneDTO();
            dto.setPhone(TEST_PHONE);
            dto.setVerifyCode(TEST_CODE);

            when(sysUserMapper.selectOne(any())).thenReturn(buildUser());

            LoginVO result = authService.loginByPhone(dto);

            assertNotNull(result);
            assertNotNull(result.getAccessToken());
        }
    }

    @Nested
    @DisplayName("Token 管理")
    class TokenTests {

        @Test
        @DisplayName("refreshToken 刷新 -> 返回新 token")
        void shouldRefreshTokenSuccessfully() {
            SysUser user = buildUser();
            String refreshToken = jwtUtil.generateRefreshToken(user.getId());

            when(sysUserMapper.selectById(1L)).thenReturn(user);

            LoginVO result = authService.refresh("Bearer " + refreshToken);

            assertNotNull(result);
            assertNotNull(result.getAccessToken());
        }

        @Test
        @DisplayName("logout -> 正常执行不抛异常")
        void shouldLogoutSuccessfully() {
            SysUser user = buildUser();
            String accessToken = "Bearer " + jwtUtil.generateAccessToken(user.getId(), user.getUsername(), user.getRole());
            String refreshToken = "Bearer " + jwtUtil.generateRefreshToken(user.getId());

            assertDoesNotThrow(() -> authService.logout(accessToken, refreshToken));
        }
    }
}