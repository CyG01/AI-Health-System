package com.example.service;

import com.example.vo.DashboardGreetingVO;
import com.example.vo.DashboardMonthVO;
import com.example.vo.DashboardTodayVO;
import com.example.vo.DashboardWeekVO;

public interface DashboardService {

    DashboardTodayVO getTodayOverview(Long userId);

    DashboardWeekVO getWeekOverview(Long userId);

    DashboardMonthVO getMonthOverview(Long userId);

    /**
     * 生成 AI 预测性问候卡片（规则引擎，非大模型调用）。
     * 根据当前时间 + 打卡状态 + 计划状态匹配卡片模板。
     */
    DashboardGreetingVO generateGreeting(Long userId);
}