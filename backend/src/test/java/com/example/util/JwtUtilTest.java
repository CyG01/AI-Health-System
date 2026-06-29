package com.example.util;

import com.example.properties.JwtProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret("test-secret-key-for-jwt-unit-test-env-only");
        jwtProperties.setAccessTokenExpire(3600000L);
        jwtProperties.setRefreshTokenExpire(604800000L);
        jwtUtil = new JwtUtil(jwtProperties);
    }

    @Test
    void shouldGenerateAndValidateAccessToken() {
        String token = jwtUtil.generateAccessToken(1L, "admin", "admin");
        assertNotNull(token);

        Long userId = jwtUtil.getUserId(token);
        assertEquals(1L, userId);

        String role = jwtUtil.getRole(token);
        assertEquals("admin", role);

        assertTrue(jwtUtil.isAccessToken(token));
    }

    @Test
    void shouldGenerateAndValidateRefreshToken() {
        String token = jwtUtil.generateRefreshToken(1L);
        assertNotNull(token);
        assertFalse(jwtUtil.isAccessToken(token));

        Long userId = jwtUtil.getUserId(token);
        assertEquals(1L, userId);
    }

    @Test
    void shouldDetectExpiredToken() {
        // 创建一个已过期的 accessToken（0ms 过期时间）
        jwtProperties.setAccessTokenExpire(0L);
        jwtUtil = new JwtUtil(jwtProperties);
        String token = jwtUtil.generateAccessToken(1L, "user", "user");

        assertFalse(jwtUtil.validateToken(token));
    }

    @Test
    void shouldRejectInvalidToken() {
        assertNull(jwtUtil.getUserId("invalid.token.string"));
        assertFalse(jwtUtil.validateToken("invalid.token.string"));
    }
}