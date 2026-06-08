package com.example.vo;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AiPlanDetailVO extends AiPlanVO {

    private String aiContent;

    public String getAiContent() {
        return aiContent;
    }

    public void setAiContent(String aiContent) {
        this.aiContent = aiContent;
    }
}
