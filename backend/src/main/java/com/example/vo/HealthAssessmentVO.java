package com.example.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class HealthAssessmentVO extends HealthRecordVO {

    private String bmiLevel;

    private List<String> risks;

    public String getBmiLevel() {
        return bmiLevel;
    }

    public void setBmiLevel(String bmiLevel) {
        this.bmiLevel = bmiLevel;
    }

    public List<String> getRisks() {
        return risks;
    }

    public void setRisks(List<String> risks) {
        this.risks = risks;
    }
}
