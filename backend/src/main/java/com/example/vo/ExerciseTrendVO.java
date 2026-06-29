package com.example.vo;

import java.io.Serializable;
import java.util.List;

public class ExerciseTrendVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> xAxis;

    private List<Integer> minutesPerDay;

    public List<String> getXAxis() {
        return xAxis;
    }

    public void setXAxis(List<String> xAxis) {
        this.xAxis = xAxis;
    }

    public List<Integer> getMinutesPerDay() {
        return minutesPerDay;
    }

    public void setMinutesPerDay(List<Integer> minutesPerDay) {
        this.minutesPerDay = minutesPerDay;
    }
}
