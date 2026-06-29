package com.example.aspect;

import com.example.common.Result;
import com.example.util.SensitiveDataMasker;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 日志脱敏切面 — 等保三级日志安全合规。
 * <p>
 * 拦截所有 Controller 方法，在请求进入和响应时自动对日志中的敏感数据进行脱敏：
 * <ul>
 *   <li>手机号：138****1234</li>
 *   <li>身份证号：110101****X</li>
 *   <li>JWT Token：Bearer eyJhb...***</li>
 *   <li>邮箱：u***@example.com</li>
 *   <li>密码字段：password: "****"</li>
 * </ul>
 * <p>
 * 注意：本切面仅脱敏日志输出，不会修改实际返回给客户端的响应数据。
 *
 * @see SensitiveDataMasker
 */
@Slf4j
@Aspect
@Component
@Order(100) // 低优先级，在其他切面（限流、防重等）之后执行
public class LogMaskingAspect {

    /** 日志参数最大长度，超出部分截断 */
    private static final int MAX_LOG_LENGTH = 500;

    /**
     * 切点：匹配 com.example.controller 包及子包下所有类的全部方法。
     */
    @Pointcut("execution(* com.example.controller..*(..))")
    public void controllerPointcut() {
        // pointcut declaration — no body
    }

    /**
     * 环绕通知：记录脱敏后的请求参数和响应结果。
     */
    @Around("controllerPointcut()")
    public Object maskSensitiveData(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getMethod().getName();
        String tag = className + "." + methodName;

        // ---- 记录请求参数（脱敏） ----
        if (log.isInfoEnabled()) {
            String maskedArgs = buildMaskedArgs(signature.getParameterNames(), joinPoint.getArgs());
            log.info("[Request] {} | params: {}", tag, maskedArgs);
        }

        // ---- 执行目标方法 ----
        long startMs = System.currentTimeMillis();
        Object result;
        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            long elapsed = System.currentTimeMillis() - startMs;
            log.warn("[Response] {} | FAILED after {}ms | error: {}",
                    tag, elapsed, SensitiveDataMasker.mask(ex.getMessage()));
            throw ex;
        }

        // ---- 记录响应结果（脱敏） ----
        long elapsed = System.currentTimeMillis() - startMs;
        if (log.isInfoEnabled()) {
            String maskedResult = summarizeResult(result);
            log.info("[Response] {} | {}ms | result: {}", tag, elapsed, maskedResult);
        }

        return result;
    }

    /**
     * 构建脱敏后的参数摘要。
     * 跳过 HttpServletRequest / Response / MultipartFile 等不可序列化对象。
     */
    private String buildMaskedArgs(String[] paramNames, Object[] args) {
        if (paramNames == null || args == null || args.length == 0) {
            return "[]";
        }

        Map<String, Object> paramMap = new LinkedHashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            Object arg = args[i];

            // 跳过不可序列化的 Servlet 对象
            if (arg instanceof ServletRequest) {
                paramMap.put(paramNames[i], "<ServletRequest>");
                continue;
            }
            if (arg instanceof ServletResponse) {
                paramMap.put(paramNames[i], "<ServletResponse>");
                continue;
            }
            if (arg instanceof MultipartFile file) {
                paramMap.put(paramNames[i],
                        "<file: " + file.getOriginalFilename() + ", " + file.getSize() + "B>");
                continue;
            }

            paramMap.put(paramNames[i], arg);
        }

        String raw = paramMap.toString();
        return SensitiveDataMasker.mask(truncate(raw, MAX_LOG_LENGTH));
    }

    /**
     * 对返回结果进行摘要并脱敏。
     * 如果返回值为 Result 包装，提取 data 部分；否则取 toString。
     */
    private String summarizeResult(Object result) {
        if (result == null) {
            return "null";
        }

        String raw;
        if (result instanceof Result<?> r) {
            Object data = r.getData();
            if (data == null) {
                raw = "Result{code=" + r.getCode() + ", msg=" + r.getMsg() + ", data=null}";
            } else {
                raw = "Result{code=" + r.getCode() + ", data=" + data.toString() + "}";
            }
        } else {
            raw = result.toString();
        }

        return SensitiveDataMasker.mask(truncate(raw, MAX_LOG_LENGTH));
    }

    /**
     * 截断字符串，超长时追加标记。
     */
    private static String truncate(String text, int maxLength) {
        if (text == null) return "null";
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...(truncated)";
    }
}
