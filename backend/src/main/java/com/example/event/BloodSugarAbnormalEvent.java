package com.example.event;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

/**
 * 血糖异常事件。用于触发跨模块告警。
 */
public class BloodSugarAbnormalEvent extends ApplicationEvent {

    private final Long userId;
    private final BigDecimal glucoseValue;
    private final int abnormalFlag; // 1=偏高 2=偏低
    private final Long recordId;

    public BloodSugarAbnormalEvent(Object source, Long userId, BigDecimal glucoseValue,
                                    int abnormalFlag, Long recordId) {
        super(source);
        this.userId = userId;
        this.glucoseValue = glucoseValue;
        this.abnormalFlag = abnormalFlag;
        this.recordId = recordId;
    }

    public Long getUserId() { return userId; }
    public BigDecimal getGlucoseValue() { return glucoseValue; }
    public int getAbnormalFlag() { return abnormalFlag; }
    public Long getRecordId() { return recordId; }
}