package com.example.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class CheckinTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> xAxis;

    private List<BigDecimal> completeRate;

    private List<Integer> totalDays;

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

    public List<Integer> getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(List<Integer> totalDays) {
        this.totalDays = totalDays;
    }
}
