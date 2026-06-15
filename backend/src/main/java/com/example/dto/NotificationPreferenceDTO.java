package com.example.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;

public class NotificationPreferenceDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Min(value = 0, message = "notificationEnabled 必须为 0 或 1")
    @Max(value = 1, message = "notificationEnabled 必须为 0 或 1")
    private Integer notificationEnabled;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "reminderTime 格式必须为 HH:mm")
    private String reminderTime;

    @Min(value = 0, message = "notifyExercise 必须为 0 或 1")
    @Max(value = 1, message = "notifyExercise 必须为 0 或 1")
    private Integer notifyExercise;

    @Min(value = 0, message = "notifyDiet 必须为 0 或 1")
    @Max(value = 1, message = "notifyDiet 必须为 0 或 1")
    private Integer notifyDiet;

    @Min(value = 0, message = "notifyCheckin 必须为 0 或 1")
    @Max(value = 1, message = "notifyCheckin 必须为 0 或 1")
    private Integer notifyCheckin;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "quietStart 格式必须为 HH:mm")
    private String quietStart;

    @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "quietEnd 格式必须为 HH:mm")
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