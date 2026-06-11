package com.example.service;

import com.example.sdui.AiAgentResponse;

public interface PlanAdjustService {

    /**
     * 基于用户打卡数据和反馈，AI动态调整计划并生成新计划。
     * 返回 SDUI 协议的 AiAgentResponse，包含调整后的计划内容及渲染组件。
     */
    AiAgentResponse adjustPlan(Long originalPlanId, Long userId, String feedback);
}