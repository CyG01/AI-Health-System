package com.example.dto;

import jakarta.validation.constraints.Pattern;
import java.io.Serializable;

public class NotificationPreferenceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Pattern(regexp = "^(true|false)$", message = "notificationEnabled 必须为 true 或 false")
    private String notificationEnabled;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "reminderTime 格式必须为 HH:mm")
    private String reminderTime;

    @Pattern(regexp = "^(true|false)$", message = "notifyExercise 必须为 true 或 false")
    private String notifyExercise;

    @Pattern(regexp = "^(true|false)$", message = "notifyDiet 必须为 true 或 false")
    private String notifyDiet;

    @Pattern(regexp = "^(true|false)$", message = "notifyCheckin 必须为 true 或 false")
    private String notifyCheckin;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "quietStart 格式必须为 HH:mm")
    private String quietStart;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "quietEnd 格式必须为 HH:mm")
    private String quietEnd;

    public String getNotificationEnabled() { return notificationEnabled; }
    public void setNotificationEnabled(String notificationEnabled) { this.notificationEnabled = notificationEnabled; }

    public String getReminderTime() { return reminderTime; }
    public void setReminderTime(String reminderTime) { this.reminderTime = reminderTime; }

    public String getNotifyExercise() { return notifyExercise; }
    public void setNotifyExercise(String notifyExercise) { this.notifyExercise = notifyExercise; }

    public String getNotifyDiet() { return notifyDiet; }
    public void setNotifyDiet(String notifyDiet) { this.notifyDiet = notifyDiet; }

    public String getNotifyCheckin() { return notifyCheckin; }
    public void setNotifyCheckin(String notifyCheckin) { this.notifyCheckin = notifyCheckin; }

    public String getQuietStart() { return quietStart; }
    public void setQuietStart(String quietStart) { this.quietStart = quietStart; }

    public String getQuietEnd() { return quietEnd; }
    public void setQuietEnd(String quietEnd) { this.quietEnd = quietEnd; }
}