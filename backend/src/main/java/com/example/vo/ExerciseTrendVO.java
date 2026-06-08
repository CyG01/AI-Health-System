package com.example.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class ExerciseTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> xAxis;

    private List<BigDecimal> completeRate;

    public List<String> getXAxis() {
        return xAxis;
    }

    public void setXAxis(List<String> xAxis) {
        this.xAxis = xAxis;
    }

    public List<BigDecimal> getCompleteRate() {
        return completeRate;
    }

    public void setCompleteRate(List<BigDecimal> completeRate) {
        this.completeRate = completeRate;
    }
}
