package com.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dto.PlanGenerateDTO;
import com.example.vo.AiPlanDetailVO;
import com.example.vo.AiPlanVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiPlanService {

    AiPlanDetailVO generatePlan(PlanGenerateDTO dto, Long userId);

    void generatePlanStream(PlanGenerateDTO dto, Long userId, SseEmitter emitter);

    Page<AiPlanVO> getPlanList(Long userId, int page, int size, String keyword);

    AiPlanDetailVO getPlanDetail(Long planId, Long userId);

    void activePlan(Long planId, Long userId);

    void deletePlan(Long planId, Long userId);
}
