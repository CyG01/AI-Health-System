package com.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dto.PlanFeedbackDTO;
import com.example.vo.PlanFeedbackVO;

import java.util.List;

public interface PlanFeedbackService {

    PlanFeedbackVO submitFeedback(Long userId, PlanFeedbackDTO dto);

    List<PlanFeedbackVO> getFeedbacksByPlanId(Long planId);

    Page<PlanFeedbackVO> getFeedbacksByPlanIdPage(Long planId, int page, int size);

    List<PlanFeedbackVO> getFeedbacksByUserId(Long userId);

    Page<PlanFeedbackVO> getFeedbacksByUserIdPage(Long userId, int page, int size);

    // 管理员功能
    Page<PlanFeedbackVO> getAllFeedbacks(int page, int size);

    PlanFeedbackVO getFeedbackById(Long id);

    PlanFeedbackVO triggerAdjust(Long feedbackId);
}