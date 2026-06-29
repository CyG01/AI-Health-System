package com.example.event;

import org.springframework.context.ApplicationEvent;

import java.time.LocalDate;

/**
 * 打卡完成事件。
 * CheckinServiceImpl 打卡完成后发布此事件。
 */
public class CheckinCompletedEvent extends ApplicationEvent {

    private final Long userId;
    private final LocalDate date;
    private final double exerciseCompletionRate;
    private final double dietCompletionRate;

    public CheckinCompletedEvent(Object source, Long userId, LocalDate date,
                                  double exerciseCompletionRate, double dietCompletionRate) {
        super(source);
        this.userId = userId;
        this.date = date;
        this.exerciseCompletionRate = exerciseCompletionRate;
        this.dietCompletionRate = dietCompletionRate;
    }

    public Long getUserId() {
        return userId;
    }

    public LocalDate getDate() {
        return date;
    }

    public double getExerciseCompletionRate() {
        return exerciseCompletionRate;
    }

    public double getDietCompletionRate() {
        return dietCompletionRate;
    }

    /**
     * 连续低完成率是否需要触发计划调整。
     */
    public boolean needsPlanAdjustment() {
        return exerciseCompletionRate < 30.0 && dietCompletionRate < 30.0;
    }
}