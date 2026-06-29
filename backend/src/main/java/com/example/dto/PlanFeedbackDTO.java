package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.io.Serializable;

public class PlanFeedbackDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull(message = "计划ID不能为空")
    private Long planId;

    @NotBlank(message = "反馈类型不能为空")
    @Pattern(regexp = "^(difficulty|satisfaction|content|general)$",
             message = "反馈类型仅支持 difficulty/satisfaction/content/general")
    private String feedbackType;

    @NotBlank(message = "反馈内容不能为空")
    private String content;

    private Integer satisfactionScore;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getSatisfactionScore() { return satisfactionScore; }
    public void setSatisfactionScore(Integer satisfactionScore) { this.satisfactionScore = satisfactionScore; }
}