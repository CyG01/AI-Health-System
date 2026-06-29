package com.example.aspect;

import com.example.agent.model.ToolCallRecord;
import com.example.agent.tool.ToolCallContext;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Tool 调用拦截切面。
 * 拦截所有标注 @Tool 的方法，记录调用参数、结果、耗时、异常信息。
 * <p>
 * 与 LangChain4j 集成：当模型通过 Function Calling 触发 Tool 时，
 * 本切面自动捕获调用信息并写入 ToolCallContext（ThreadLocal），
 * 供上层（PlanGenerateV2Service / AgentOrchestrator）读取后填充到 AiAgentResponse.toolCalls。
 */
@Slf4j
@Aspect
@Component
public class ToolCallAspect {

    /**
     * 拦截所有标注 @Tool 的方法。
     */
    @Around("@annotation(tool)")
    public Object aroundToolMethod(ProceedingJoinPoint joinPoint, Tool tool) throws Throwable {
        long startTime = System.currentTimeMillis();
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        String toolName = method.getName();
        String description = (tool.value() != null && tool.value().length > 0)
                ? String.join(", ", tool.value()) : "";
        Map<String, Object> params = extractParameters(method, joinPoint.getArgs());

        boolean success = true;
        String resultSummary = null;
        String errorMessage = null;

        try {
            Object result = joinPoint.proceed();
            resultSummary = truncate(String.valueOf(result), 500);
            log.info("[ToolCall] {} 成功 | 参数={} | 耗时={}ms | 结果摘要={}",
                    toolName, maskSensitive(params), System.currentTimeMillis() - startTime, resultSummary);
            return result;
        } catch (Exception e) {
            success = false;
            errorMessage = e.getClass().getSimpleName() + ": " + truncate(e.getMessage(), 200);
            resultSummary = null;
            log.error("[ToolCall] {} 失败 | 参数={} | 耗时={}ms | 异常={}",
                    toolName, maskSensitive(params), System.currentTimeMillis() - startTime, errorMessage, e);
            throw e;  // 原样抛出，让 LangChain4j 将异常信息传给模型
        } finally {
            long latencyMs = System.currentTimeMillis() - startTime;

            ToolCallRecord record = ToolCallRecord.builder()
                    .toolName(toolName)
                    .description(description)
                    .parameters(maskSensitive(params))
                    .resultSummary(resultSummary)
                    .success(success)
                    .errorMessage(errorMessage)
                    .latencyMs(latencyMs)
                    .timestamp(LocalDateTime.now())
                    .build();

            ToolCallContext.addRecord(record);
        }
    }

    /**
     * 从方法签名和实参中提取参数名 → 值映射。
     */
    private Map<String, Object> extractParameters(Method method, Object[] args) {
        Map<String, Object> params = new LinkedHashMap<>();
        Parameter[] methodParams = method.getParameters();
        for (int i = 0; i < methodParams.length; i++) {
            String paramName = getParamName(methodParams[i]);
            Object paramValue = args[i];
            // userId 自动脱敏
            if ("userId".equals(paramName) && paramValue instanceof Long) {
                Long uid = (Long) paramValue;
                paramValue = uid == null ? null : "uid:" + (uid % 10000);  // 只保留后4位映射
            }
            params.put(paramName, paramValue);
        }
        return params;
    }

    /**
     * 获取参数名：优先 @P 注解值，其次反射参数名。
     */
    private String getParamName(Parameter parameter) {
        P pAnnotation = parameter.getAnnotation(P.class);
        if (pAnnotation != null && !pAnnotation.value().isBlank()) {
            return pAnnotation.value();
        }
        return parameter.getName();
    }

    /**
     * 对敏感参数（如 userId）进行脱敏。
     */
    private Map<String, Object> maskSensitive(Map<String, Object> params) {
        Map<String, Object> masked = new LinkedHashMap<>(params);
        // userId 已在 extractParameters 中处理
        // 扩展点：可在此方法中对其他敏感字段脱敏
        return masked;
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen) + "...(截断)";
    }
}