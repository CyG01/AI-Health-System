package com.example.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 热量缺口分析（摄入 vs 消耗）
 */
public class CalorieDeficitVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> xAxis;
    private List<Integer> consumed;
    private List<Integer> burned;
    private List<Integer> net;

    public List<String> getXAxis() { return xAxis; }
    public void setXAxis(List<String> xAxis) { this.xAxis = xAxis; }

    public List<Integer> getConsumed() { return consumed; }
    public void setConsumed(List<Integer> consumed) { this.consumed = consumed; }

    public List<Integer> getBurned() { return burned; }
    public void setBurned(List<Integer> burned) { this.burned = burned; }

    public List<Integer> getNet() { return net; }
    public void setNet(List<Integer> net) { this.net = net; }
}