package com.example.service;

import com.example.dto.PlanGenerateDTO;
import com.example.vo.AiPlanDetailVO;
import com.example.vo.AiPlanVO;

import java.util.List;

public interface AiPlanService {

    AiPlanDetailVO generatePlan(PlanGenerateDTO dto, Long userId);

    List<AiPlanVO> getPlanList(Long userId);

    AiPlanDetailVO getPlanDetail(Long planId, Long userId);

    void activePlan(Long planId, Long userId);

    void deletePlan(Long planId, Long userId);
}
