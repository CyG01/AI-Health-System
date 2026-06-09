package com.example.service;

import com.example.vo.AiPlanDetailVO;

public interface PlanAdjustService {

    /**
     * 基于用户打卡数据和反馈，AI动态调整计划并生成新计划
     */
    AiPlanDetailVO adjustPlan(Long originalPlanId, Long userId, String feedback);
}