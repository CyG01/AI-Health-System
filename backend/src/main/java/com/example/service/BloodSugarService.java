package com.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dto.BloodSugarSubmitDTO;
import com.example.vo.BloodSugarVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface BloodSugarService {

    /** 提交血糖记录 */
    BloodSugarVO submitRecord(Long userId, BloodSugarSubmitDTO dto);

    /** 分页查询血糖记录 */
    Page<BloodSugarVO> getRecordsPage(Long userId, int page, int size);

    /** 按日期查询 */
    List<BloodSugarVO> getRecordsByDate(Long userId, LocalDate date);

    /** 查询最近N天趋势（优先走 TDengine） */
    List<BloodSugarVO> getTrend(Long userId, int days);

    /** 删除记录 */
    void deleteRecord(Long userId, Long recordId);

    /** 获取日均血糖（优先走 TDengine，降级走 MySQL） */
    BigDecimal getDailyAvg(Long userId, LocalDate date);

    /** 获取年度血糖趋势数据（优先走 TDengine） */
    List<BloodSugarVO> getYearTrend(Long userId);
}