package com.example.service;

import java.util.Map;

public interface RecommendationService {

    /**
     * 获取个性化推荐内容（运动+饮食+健康建议）
     */
    Map<String, Object> getRecommendations(Long userId);
}