package com.example.sdui;

import java.util.ArrayList;
import java.util.List;

/**
 * 运动卡片组件 — 展示单项运动任务的详情，支持打卡交互。
 * 支持结构化阶段拆分（热身/核心/放松）和场景标签。
 */
public class ExerciseCardWidget extends Widget {

    private String exerciseName;
    private Integer duration;
    private String intensity;
    private String videoUrl;
    private String instruction;
    private Boolean completed;
    private String checkinAction;
    private String scenarioTag;
    private List<ExercisePhaseWidget.Phase> phases = new ArrayList<>();
    private Integer completedPhases;

    public ExerciseCardWidget() {
        this.type = "exercise_card";
    }

    public String getExerciseName() {
        return exerciseName;
    }

    public void setExerciseName(String exerciseName) {
        this.exerciseName = exerciseName;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getIntensity() {
        return intensity;
    }

    public void setIntensity(String intensity) {
        this.intensity = intensity;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public String getCheckinAction() {
        return checkinAction;
    }

    public void setCheckinAction(String checkinAction) {
        this.checkinAction = checkinAction;
    }

    public String getScenarioTag() {
        return scenarioTag;
    }

    public void setScenarioTag(String scenarioTag) {
        this.scenarioTag = scenarioTag;
    }

    public List<ExercisePhaseWidget.Phase> getPhases() {
        return phases;
    }

    public void setPhases(List<ExercisePhaseWidget.Phase> phases) {
        this.phases = phases;
    }

    public Integer getCompletedPhases() {
        return completedPhases;
    }

    public void setCompletedPhases(Integer completedPhases) {
        this.completedPhases = completedPhases;
    }
}