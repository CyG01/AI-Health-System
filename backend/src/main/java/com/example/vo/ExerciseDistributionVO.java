package com.example.vo;

import java.io.Serializable;
import java.util.List;

/**
 * 运动类型分布
 */
public class ExerciseDistributionVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private List<String> names;
    private List<Long> values;

    public List<String> getNames() { return names; }
    public void setNames(List<String> names) { this.names = names; }

    public List<Long> getValues() { return values; }
    public void setValues(List<Long> values) { this.values = values; }
}