package com.example.vo;

import java.io.Serializable;
import java.util.List;

public class CalorieTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> xAxis;
    private List<Integer> dailyCalories;

    public List<String> getXAxis() { return xAxis; }
    public void setXAxis(List<String> xAxis) { this.xAxis = xAxis; }

    public List<Integer> getDailyCalories() { return dailyCalories; }
    public void setDailyCalories(List<Integer> dailyCalories) { this.dailyCalories = dailyCalories; }
}