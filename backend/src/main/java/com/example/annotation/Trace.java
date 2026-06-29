package com.example.annotation;

import java.lang.annotation.*;

/**
 * 全链路追踪注解（Phase 3 可观测性）。
 *
 * 标记关键方法，配合 SkyWalking Agent 或自定义 TraceAspect，
 * 在调用链中自动记录方法耗时、参数摘要、返回值状态。
 *
 * 使用方式：
 * <pre>{@code
 * @Trace(spanName = "model-router-chat", recordArgs = false, recordResult = false)
 * public String chat(List<Map<String, String>> messages, String scenario) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Trace {

    /** Span 名称（默认使用方法全限定名） */
    String spanName() default "";

    /** 是否记录方法参数（含敏感数据的方法应设为 false） */
    boolean recordArgs() default false;

    /** 是否记录返回值（大文本方法应设为 false） */
    boolean recordResult() default false;

    /** 延迟阈值（毫秒），超过时输出 WARN 日志 */
    long slowThresholdMs() default 3000;
}