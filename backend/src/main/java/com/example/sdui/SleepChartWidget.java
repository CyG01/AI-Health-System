package com.example.sdui;

import java.util.ArrayList;
import java.util.List;

/**
 * 睡眠图表组件 — 展示睡眠质量评分、各阶段时长和改善建议。
 */
public class SleepChartWidget extends Widget {

    private Integer sleepScore;
    private Double totalHours;
    private Double deepSleepHours;
    private Double lightSleepHours;
    private Double remHours;
    private List<SleepPhase> phases = new ArrayList<>();
    private String suggestion;

    public SleepChartWidget() {
        this.type = "sleep_chart";
    }

    public Integer getSleepScore() { return sleepScore; }
    public void setSleepScore(Integer sleepScore) { this.sleepScore = sleepScore; }

    public Double getTotalHours() { return totalHours; }
    public void setTotalHours(Double totalHours) { this.totalHours = totalHours; }

    public Double getDeepSleepHours() { return deepSleepHours; }
    public void setDeepSleepHours(Double deepSleepHours) { this.deepSleepHours = deepSleepHours; }

    public Double getLightSleepHours() { return lightSleepHours; }
    public void setLightSleepHours(Double lightSleepHours) { this.lightSleepHours = lightSleepHours; }

    public Double getRemHours() { return remHours; }
    public void setRemHours(Double remHours) { this.remHours = remHours; }

    public List<SleepPhase> getPhases() { return phases; }
    public void setPhases(List<SleepPhase> phases) { this.phases = phases; }

    public String getSuggestion() { return suggestion; }
    public void setSuggestion(String suggestion) { this.suggestion = suggestion; }

    /**
     * 睡眠阶段数据。
     */
    public static class SleepPhase {
        private String name;
        private Double hours;
        private String color;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public Double getHours() { return hours; }
        public void setHours(Double hours) { this.hours = hours; }

        public String getColor() { return color; }
        public void setColor(String color) { this.color = color; }
    }
}