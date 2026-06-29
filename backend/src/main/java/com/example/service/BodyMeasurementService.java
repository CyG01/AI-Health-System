package com.example.service;

import com.example.dto.BodyMeasurementSubmitDTO;
import com.example.vo.BodyMeasurementVO;

import java.util.List;

public interface BodyMeasurementService {

    /**
     * 提交围度记录
     */
    BodyMeasurementVO submit(Long userId, BodyMeasurementSubmitDTO dto);

    /**
     * 获取最新围度记录
     */
    BodyMeasurementVO getLatest(Long userId);

    /**
     * 获取围度历史列表
     */
    List<BodyMeasurementVO> getHistory(Long userId, int limit);

    /**
     * 获取围度趋势数据
     */
    List<BodyMeasurementVO> getTrend(Long userId, int months);

    /**
     * 删除围度记录
     */
    void delete(Long userId, Long id);
}