package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.convert.DailyCheckinConvert;
import com.example.dto.CheckinSubmitDTO;
import com.example.dto.CheckinSupplementDTO;
import com.example.entity.DailyCheckin;
import com.example.mapper.DailyCheckinMapper;
import com.example.service.CheckinService;
import com.example.vo.CheckinStatsVO;
import com.example.vo.CheckinVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CheckinServiceImpl implements CheckinService {

    private static final Logger log = LoggerFactory.getLogger(CheckinServiceImpl.class);

    private static final int SUPPLEMENT_MAX_DAYS = 7;

    @Autowired
    private DailyCheckinMapper dailyCheckinMapper;

    @Autowired
    private DailyCheckinConvert dailyCheckinConvert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckinVO submitCheckin(Long userId, CheckinSubmitDTO dto) {
        LocalDate today = LocalDate.now();

        LambdaQueryWrapper<DailyCheckin> existWrapper = new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .eq(DailyCheckin::getCheckDate, today);
        if (dailyCheckinMapper.selectCount(existWrapper) > 0) {
            throw new BusinessException("今日已打卡，请勿重复提交");
        }

        DailyCheckin checkin = dailyCheckinConvert.toEntity(dto);
        checkin.setUserId(userId);
        checkin.setCheckDate(today);

        dailyCheckinMapper.insert(checkin);
        log.info("提交打卡 userId={} checkDate={} checkinId={}", userId, today, checkin.getId());
        return dailyCheckinConvert.toCheckinVO(checkin);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CheckinVO supplementCheckin(Long userId, CheckinSupplementDTO dto) {
        LocalDate checkDate = dto.getCheckDate();
        LocalDate today = LocalDate.now();

        if (checkDate.isAfter(today)) {
            throw new BusinessException("不能补打未来日期的卡");
        }

        long daysBetween = ChronoUnit.DAYS.between(checkDate, today);
        if (daysBetween > SUPPLEMENT_MAX_DAYS) {
            throw new BusinessException("补卡日期不得超过过去" + SUPPLEMENT_MAX_DAYS + "天");
        }

        LambdaQueryWrapper<DailyCheckin> existWrapper = new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .eq(DailyCheckin::getCheckDate, checkDate);
        if (dailyCheckinMapper.selectCount(existWrapper) > 0) {
            throw new BusinessException("该日期已有打卡记录");
        }

        DailyCheckin checkin = dailyCheckinConvert.toEntity(dto);
        checkin.setUserId(userId);

        dailyCheckinMapper.insert(checkin);
        log.info("补卡提交 userId={} checkDate={} checkinId={}", userId, checkDate, checkin.getId());
        return dailyCheckinConvert.toCheckinVO(checkin);
    }

    @Override
    public List<CheckinVO> getCheckinList(Long userId) {
        LambdaQueryWrapper<DailyCheckin> wrapper = new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .orderByDesc(DailyCheckin::getCheckDate);
        return dailyCheckinMapper.selectList(wrapper).stream()
                .map(dailyCheckinConvert::toCheckinVO)
                .collect(Collectors.toList());
    }

    @Override
    public Page<CheckinVO> getCheckinPage(Long userId, int page, int size) {
        Page<DailyCheckin> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DailyCheckin> wrapper = new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .orderByDesc(DailyCheckin::getCheckDate);
        Page<DailyCheckin> result = dailyCheckinMapper.selectPage(pageParam, wrapper);
        Page<CheckinVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream()
                .map(dailyCheckinConvert::toCheckinVO)
                .toList());
        return voPage;
    }

    @Override
    public CheckinStatsVO getStats(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate monthStart = today.withDayOfMonth(1);

        LambdaQueryWrapper<DailyCheckin> allWrapper = new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .orderByDesc(DailyCheckin::getCheckDate);
        List<DailyCheckin> allRecords = dailyCheckinMapper.selectList(allWrapper);

        int totalDays = allRecords.size();
        int consecutiveDays = calculateConsecutiveDays(allRecords, today);

        int currentWeekDays = (int) allRecords.stream()
                .filter(r -> !r.getCheckDate().isBefore(weekStart) && !r.getCheckDate().isAfter(today))
                .count();

        int currentMonthDays = (int) allRecords.stream()
                .filter(r -> !r.getCheckDate().isBefore(monthStart) && !r.getCheckDate().isAfter(today))
                .count();

        long exerciseCompleteCount = allRecords.stream()
                .filter(r -> r.getExerciseStatus() != null && r.getExerciseStatus() == 2)
                .count();
        long dietCompleteCount = allRecords.stream()
                .filter(r -> r.getDietStatus() != null && r.getDietStatus() == 2)
                .count();

        BigDecimal exerciseCompleteRate = BigDecimal.ZERO;
        BigDecimal dietCompleteRate = BigDecimal.ZERO;
        if (totalDays > 0) {
            exerciseCompleteRate = BigDecimal.valueOf(exerciseCompleteCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalDays), 1, RoundingMode.HALF_UP);
            dietCompleteRate = BigDecimal.valueOf(dietCompleteCount)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalDays), 1, RoundingMode.HALF_UP);
        }

        CheckinStatsVO vo = new CheckinStatsVO();
        vo.setConsecutiveDays(consecutiveDays);
        vo.setTotalDays(totalDays);
        vo.setCurrentWeekDays(currentWeekDays);
        vo.setCurrentMonthDays(currentMonthDays);
        vo.setExerciseCompleteRate(exerciseCompleteRate);
        vo.setDietCompleteRate(dietCompleteRate);
        return vo;
    }

    private int calculateConsecutiveDays(List<DailyCheckin> records, LocalDate today) {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        List<LocalDate> dates = records.stream()
                .map(DailyCheckin::getCheckDate)
                .distinct()
                .sorted(LocalDate::compareTo)
                .toList();

        LocalDate latestDate = dates.get(dates.size() - 1);
        if (latestDate.isBefore(today.minusDays(1))) {
            return 0;
        }

        int consecutive = 0;
        LocalDate pointer = latestDate;
        for (int i = dates.size() - 1; i >= 0; i--) {
            if (dates.get(i).equals(pointer)) {
                consecutive++;
                pointer = pointer.minusDays(1);
            } else if (dates.get(i).isBefore(pointer)) {
                break;
            }
        }
        return consecutive;
    }
}
