package com.example.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 安全响应头过滤器。
 * 为所有 HTTP 响应添加浏览器侧安全防护头。
 */
@Component
public class SecurityHeadersFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 禁止 MIME 类型嗅探
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        // 禁止被嵌入 iframe（防御点击劫持）
        httpResponse.setHeader("X-Frame-Options", "DENY");
        // 反射型 XSS 过滤器（现代浏览器默认开启，设为 0 表示不启用旧版过滤，用 CSP 替代）
        httpResponse.setHeader("X-XSS-Protection", "0");
        // 禁止浏览器缓存敏感数据（API 响应默认不缓存）
        httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
        // 引用策略：同源请求发送完整 Referer，跨域不发送
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
        // 权限策略：限制敏感 API 使用
        httpResponse.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }
}