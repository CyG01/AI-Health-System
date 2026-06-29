package com.example.aspect;

import com.example.annotation.Trace;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 全链路追踪切面（Phase 3 可观测性）。
 *
 * 配合 @Trace 注解，自动收集方法调用延迟、参数/结果日志，
 * 同时导出延迟指标到 Micrometer/Prometheus。
 *
 * SkyWalking Agent 会自动基于当前 Trace ID 构建完整链路，
 * 本切面补充自定义 Span 属性和慢调用告警。
 */
@Slf4j
@Aspect
@Component
public class TraceAspect {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<String, Timer> timerCache = new ConcurrentHashMap<>();

    public TraceAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("@annotation(traceAnnotation)")
    public Object around(ProceedingJoinPoint joinPoint, Trace traceAnnotation) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String spanName = getSpanName(traceAnnotation, signature);

        // 是否记录参数
        if (traceAnnotation.recordArgs()) {
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                String argsSummary = Arrays.stream(args)
                        .map(this::summarize)
                        .limit(5)
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");
                log.debug("[Trace {}] args: {}", spanName, argsSummary);
            }
        }

        long startNano = System.nanoTime();
        long startMs = System.currentTimeMillis();
        Object result;

        try {
            result = joinPoint.proceed();
        } catch (Throwable ex) {
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNano);
            log.error("[Trace {}] FAILED after {}ms: {}",
                    spanName, elapsedMs, ex.getMessage());
            throw ex;
        }

        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNano);

        // 记录到 Micrometer
        getOrCreateTimer(spanName).record(elapsedMs, TimeUnit.MILLISECONDS);

        // 是否记录返回结果
        if (traceAnnotation.recordResult()) {
            String resultSummary = summarize(result);
            log.debug("[Trace {}] result: {}", spanName, resultSummary);
        }

        // 慢调用告警
        long threshold = traceAnnotation.slowThresholdMs();
        if (elapsedMs > threshold) {
            log.warn("[Trace {}] SLOW CALL: {}ms > threshold {}ms",
                    spanName, elapsedMs, threshold);
        }

        if (log.isDebugEnabled()) {
            log.debug("[Trace {}] completed in {}ms", spanName, elapsedMs);
        }

        return result;
    }

    private String getSpanName(Trace annotation, MethodSignature signature) {
        String name = annotation.spanName();
        if (name != null && !name.isBlank()) {
            return name;
        }
        return signature.getDeclaringType().getSimpleName() +
                "." + signature.getMethod().getName();
    }

    private Timer getOrCreateTimer(String spanName) {
        return timerCache.computeIfAbsent(spanName, n ->
                Timer.builder("trace_method_latency_seconds")
                        .description("Traced method execution latency")
                        .tag("method", n)
                        .register(meterRegistry)
        );
    }

    private String summarize(Object obj) {
        if (obj == null) return "null";
        String str = obj.toString();
        if (str.length() > 200) {
            return str.substring(0, 200) + "...";
        }
        return str;
    }
}