package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户个性化提醒时间表实体类
 */
@TableName("user_reminder_schedule")
public class UserReminderSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 提醒类型：CHECKIN/WATER/EXERCISE/SLEEP/MEAL/MEDICATION */
    private String reminderType;

    /** 提醒时间 HH:mm（用户当地时间） */
    private String reminderTime;

    /** 重复星期（1=周一...7=周日，逗号分隔） */
    private String repeatDays;

    /** 是否启用 */
    private Integer isEnabled;

    /** 贪睡时长（分钟） */
    private Integer snoozeMinutes;

    /** 仅震动 0=否 1=是 */
    private Integer vibrationOnly;

    /** 自定义提醒消息 */
    private String customMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    // --- getters/setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getReminderType() { return reminderType; }
    public void setReminderType(String reminderType) { this.reminderType = reminderType; }

    public String getReminderTime() { return reminderTime; }
    public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }

    public String getRepeatDays() { return repeatDays; }
    public void setRepeatDays(String repeatDays) { this.repeatDays = repeatDays; }

    public Integer getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Integer isEnabled) { this.isEnabled = isEnabled; }

    public Integer getSnoozeMinutes() { return snoozeMinutes; }
    public void setSnoozeMinutes(Integer snoozeMinutes) { this.snoozeMinutes = snoozeMinutes; }

    public Integer getVibrationOnly() { return vibrationOnly; }
    public void setVibrationOnly(Integer vibrationOnly) { this.vibrationOnly = vibrationOnly; }

    public String getCustomMessage() { return customMessage; }
    public void setCustomMessage(String customMessage) { this.customMessage = customMessage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
