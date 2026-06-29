package com.example.vo;

import java.io.Serializable;

/**
 * 通知偏好设置
 */
public class NotificationPreferenceVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer notificationEnabled;
    private String reminderTime;
    private Integer notifyExercise;
    private Integer notifyDiet;
    private Integer notifyCheckin;
    private String quietStart;
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