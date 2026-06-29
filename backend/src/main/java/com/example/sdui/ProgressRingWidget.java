package com.example.sdui;

/**
 * 进度环组件 — 环形进度条展示完成度百分比。
 */
public class ProgressRingWidget extends Widget {

    private Double percentage;
    private String label;
    private String color;
    private String subText;

    public ProgressRingWidget() {
        this.type = "progress_ring";
    }

    public Double getPercentage() {
        return percentage;
    }

    public void setPercentage(Double percentage) {
        this.percentage = percentage;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getSubText() {
        return subText;
    }

    public void setSubText(String subText) {
        this.subText = subText;
    }
}