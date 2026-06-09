package com.example.service;

import com.example.vo.HealthReportVO;

import java.util.List;

public interface HealthReportService {

    /**
     * 手动生成指定类型的健康报告
     */
    HealthReportVO generateReport(Long userId, String reportType);

    /**
     * 获取用户报告列表
     */
    List<HealthReportVO> getReportList(Long userId, int page, int size);

    /**
     * 获取单个报告详情
     */
    HealthReportVO getReportDetail(Long reportId, Long userId);

    /**
     * 标记报告已读
     */
    void markAsRead(Long reportId, Long userId);

    /**
     * 定时任务：每周一自动生成周报
     */
    void autoGenerateWeeklyReports();

    /**
     * 定时任务：每月1号自动生成月报
     */
    void autoGenerateMonthlyReports();
}