package com.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dto.PlanGenerateDTO;
import com.example.dto.PlanSolidifyDTO;
import com.example.vo.AiPlanDetailVO;
import com.example.vo.AiPlanVO;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

public interface AiPlanService {

    AiPlanDetailVO generatePlan(PlanGenerateDTO dto, Long userId);

    void generatePlanStream(PlanGenerateDTO dto, Long userId, SseEmitter emitter);

    Page<AiPlanVO> getPlanList(Long userId, int page, int size, String keyword);

    AiPlanDetailVO getPlanDetail(Long planId, Long userId);

    void activePlan(Long planId, Long userId);

    void deletePlan(Long planId, Long userId);

    void completeTask(Long detailId, Long userId);

    /**
     * 将聊天生成的临时计划固化为正式计划。
     * 状态从 DRAFT → ACTIVE，自动填充日历。
     *
     * @param userId 用户ID
     * @param dto    固化请求（临时计划ID + 版本号）
     * @return 固化后的计划详情
     */
    AiPlanVO solidifyPlan(Long userId, PlanSolidifyDTO dto);

    /**
     * 更新计划中某天的单个任务项（Copilot replace_item 持久化）
     */
    void updateDayItem(Long planId, Long userId, int dayIndex, int itemIndex, Map<String, Object> newItem);

    /**
     * 替换计划中某天的全部任务项（Copilot replace_day_items 持久化）
     */
    void replaceDayItems(Long planId, Long userId, int dayIndex, List<Map<String, Object>> items);

    /**
     * 整体替换计划内容（Copilot set_plan 持久化）
     */
    void updatePlanContent(Long planId, Long userId, String planJson, List<Map<String, Object>> days);
}
