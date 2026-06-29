package com.example.sdui;

/**
 * 通知提醒组件 — 展示系统通知或AI建议推送。
 */
public class NotificationWidget extends Widget {

    private String message;
    private String severity;
    private String actionUrl;
    private String actionLabel;
    private Boolean dismissible;

    public NotificationWidget() {
        this.type = "notification";
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public void setActionLabel(String actionLabel) {
        this.actionLabel = actionLabel;
    }

    public Boolean getDismissible() {
        return dismissible;
    }

    public void setDismissible(Boolean dismissible) {
        this.dismissible = dismissible;
    }
}