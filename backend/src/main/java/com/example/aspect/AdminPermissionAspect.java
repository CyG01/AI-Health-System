package com.example.aspect;

import com.example.annotation.AdminOnly;
import com.example.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AdminPermissionAspect {

    private static final Logger log = LoggerFactory.getLogger(AdminPermissionAspect.class);

    @Around("@within(com.example.annotation.AdminOnly) || @annotation(com.example.annotation.AdminOnly)")
    public Object checkAdmin(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        String userRole = (String) request.getAttribute("userRole");

        if (userRole == null || !"admin".equals(userRole)) {
            log.warn("无管理员权限访问: URI={}, role={}", request.getRequestURI(), userRole);
            throw new BusinessException(403, "无管理员权限");
        }

        return joinPoint.proceed();
    }
}
