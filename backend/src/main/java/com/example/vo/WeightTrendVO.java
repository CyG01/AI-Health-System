package com.example.vo;

import java.io.Serializable;
import java.util.List;

public class WeightTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> xAxis;
    private List<Integer> yAxis;

    public List<String> getXAxis() { return xAxis; }
    public void setXAxis(List<String> xAxis) { this.xAxis = xAxis; }

    public List<Integer> getYAxis() { return yAxis; }
    public void setYAxis(List<Integer> yAxis) { this.yAxis = yAxis; }
}