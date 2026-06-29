package com.example.vo;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class AiPlanDetailVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String planType;
    private String planName;
    private Integer durationDays;
    private LocalDate startDate;
    private Integer status;
    private LocalDateTime createTime;
    private String aiContent;

    private List<DetailItem> details;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }

    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public String getAiContent() { return aiContent; }
    public void setAiContent(String aiContent) { this.aiContent = aiContent; }

    public List<DetailItem> getDetails() { return details; }
    public void setDetails(List<DetailItem> details) { this.details = details; }

    public static class DetailItem implements Serializable {
        private static final long serialVersionUID = 1L;

        private Long id;
        private Long planId;
        private Integer daySequence;
        private String itemType;
        private Long itemId;
        private String itemName;
        private String targetAmount;
        private Integer status;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }

        public Long getPlanId() { return planId; }
        public void setPlanId(Long planId) { this.planId = planId; }

        public Integer getDaySequence() { return daySequence; }
        public void setDaySequence(Integer daySequence) { this.daySequence = daySequence; }

        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }

        public Long getItemId() { return itemId; }
        public void setItemId(Long itemId) { this.itemId = itemId; }

        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }

        public String getTargetAmount() { return targetAmount; }
        public void setTargetAmount(String targetAmount) { this.targetAmount = targetAmount; }

        public Integer getStatus() { return status; }
        public void setStatus(Integer status) { this.status = status; }
    }
}