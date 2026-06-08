package com.example.service;

import com.example.vo.BmiTrendVO;
import com.example.vo.CalorieTrendVO;
import com.example.vo.CheckinTrendVO;
import com.example.vo.ExerciseTrendVO;
import com.example.vo.ProgressVO;
import com.example.vo.WeightTrendVO;

public interface StatisticsService {

    WeightTrendVO getWeightTrend(Long userId, Integer days);

    BmiTrendVO getBmiTrend(Long userId, Integer days);

    CheckinTrendVO getCheckinTrend(Long userId, Integer days);

    ExerciseTrendVO getExerciseTrend(Long userId, Integer days);

    CalorieTrendVO getCalorieTrend(Long userId, Integer days);

    ProgressVO getProgress(Long userId);
}
