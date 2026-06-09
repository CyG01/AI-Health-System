package com.example.dto;

import jakarta.validation.constraints.Pattern;

/**
 * 通知偏好更新请求
 */
public class NotificationPreferenceDTO {

    private Integer notificationEnabled;

    @Pattern(regexp = "^$|^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", message = "时间格式应为 HH:mm")
    private String reminderTime;

    private Integer notifyExercise;

    private Integer notifyDiet;

    private Integer notifyCheckin;

    @Pattern(regexp = "^$|^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", message = "时间格式应为 HH:mm")
    private String quietStart;

    @Pattern(regexp = "^$|^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$", message = "时间格式应为 HH:mm")
    private String quietEnd;

    public Integer getNotificationEnabled() { return notificationEnabled; }
    public void setNotificationEnabled(Integer notificationEnabled) { this.notificationEnabled = notificationEnabled; }

    public String getReminderTime() { return reminderTime; }
    public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }

    public Integer getNotifyExercise() { return notifyExercise; }
    public void setNotifyExercise(Integer notifyExercise) { this.notifyExercise = notifyExercise; }

    public Integer getNotifyDiet() { return notifyDiet; }
    public void setNotifyDiet(Integer notifyDiet) { this.notifyDiet = notifyDiet; }

    public Integer getNotifyCheckin() { return notifyCheckin; }
    public void setNotifyCheckin(Integer notifyCheckin) { this.notifyCheckin = notifyCheckin; }

    public String getQuietStart() { return quietStart; }
    public void setQuietStart(String quietStart) { this.quietStart = quietStart; }

    public String getQuietEnd() { return quietEnd; }
    public void setQuietEnd(String quietEnd) { this.quietEnd = quietEnd; }
}