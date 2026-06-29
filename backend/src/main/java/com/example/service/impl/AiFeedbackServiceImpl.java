package com.example.service.impl;

import com.example.dto.AiFeedbackDTO;
import com.example.entity.AiFeedback;
import com.example.mapper.AiFeedbackMapper;
import com.example.service.AiFeedbackService;
import com.example.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI反馈服务实现。
 * 负责处理用户对AI建议的评价，并将负面反馈存入用户记忆以优化后续AI建议。
 */
@Slf4j
@Service
public class AiFeedbackServiceImpl implements AiFeedbackService {

    private final AiFeedbackMapper feedbackMapper;
    private final MemoryService memoryService;

    public AiFeedbackServiceImpl(AiFeedbackMapper feedbackMapper,
                                  MemoryService memoryService) {
        this.feedbackMapper = feedbackMapper;
        this.memoryService = memoryService;
    }

    @Override
    public void submitFeedback(Long userId, AiFeedbackDTO dto) {
        AiFeedback feedback = new AiFeedback();
        feedback.setUserId(userId);
        feedback.setAiResponseId(dto.getAiResponseId());
        feedback.setRating(dto.getRating());
        feedback.setComment(dto.getComment());
        feedback.setManualReviewed(0);
        feedback.setCreatedAt(LocalDateTime.now());
        feedback.setUpdatedAt(LocalDateTime.now());
        feedbackMapper.insert(feedback);

        log.info("用户提交AI反馈 userId={} responseId={} rating={}",
                userId, dto.getAiResponseId(), dto.getRating());

        // 自动记录负面反馈到用户记忆，下次生成建议时自动规避
        if ("incorrect".equals(dto.getRating()) || "useless".equals(dto.getRating())) {
            String memoryContent = "用户反馈AI建议存在问题：" + (dto.getComment() != null ? dto.getComment() : "未提供详细说明");
            memoryService.store(userId, memoryContent, "FEEDBACK", 9, "USER_INPUT");
            log.info("负面反馈已存入用户记忆 userId={} rating={}", userId, dto.getRating());
        }
    }

    @Override
    public List<AiFeedback> getPendingReviewList() {
        return feedbackMapper.selectPendingReview();
    }

    @Override
    public void reviewFeedback(Long id, String reviewResult, Long reviewerId) {
        AiFeedback feedback = new AiFeedback();
        feedback.setId(id);
        feedback.setManualReviewed(1);
        feedback.setReviewerId(reviewerId);
        feedback.setReviewResult(reviewResult);
        feedback.setResolvedAt(LocalDateTime.now());
        feedbackMapper.updateById(feedback);

        log.info("管理员审核反馈 feedbackId={} result={} reviewerId={}",
                id, reviewResult, reviewerId);

        // 如果是有效错误反馈，记录日志（后续可扩展自动触发知识库优化）
        if ("valid".equals(reviewResult)) {
            AiFeedback original = feedbackMapper.selectById(id);
            log.warn("确认有效错误反馈，应考虑更新知识库 feedbackId={} comment={}",
                    id, original != null ? original.getComment() : "N/A");
        }
    }
}