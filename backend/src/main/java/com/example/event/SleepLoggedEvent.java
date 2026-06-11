package com.example.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

/**
 * 睡眠记录完成事件。
 */
public class SleepLoggedEvent extends ApplicationEvent {

    private final Long userId;
    private final int sleepHours;
    private final int deepSleepMinutes;
    private final LocalDate sleepDate;

    public SleepLoggedEvent(Object source, Long userId, int sleepHours,
                             int deepSleepMinutes, LocalDate sleepDate) {
        super(source);
        this.userId = userId;
        this.sleepHours = sleepHours;
        this.deepSleepMinutes = deepSleepMinutes;
        this.sleepDate = sleepDate;
    }

    public Long getUserId() {
        return userId;
    }

    public int getSleepHours() {
        return sleepHours;
    }

    public int getDeepSleepMinutes() {
        return deepSleepMinutes;
    }

    public LocalDate getSleepDate() {
        return sleepDate;
    }

    /**
     * 睡眠不足是否需要提醒。
     */
    public boolean isInsufficientSleep() {
        return sleepHours < 6 || deepSleepMinutes < 60;
    }
}