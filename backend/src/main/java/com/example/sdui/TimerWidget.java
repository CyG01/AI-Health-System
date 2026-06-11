package com.example.sdui;

/**
 * 计时器组件 — 用于运动计时、休息倒计时等场景。
 */
public class TimerWidget extends Widget {

    private Integer totalSeconds;
    private String timerType;
    private String startAction;
    private String pauseAction;
    private String resetAction;

    public TimerWidget() {
        this.type = "timer";
    }

    public Integer getTotalSeconds() {
        return totalSeconds;
    }

    public void setTotalSeconds(Integer totalSeconds) {
        this.totalSeconds = totalSeconds;
    }

    public String getTimerType() {
        return timerType;
    }

    public void setTimerType(String timerType) {
        this.timerType = timerType;
    }

    public String getStartAction() {
        return startAction;
    }

    public void setStartAction(String startAction) {
        this.startAction = startAction;
    }

    public String getPauseAction() {
        return pauseAction;
    }

    public void setPauseAction(String pauseAction) {
        this.pauseAction = pauseAction;
    }

    public String getResetAction() {
        return resetAction;
    }

    public void setResetAction(String resetAction) {
        this.resetAction = resetAction;
    }
}