package com.example.interceptor;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import com.example.common.BusinessException;
import com.example.properties.JwtProperties;
import com.example.util.JwtUtil;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final JwtProperties jwtProperties;

    public JwtInterceptor(JwtUtil jwtUtil, JwtProperties jwtProperties) {
        this.jwtUtil = jwtUtil;
        this.jwtProperties = jwtProperties;
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

        request.setAttribute("userId", jwtUtil.getUserId(token));
        request.setAttribute("username", jwtUtil.getUsername(token));
        request.setAttribute("role", jwtUtil.getRole(token));
        return true;
    }
}
