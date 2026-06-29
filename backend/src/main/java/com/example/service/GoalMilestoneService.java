package com.example.service;

import com.example.dto.GoalMilestoneDTO;
import com.example.vo.GoalMilestoneVO;

import java.util.List;

public interface GoalMilestoneService {

    /**
     * 创建目标
     */
    GoalMilestoneVO create(Long userId, GoalMilestoneDTO dto);

    /**
     * 更新目标
     */
    GoalMilestoneVO update(Long userId, GoalMilestoneDTO dto);

    /**
     * 删除目标
     */
    void delete(Long userId, Long goalId);

    /**
     * 获取用户所有目标
     */
    List<GoalMilestoneVO> list(Long userId);

    /**
     * 获取单个目标
     */
    GoalMilestoneVO getById(Long userId, Long goalId);

    /**
     * 标记目标完成/放弃
     */
    GoalMilestoneVO updateStatus(Long userId, Long goalId, Integer status);

    /**
     * 更新目标进度 (由其他模块回调更新)
     */
    void updateProgress(Long userId, String goalType, java.math.BigDecimal newValue);
}