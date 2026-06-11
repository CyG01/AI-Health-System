package com.example.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class BloodSugarVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private LocalDate recordDate;
    private LocalTime recordTime;
    private String measureType;
    private BigDecimal glucoseValue;
    private String note;
    private Integer abnormalFlag;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public LocalTime getRecordTime() { return recordTime; }
    public void setRecordTime(LocalTime recordTime) { this.recordTime = recordTime; }
    public String getMeasureType() { return measureType; }
    public void setMeasureType(String measureType) { this.measureType = measureType; }
    public BigDecimal getGlucoseValue() { return glucoseValue; }
    public void setGlucoseValue(BigDecimal glucoseValue) { this.glucoseValue = glucoseValue; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public Integer getAbnormalFlag() { return abnormalFlag; }
    public void setAbnormalFlag(Integer abnormalFlag) { this.abnormalFlag = abnormalFlag; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}