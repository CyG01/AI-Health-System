package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("body_measurement")
public class BodyMeasurement implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate recordDate;

    private BigDecimal waist;

    private BigDecimal hip;

    private BigDecimal chest;

    private BigDecimal thigh;

    private BigDecimal arm;

    private BigDecimal bodyFatRate;

    private String note;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    @TableLogic
    private Integer isDeleted;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public LocalDate getRecordDate() { return recordDate; }
    public void setRecordDate(LocalDate recordDate) { this.recordDate = recordDate; }
    public BigDecimal getWaist() { return waist; }
    public void setWaist(BigDecimal waist) { this.waist = waist; }
    public BigDecimal getHip() { return hip; }
    public void setHip(BigDecimal hip) { this.hip = hip; }
    public BigDecimal getChest() { return chest; }
    public void setChest(BigDecimal chest) { this.chest = chest; }
    public BigDecimal getThigh() { return thigh; }
    public void setThigh(BigDecimal thigh) { this.thigh = thigh; }
    public BigDecimal getArm() { return arm; }
    public void setArm(BigDecimal arm) { this.arm = arm; }
    public BigDecimal getBodyFatRate() { return bodyFatRate; }
    public void setBodyFatRate(BigDecimal bodyFatRate) { this.bodyFatRate = bodyFatRate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Integer getIsDeleted() { return isDeleted; }
    public void setIsDeleted(Integer isDeleted) { this.isDeleted = isDeleted; }
}