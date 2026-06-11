package com.example.sdui;

/**
 * 文本块组件 — 纯文本内容展示，作为降级兜底组件。
 */
public class TextBlockWidget extends Widget {

    private String content;
    private String textSize;
    private Boolean bold;

    public TextBlockWidget() {
        this.type = "text_block";
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTextSize() {
        return textSize;
    }

    public void setTextSize(String textSize) {
        this.textSize = textSize;
    }

    public Boolean getBold() {
        return bold;
    }

    public void setBold(Boolean bold) {
        this.bold = bold;
    }
}