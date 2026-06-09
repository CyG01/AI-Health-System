package com.example.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 营养素占比饼图
 */
public class NutrientRatioVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> names;
    private List<Double> values;

    public List<String> getNames() { return names; }
    public void setNames(List<String> names) { this.names = names; }

    public List<Double> getValues() { return values; }
    public void setValues(List<Double> values) { this.values = values; }
}