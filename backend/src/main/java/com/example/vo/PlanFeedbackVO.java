package com.example.vo;

import java.io.Serializable;
import java.time.LocalDateTime;

public class PlanFeedbackVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long planId;
    private Long userId;
    private String feedbackType;
    private String content;
    private Integer satisfactionScore;
    private String adjustmentSuggestion;
    private Integer isAdjusted;
    private Long newPlanId;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getFeedbackType() { return feedbackType; }
    public void setFeedbackType(String feedbackType) { this.feedbackType = feedbackType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getSatisfactionScore() { return satisfactionScore; }
    public void setSatisfactionScore(Integer satisfactionScore) { this.satisfactionScore = satisfactionScore; }

    public String getAdjustmentSuggestion() { return adjustmentSuggestion; }
    public void setAdjustmentSuggestion(String adjustmentSuggestion) { this.adjustmentSuggestion = adjustmentSuggestion; }

    public Integer getIsAdjusted() { return isAdjusted; }
    public void setIsAdjusted(Integer isAdjusted) { this.isAdjusted = isAdjusted; }

    public Long getNewPlanId() { return newPlanId; }
    public void setNewPlanId(Long newPlanId) { this.newPlanId = newPlanId; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}