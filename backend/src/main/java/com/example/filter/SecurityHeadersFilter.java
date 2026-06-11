package com.example.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 安全响应头过滤器 — 等保三级完整版。
 *
 * 为所有 HTTP 响应添加浏览器侧安全防护头，覆盖：
 * - X-Content-Type-Options（MIME 嗅探防护）
 * - X-Frame-Options（点击劫持防护）
 * - Content-Security-Policy（CSP，XSS/数据注入防护）
 * - Strict-Transport-Security（HSTS，强制 HTTPS）
 * - Referrer-Policy（引用策略）
 * - Permissions-Policy（权限策略）
 * - Cache-Control（缓存控制）
 */
@Component
public class SecurityHeadersFilter implements Filter {

    /**
     * CSP 策略：仅允许同源资源，禁止 inline script 和 eval。
     * 等保三级要求：禁止内联脚本，防止 XSS 攻击。
     */
    private static final String CSP_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self' 'wasm-unsafe-eval'",
            "style-src 'self' 'unsafe-inline'",
            "img-src 'self' data: blob: https:",
            "font-src 'self' data:",
            "connect-src 'self' wss: https:",
            "media-src 'self'",
            "object-src 'none'",
            "frame-ancestors 'none'",
            "form-action 'self'",
            "base-uri 'self'"
    );

    /** HSTS：强制浏览器在 max-age 秒内仅通过 HTTPS 访问 */
    private static final String HSTS_HEADER_VALUE = "max-age=31536000; includeSubDomains; preload";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        // ---- 等保三级必需安全头 ----

        // CSP 内容安全策略：防御 XSS 和数据注入
        httpResponse.setHeader("Content-Security-Policy", CSP_POLICY);

        // HSTS：仅对 HTTPS 请求下发（HTTP 请求不下发 HSTS 以防止误配置）
        if (httpRequest.isSecure()) {
            httpResponse.setHeader("Strict-Transport-Security", HSTS_HEADER_VALUE);
        }

        // 禁止 MIME 类型嗅探
        httpResponse.setHeader("X-Content-Type-Options", "nosniff");

        // 禁止被嵌入 iframe（防御点击劫持）
        httpResponse.setHeader("X-Frame-Options", "DENY");

        // 反射型 XSS 过滤器（设为 0 表示用 CSP 替代旧版浏览器过滤器）
        httpResponse.setHeader("X-XSS-Protection", "0");

        // 禁止浏览器缓存敏感数据
        httpResponse.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");

        // 引用策略：同源发送完整 Referer，跨域不发送
        httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // 权限策略：限制敏感 API
        httpResponse.setHeader("Permissions-Policy", "camera=(), microphone=(), geolocation=()");

        // 防范跨源资源策略（COEP/COOP/CORP）
        httpResponse.setHeader("Cross-Origin-Resource-Policy", "same-origin");
        httpResponse.setHeader("Cross-Origin-Opener-Policy", "same-origin");

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }
}