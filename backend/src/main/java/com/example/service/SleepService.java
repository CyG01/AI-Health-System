package com.example.service;

import com.example.dto.SleepRecordSubmitDTO;
import com.example.vo.SleepRecordVO;

import java.util.List;

public interface SleepService {

    /**
     * 提交睡眠记录
     */
    SleepRecordVO submit(Long userId, SleepRecordSubmitDTO dto);

    /**
     * 获取指定日期的睡眠记录
     */
    SleepRecordVO getByDate(Long userId, java.time.LocalDate date);

    /**
     * 获取近N天的睡眠记录列表
     */
    List<SleepRecordVO> getList(Long userId, int days);

    /**
     * AI睡眠分析
     */
    String analyzeSleep(Long userId);

    /**
     * 删除睡眠记录
     */
    void delete(Long userId, Long id);
}