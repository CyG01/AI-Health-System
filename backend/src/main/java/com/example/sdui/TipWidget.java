package com.example.sdui;

/**
 * 提示条组件 — 展示简短的健康提示或建议标签。
 */
public class TipWidget extends Widget {

    private String content;
    private String icon;
    private String category;

    public TipWidget() {
        this.type = "tip";
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}