package com.example.service;

import com.example.vo.DashboardMonthVO;
import com.example.vo.DashboardTodayVO;
import com.example.vo.DashboardWeekVO;

public interface DashboardService {

    DashboardTodayVO getTodayOverview(Long userId);

    DashboardWeekVO getWeekOverview(Long userId);

    DashboardMonthVO getMonthOverview(Long userId);
}