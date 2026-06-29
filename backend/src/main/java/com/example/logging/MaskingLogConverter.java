package com.example.logging;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.example.util.SensitiveDataMasker;

/**
 * Logback 日志脱敏转换器 — 全局日志安全兜底。
 * <p>
 * 注册到 logback-spring.xml 后，所有经过 Logback 输出的日志消息
 * 都会自动通过 {@link SensitiveDataMasker#mask(String)} 进行脱敏处理。
 * <p>
 * 配合 {@code LogMaskingAspect} 形成双重保障：
 * <ol>
 *   <li>AOP 切面：在 Controller 层结构化地记录脱敏请求/响应日志</li>
 *   <li>本转换器：兜底拦截所有 log 输出中的敏感数据</li>
 * </ol>
 * <p>
 * 使用方式（logback-spring.xml）：
 * <pre>{@code
 * <conversionRule conversionWord="maskedMsg"
 *                 converterClass="com.example.logging.MaskingLogConverter"/>
 * <pattern>%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %maskedMsg%n</pattern>
 * }</pre>
 *
 * @see SensitiveDataMasker
 */
public class MaskingLogConverter extends ClassicConverter {

    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null || message.isEmpty()) {
            return message;
        }
        return SensitiveDataMasker.mask(message);
    }
}
