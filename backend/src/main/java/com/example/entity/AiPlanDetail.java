package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;

@TableName("ai_plan_detail")
public class AiPlanDetail implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long planId;

    private Integer daySequence;

    private String itemType;

    private Long itemId;

    private String itemName;

    private String targetAmount;

    private Integer status;

    /** 子阶段名称（如 热身、核心训练、放松拉伸） */
    private String subPhase;

    /** 子阶段类型：warmup / core / cooldown */
    private String subPhaseType;

    /** 子阶段排序 */
    private Integer phaseOrder;

    /** 子阶段时长（分钟） */
    private Integer phaseDurationMinutes;

    /** 场景标签：workday / weekend / travel */
    private String scenarioTag;

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

    public String getSubPhase() { return subPhase; }
    public void setSubPhase(String subPhase) { this.subPhase = subPhase; }

    public String getSubPhaseType() { return subPhaseType; }
    public void setSubPhaseType(String subPhaseType) { this.subPhaseType = subPhaseType; }

    public Integer getPhaseOrder() { return phaseOrder; }
    public void setPhaseOrder(Integer phaseOrder) { this.phaseOrder = phaseOrder; }

    public Integer getPhaseDurationMinutes() { return phaseDurationMinutes; }
    public void setPhaseDurationMinutes(Integer phaseDurationMinutes) { this.phaseDurationMinutes = phaseDurationMinutes; }

    public String getScenarioTag() { return scenarioTag; }
    public void setScenarioTag(String scenarioTag) { this.scenarioTag = scenarioTag; }
}