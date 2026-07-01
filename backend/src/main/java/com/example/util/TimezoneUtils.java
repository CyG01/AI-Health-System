package com.example.util;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Set;

/**
 * 时区工具类
 * 支持用户时区与服务器时区的转换
 */
public class TimezoneUtils {

    /** 默认时区：Asia/Shanghai */
    public static final String DEFAULT_TIMEZONE = "Asia/Shanghai";

    /** 支持的时区列表（可扩展） */
    private static final Set<String> SUPPORTED_TIMEZONES = Set.of(
            "Asia/Shanghai", "Asia/Tokyo", "Asia/Singapore", "Asia/Hong_Kong",
            "Asia/Seoul", "Asia/Bangkok", "Asia/Dubai", "Asia/Kolkata",
            "Europe/London", "Europe/Paris", "Europe/Berlin", "Europe/Moscow",
            "America/New_York", "America/Los_Angeles", "America/Chicago",
            "America/Toronto", "Australia/Sydney", "Pacific/Auckland"
    );

    private TimezoneUtils() {}

    /**
     * 验证时区ID是否合法
     */
    public static boolean isValidTimezone(String timezoneId) {
        if (timezoneId == null || timezoneId.isBlank()) {
            return false;
        }
        try {
            ZoneId.of(timezoneId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取安全的时区ID（非法则返回默认）
     */
    public static String safeTimezone(String timezoneId) {
        return isValidTimezone(timezoneId) ? timezoneId : DEFAULT_TIMEZONE;
    }

    /**
     * 将用户当地时间转换为服务器时间戳（毫秒）
     *
     * @param userLocalTime 用户当地的日期时间
     * @param userTimezone  用户时区ID
     * @return 服务器时间戳（毫秒）
     */
    public static long toServerTimestamp(LocalDateTime userLocalTime, String userTimezone) {
        ZoneId userZone = ZoneId.of(safeTimezone(userTimezone));
        ZoneId serverZone = ZoneId.systemDefault();

        ZonedDateTime userZoned = userLocalTime.atZone(userZone);
        ZonedDateTime serverZoned = userZoned.withZoneSameInstant(serverZone);

        return serverZoned.toInstant().toEpochMilli();
    }

    /**
     * 将用户当地的"HH:mm"时间转换为今天对应的服务器时间戳
     * 如果今天的时间已过，则返回明天的
     *
     * @param userTimeStr 用户当地时间 HH:mm
     * @param userTimezone 用户时区ID
     * @return 服务器时间戳（毫秒）
     */
    public static long calculateNextReminderTimestamp(String userTimeStr, String userTimezone) {
        ZoneId userZone = ZoneId.of(safeTimezone(userTimezone));
        ZoneId serverZone = ZoneId.systemDefault();

        // 解析用户当地时间
        LocalTime userTime = LocalTime.parse(userTimeStr, DateTimeFormatter.ofPattern("HH:mm"));

        // 获取用户当地今天的日期
        LocalDate userToday = LocalDate.now(userZone);
        LocalDateTime userLocalDateTime = LocalDateTime.of(userToday, userTime);

        // 转换为服务器时间
        ZonedDateTime userZoned = userLocalDateTime.atZone(userZone);
        ZonedDateTime serverZoned = userZoned.withZoneSameInstant(serverZone);

        // 如果今天的时间已过，则用明天的
        if (serverZoned.toInstant().isBefore(Instant.now())) {
            userLocalDateTime = userLocalDateTime.plusDays(1);
            userZoned = userLocalDateTime.atZone(userZone);
            serverZoned = userZoned.withZoneSameInstant(serverZone);
        }

        return serverZoned.toInstant().toEpochMilli();
    }

    /**
     * 获取用户当地当前时间
     */
    public static LocalDateTime getUserLocalNow(String userTimezone) {
        return LocalDateTime.now(ZoneId.of(safeTimezone(userTimezone)));
    }

    /**
     * 获取用户当地当前日期
     */
    public static LocalDate getUserLocalDate(String userTimezone) {
        return LocalDate.now(ZoneId.of(safeTimezone(userTimezone)));
    }

    /**
     * 计算用户当地时间与服务器时间的小时差
     */
    public static int getHourOffset(String userTimezone) {
        ZoneId userZone = ZoneId.of(safeTimezone(userTimezone));
        ZoneId serverZone = ZoneId.systemDefault();

        ZonedDateTime userNow = ZonedDateTime.now(userZone);
        ZonedDateTime serverNow = userNow.withZoneSameInstant(serverZone);

        return Duration.between(serverNow, userNow).toHoursPart();
    }

    /**
     * 判断用户当地今天是否是指定星期几
     *
     * @param userTimezone 用户时区
     * @param dayOfWeek    星期几（1=周一...7=周日）
     */
    public static boolean isUserLocalDayOfWeek(String userTimezone, int dayOfWeek) {
        LocalDate userDate = getUserLocalDate(userTimezone);
        return userDate.getDayOfWeek().getValue() == dayOfWeek;
    }

    /**
     * 格式化用户当地时间为字符串
     */
    public static String formatUserLocalTime(String userTimezone, String pattern) {
        return DateTimeFormatter.ofPattern(pattern)
                .format(LocalDateTime.now(ZoneId.of(safeTimezone(userTimezone))));
    }
}
