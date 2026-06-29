package com.example.service;

import java.util.Map;

/**
 * 用户隐私授权服务接口。
 */
public interface PrivacyService {

    /**
     * 查询用户数据授权状态。
     *
     * @param userId 用户ID
     * @return 包含 userId、dataConsentForModel、dataConsentForRecommend 的映射
     */
    Map<String, Object> getConsent(Long userId);

    /**
     * 更新用户数据授权状态。
     *
     * @param userId                  用户ID
     * @param dataConsentForModel     数据用于模型训练授权 0=未授权 1=已授权，null 表示不更新
     * @param dataConsentForRecommend 数据用于个性化推荐授权 0=未授权 1=已授权，null 表示不更新
     * @return 更新后的授权状态映射
     */
    Map<String, Object> updateConsent(Long userId, Integer dataConsentForModel, Integer dataConsentForRecommend);
}
