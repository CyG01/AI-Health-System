package com.example.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.io.Serializable;

public class PlanGenerateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotBlank(message = "计划类型不能为空")
    @Pattern(regexp = "^(sport|diet|comprehensive|rehabilitation|meditation)$", message = "计划类型必须为sport/diet/comprehensive/rehabilitation/meditation")
    private String planType;

    @NotNull(message = "计划天数不能为空")
    @Min(value = 7, message = "计划天数最小为7天")
    @Max(value = 30, message = "计划天数最大为30天")
    private Integer durationDays;

    @Size(max = 50, message = "运动强度不能超过50个字符")
    private String intensity;

    @Size(max = 50, message = "口味偏好不能超过50个字符")
    private String tastePreference;

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public Integer getDurationDays() {
        return durationDays;
    }

    public void setDurationDays(Integer durationDays) {
        this.durationDays = durationDays;
    }

    public String getIntensity() {
        return intensity;
    }

    public void setIntensity(String intensity) {
        this.intensity = intensity;
    }

    public String getTastePreference() {
        return tastePreference;
    }

    public void setTastePreference(String tastePreference) {
        this.tastePreference = tastePreference;
    }
}
