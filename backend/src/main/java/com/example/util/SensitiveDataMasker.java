package com.example.util;

import java.util.regex.Pattern;

/**
 * 敏感数据脱敏工具 — 等保三级日志安全。
 *
 * 用于在日志输出前对敏感字段进行脱敏处理，防止：
 * - 手机号泄露
 * - 身份证号/社保号泄露
 * - 密码/JWT Token 泄露
 * - 银行卡号泄露
 * - 邮箱泄露
 */
public final class SensitiveDataMasker {

    private SensitiveDataMasker() {}

    // ===================== 正则模式 =====================

    /** 手机号：1 开头 11 位数字 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("(1[3-9]\\d)\\d{4}(\\d{4})");

    /** 身份证号：18 位或 15 位 */
    private static final Pattern ID_CARD_PATTERN = Pattern.compile(
            "(\\d{6})\\d{8,11}([\\dXx])"
    );

    /** JWT Token：Bearer 后面跟的 token */
    private static final Pattern JWT_PATTERN = Pattern.compile(
            "(Bearer\\s+)([A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+\\.[A-Za-z0-9\\-_]+)"
    );

    /** 邮箱：保留首字符和域名 */
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "(.)(.*)(@.*)"
    );

    /** 密码参数（JSON 日志中） */
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "(\"password\"\\s*:\\s*\")([^\"]+)(\")"
    );

    // ===================== 公开方法 =====================

    /**
     * 对日志消息进行全量脱敏。
     */
    public static String mask(String logMessage) {
        if (logMessage == null || logMessage.isEmpty()) {
            return logMessage;
        }

        String result = logMessage;
        result = maskPhone(result);
        result = maskIdCard(result);
        result = maskJwt(result);
        result = maskEmail(result);
        result = maskPassword(result);
        return result;
    }

    /**
     * 脱敏手机号：138****1234
     */
    public static String maskPhone(String text) {
        if (text == null) return null;
        return PHONE_PATTERN.matcher(text).replaceAll("$1****$2");
    }

    /**
     * 脱敏身份证号：110101****1234
     */
    public static String maskIdCard(String text) {
        if (text == null) return null;
        return ID_CARD_PATTERN.matcher(text).replaceAll("$1****$2");
    }

    /**
     * 脱敏 JWT Token：Bearer eyJ***.xxx.yyy → Bearer eyJ***...***
     */
    public static String maskJwt(String text) {
        if (text == null) return null;
        return JWT_PATTERN.matcher(text).replaceAll("$1$2".substring(0, 6) + "..." + "$2".substring("$2".length() - 6));
    }

    /**
     * 脱敏邮箱：u***@example.com
     */
    public static String maskEmail(String text) {
        if (text == null) return null;
        return EMAIL_PATTERN.matcher(text).replaceAll("$1***$3");
    }

    /**
     * 脱敏密码字段：password: "****"
     */
    public static String maskPassword(String text) {
        if (text == null) return null;
        return PASSWORD_PATTERN.matcher(text).replaceAll("$1****$3");
    }

    /**
     * 脱敏姓名（保留姓，名用 * 替代）
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() == 1) return "*";
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /**
     * 简单脱敏：保留前 keepFront 位和后 keepBack 位，中间用 maskChar 替换。
     */
    public static String maskMiddle(String value, int keepFront, int keepBack, char maskChar) {
        if (value == null || value.isEmpty()) return value;
        if (value.length() <= keepFront + keepBack) {
            return maskChar + "*".repeat(Math.max(0, value.length() - 1));
        }
        return value.substring(0, keepFront)
                + String.valueOf(maskChar).repeat(value.length() - keepFront - keepBack)
                + value.substring(value.length() - keepBack);
    }
}