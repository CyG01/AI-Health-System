package com.example.service;

import com.example.dto.WaterRecordSubmitDTO;
import com.example.vo.WaterRecordVO;

import java.time.LocalDate;
import java.util.List;

public interface WaterService {

    /**
     * 提交饮水记录
     */
    WaterRecordVO submit(Long userId, WaterRecordSubmitDTO dto);

    /**
     * 获取今日饮水总量
     */
    WaterRecordVO getToday(Long userId);

    /**
     * 获取近N天饮水记录
     */
    List<WaterRecordVO> getList(Long userId, int days);

    /**
     * 获取指定日期的饮水汇总
     */
    int getDailyTotal(Long userId, LocalDate date);

    /**
     * 删除饮水记录
     */
    void delete(Long userId, Long id);
}