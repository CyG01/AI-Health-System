package com.example.service;

import com.example.dto.OnboardingRequest;
import com.example.entity.UserProfile;

public interface OnboardingService {

    /**
     * 提交新手引导问卷
     */
    UserProfile submitOnboarding(Long userId, OnboardingRequest request);

    /**
     * 获取用户画像
     */
    UserProfile getProfile(Long userId);

    /**
     * 检查是否已完成新手引导
     */
    boolean isOnboardingCompleted(Long userId);

    /**
     * 生成用户自然语言画像摘要（用于注入AI上下文）
     */
    String buildProfileSummary(Long userId);

    /**
     * 新用户7天激活策略：根据注册天数推送对应内容
     */
    String getActivationMessage(Long userId);

    /**
     * 每日递增注册天数
     */
    void incrementRegistrationDay(Long userId);
}