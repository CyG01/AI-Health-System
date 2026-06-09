package com.example.service;

import com.example.vo.ExerciseGuidanceVO;

public interface ExerciseGuidanceService {

    /**
     * 获取运动项目的AI动作指导（缓存优先）
     */
    ExerciseGuidanceVO getGuidance(Long exerciseId);
}