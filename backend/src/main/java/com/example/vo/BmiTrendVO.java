package com.example.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public class BmiTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> xAxis;

    private List<BigDecimal> yAxis;

    public List<String> getXAxis() {
        return xAxis;
    }

    public void setXAxis(List<String> xAxis) {
        this.xAxis = xAxis;
    }

    public List<BigDecimal> getYAxis() {
        return yAxis;
    }

    public void setYAxis(List<BigDecimal> yAxis) {
        this.yAxis = yAxis;
    }
}
