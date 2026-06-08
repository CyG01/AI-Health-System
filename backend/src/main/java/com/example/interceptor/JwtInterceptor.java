package com.example.interceptor;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.common.BusinessException;
import com.example.properties.JwtProperties;
import com.example.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * JWT 拦截器 — 校验 Token 合法性 + Redis 黑名单校验（防越权）
 */
@Component
public class JwtInterceptor implements HandlerInterceptor {

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;
    private final StringRedisTemplate stringRedisTemplate;

    public JwtInterceptor(JwtUtil jwtUtil,
                          JwtProperties jwtProperties,
                          StringRedisTemplate stringRedisTemplate) {
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authorization = request.getHeader(jwtProperties.getHeader());
        String token = jwtUtil.extractToken(authorization);
        if (token == null || token.isBlank()) {
            throw new BusinessException(401, "未登录或token已过期");
        }
        if (!jwtUtil.validateToken(token)) {
            throw new BusinessException(401, "未登录或token已过期");
        }
        if (!jwtUtil.isAccessToken(token)) {
            throw new BusinessException(401, "未登录或token已过期");
        }

        // 越权防护：检查 token 是否已被加入黑名单（退出登录后不可复用）
        String blacklistKey = TOKEN_BLACKLIST_PREFIX + token;
        Boolean isBlacklisted = stringRedisTemplate.hasKey(blacklistKey);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            throw new BusinessException(401, "token已失效，请重新登录");
        }

        request.setAttribute("userId", jwtUtil.getUserId(token));
        request.setAttribute("username", jwtUtil.getUsername(token));
        request.setAttribute("role", jwtUtil.getRole(token));
        return true;
    }
}
