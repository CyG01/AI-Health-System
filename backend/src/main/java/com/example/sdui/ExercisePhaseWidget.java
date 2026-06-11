package com.example.sdui;

import java.util.ArrayList;
import java.util.List;

/**
 * 运动阶段组件 — 展示运动任务的结构化拆分（热身/核心/放松），支持逐阶段打卡。
 */
public class ExercisePhaseWidget extends Widget {

    private String exerciseName;
    private Integer totalDuration;
    private String intensity;
    private String scenarioTag;
    private List<Phase> phases = new ArrayList<>();
    private Integer completedPhases;
    private String videoUrl;

    public ExercisePhaseWidget() {
        this.type = "exercise_phase";
    }

    public String getExerciseName() { return exerciseName; }
    public void setExerciseName(String exerciseName) { this.exerciseName = exerciseName; }

    public Integer getTotalDuration() { return totalDuration; }
    public void setTotalDuration(Integer totalDuration) { this.totalDuration = totalDuration; }

    public String getIntensity() { return intensity; }
    public void setIntensity(String intensity) { this.intensity = intensity; }

    public String getScenarioTag() { return scenarioTag; }
    public void setScenarioTag(String scenarioTag) { this.scenarioTag = scenarioTag; }

    public List<Phase> getPhases() { return phases; }
    public void setPhases(List<Phase> phases) { this.phases = phases; }

    public Integer getCompletedPhases() { return completedPhases; }
    public void setCompletedPhases(Integer completedPhases) { this.completedPhases = completedPhases; }

    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }

    /**
     * 运动子阶段。
     */
    public static class Phase {
        private String name;
        private String type;       // warmup / core / cooldown
        private Integer durationMinutes;
        private String instruction;
        private String heartRateZone;
        private Boolean completed;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public Integer getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(Integer durationMinutes) { this.durationMinutes = durationMinutes; }

        public String getInstruction() { return instruction; }
        public void setInstruction(String instruction) { this.instruction = instruction; }

        public String getHeartRateZone() { return heartRateZone; }
        public void setHeartRateZone(String heartRateZone) { this.heartRateZone = heartRateZone; }

        public Boolean getCompleted() { return completed; }
        public void setCompleted(Boolean completed) { this.completed = completed; }
    }
}