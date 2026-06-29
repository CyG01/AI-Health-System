package com.example.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * CSRF 防护过滤器 — 等保三级。
 *
 * 策略：对于状态变更请求（POST/PUT/DELETE/PATCH），验证以下条件之一：
 * 1. 携带有效的 JWT Authorization 头（Bearer Token 天然防 CSRF）
 * 2. 携带 X-Requested-With 头（AJAX 请求标记）
 * 3. 携带自定义 X-CSRF-Token 头
 *
 * 注意：基于 JWT Bearer Token 的 API 天然不受传统 CSRF 攻击影响，
 * 因为浏览器不会自动附加 Authorization 头到跨域请求。
 * 此过滤器作为 defense-in-depth 补充防护。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
public class CsrfProtectionFilter extends OncePerRequestFilter {

    private static final Set<String> SAFE_METHODS = Set.of(
            HttpMethod.GET.name(),
            HttpMethod.HEAD.name(),
            HttpMethod.OPTIONS.name(),
            HttpMethod.TRACE.name()
    );

    /** 允许跳过 CSRF 检查的路径前缀 */
    private static final Set<String> CSRF_EXEMPT_PATHS = Set.of(
            "/api/auth/",
            "/api/public/",
            "/actuator/health",
            "/swagger",
            "/v3/api-docs",
            "/webjars/"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String method = request.getMethod().toUpperCase();
        String path = request.getRequestURI();

        // 安全方法（GET/HEAD/OPTIONS）跳过检查
        if (SAFE_METHODS.contains(method)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 豁免路径跳过
        for (String exempt : CSRF_EXEMPT_PATHS) {
            if (path.startsWith(exempt)) {
                filterChain.doFilter(request, response);
                return;
            }
        }

        // 状态变更请求：验证 CSRF 防护
        boolean hasValidAuth = request.getHeader("Authorization") != null
                && request.getHeader("Authorization").startsWith("Bearer ");

        boolean hasXRequestedWith = "XMLHttpRequest".equals(
                request.getHeader("X-Requested-With"));

        boolean hasCsrfToken = request.getHeader("X-CSRF-Token") != null;

        if (hasValidAuth || hasXRequestedWith || hasCsrfToken) {
            filterChain.doFilter(request, response);
            return;
        }

        // CSRF 检测失败
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"code\":403,\"message\":\"CSRF validation failed\"}"
        );
    }
}