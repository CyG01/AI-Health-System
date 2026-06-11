package com.example.service;

import com.example.dto.AiFeedbackDTO;
import com.example.entity.AiFeedback;

import java.util.List;

/**
 * AI反馈服务接口。
 */
public interface AiFeedbackService {

    /**
     * 用户提交反馈。
     */
    void submitFeedback(Long userId, AiFeedbackDTO dto);

    /**
     * 获取待审核反馈列表。
     */
    List<AiFeedback> getPendingReviewList();

    /**
     * 人工审核反馈。
     */
    void reviewFeedback(Long id, String reviewResult, Long reviewerId);
}