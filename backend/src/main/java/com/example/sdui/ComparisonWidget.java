package com.example.sdui;

/**
 * 对比组件 — 展示前后数据对比（如体重变化、饮食对比等）。
 */
public class ComparisonWidget extends Widget {

    private String beforeLabel;
    private String beforeValue;
    private String afterLabel;
    private String afterValue;
    private String changePercentage;
    private String changeDirection;

    public ComparisonWidget() {
        this.type = "comparison";
    }

    public String getBeforeLabel() {
        return beforeLabel;
    }

    public void setBeforeLabel(String beforeLabel) {
        this.beforeLabel = beforeLabel;
    }

    public String getBeforeValue() {
        return beforeValue;
    }

    public void setBeforeValue(String beforeValue) {
        this.beforeValue = beforeValue;
    }

    public String getAfterLabel() {
        return afterLabel;
    }

    public void setAfterLabel(String afterLabel) {
        this.afterLabel = afterLabel;
    }

    public String getAfterValue() {
        return afterValue;
    }

    public void setAfterValue(String afterValue) {
        this.afterValue = afterValue;
    }

    public String getChangePercentage() {
        return changePercentage;
    }

    public void setChangePercentage(String changePercentage) {
        this.changePercentage = changePercentage;
    }

    public String getChangeDirection() {
        return changeDirection;
    }

    public void setChangeDirection(String changeDirection) {
        this.changeDirection = changeDirection;
    }
}