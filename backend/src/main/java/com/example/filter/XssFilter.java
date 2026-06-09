package com.example.filter;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * XSS保护 - Servlet Filter层
 * 对query参数和form参数进行HTML转义。
 * 与JacksonXssConfig分工：Filter层处理query/form参数，Jackson层处理JSON body。
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class XssFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        filterChain.doFilter(new XssHttpServletRequestWrapper(request), response);
    }
}
