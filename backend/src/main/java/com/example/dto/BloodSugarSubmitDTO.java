package com.example.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class BloodSugarSubmitDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private LocalDate recordDate;

    private LocalTime recordTime;

    @NotNull
    private String measureType;

    @NotNull
    @DecimalMin("0.1") @DecimalMax("50.0")
    private BigDecimal glucoseValue;

    private String note;

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
}