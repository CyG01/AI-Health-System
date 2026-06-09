package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.DailyCheckin;
import com.example.entity.HealthRecord;
import com.example.mapper.DailyCheckinMapper;
import com.example.mapper.HealthRecordMapper;
import com.example.service.StatisticsService;
import com.example.vo.BmiTrendVO;
import com.example.vo.CalorieTrendVO;
import com.example.vo.CheckinTrendVO;
import com.example.vo.ExerciseTrendVO;
import com.example.vo.ProgressVO;
import com.example.vo.WeightTrendVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class StatisticsServiceImpl implements StatisticsService {

    private static final Logger log = LoggerFactory.getLogger(StatisticsServiceImpl.class);

    private static final String STATS_CACHE_PREFIX = "stats:";
    private static final long STATS_CACHE_TTL_HOURS = 1;

    @Autowired
    private HealthRecordMapper healthRecordMapper;

    @Autowired
    private DailyCheckinMapper dailyCheckinMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public WeightTrendVO getWeightTrend(Long userId, Integer days) {
        String cacheKey = STATS_CACHE_PREFIX + "weight:" + userId + ":" + days;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (WeightTrendVO) cached;
        }

        int range = resolveDays(days);
        LocalDate startDate = LocalDate.now().minusDays(range - 1);

        LambdaQueryWrapper<DailyCheckin> wrapper = new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .ge(DailyCheckin::getCheckDate, startDate)
                .isNotNull(DailyCheckin::getCurrentWeight)
                .orderByAsc(DailyCheckin::getCheckDate);
        List<DailyCheckin> records = dailyCheckinMapper.selectList(wrapper);

        Map<LocalDate, BigDecimal> weightMap = records.stream()
                .collect(Collectors.toMap(
                        DailyCheckin::getCheckDate,
                        DailyCheckin::getCurrentWeight,
                        (existing, replacement) -> replacement));

        List<String> xAxis = new ArrayList<>();
        List<BigDecimal> yAxis = new ArrayList<>();

        for (LocalDate d = startDate; !d.isAfter(LocalDate.now()); d = d.plusDays(1)) {
            xAxis.add(d.toString());
            yAxis.add(weightMap.getOrDefault(d, null));
        }

        WeightTrendVO vo = new WeightTrendVO();
        vo.setXAxis(xAxis);
        vo.setYAxis(yAxis);

        redisTemplate.opsForValue().set(cacheKey, vo, STATS_CACHE_TTL_HOURS, TimeUnit.HOURS);
        return vo;
    }

    @Override
    public BmiTrendVO getBmiTrend(Long userId, Integer days) {
        String cacheKey = STATS_CACHE_PREFIX + "bmi:" + userId + ":" + days;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (BmiTrendVO) cached;
        }

        int range = resolveDays(days);
        LocalDate startDate = LocalDate.now().minusDays(range - 1);

        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId)
                .ge(HealthRecord::getCreateTime, startDate.atStartOfDay())
                .orderByAsc(HealthRecord::getCreateTime);
        List<HealthRecord> records = healthRecordMapper.selectList(wrapper);

        List<String> xAxis = new ArrayList<>();
        List<BigDecimal> yAxis = new ArrayList<>();

        for (HealthRecord record : records) {
            xAxis.add(record.getCreateTime().toLocalDate().toString());
            yAxis.add(record.getBmi());
        }

        BmiTrendVO vo = new BmiTrendVO();
        vo.setXAxis(xAxis);
        vo.setYAxis(yAxis);

        redisTemplate.opsForValue().set(cacheKey, vo, STATS_CACHE_TTL_HOURS, TimeUnit.HOURS);
        return vo;
    }

    @Override
    public CheckinTrendVO getCheckinTrend(Long userId, Integer days) {
        String cacheKey = STATS_CACHE_PREFIX + "checkin:" + userId + ":" + days;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (CheckinTrendVO) cached;
        }

        int range = resolveDays(days);
        LocalDate startDate = LocalDate.now().minusDays(range - 1);

        LambdaQueryWrapper<DailyCheckin> wrapper = new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .ge(DailyCheckin::getCheckDate, startDate)
                .orderByAsc(DailyCheckin::getCheckDate);
        List<DailyCheckin> records = dailyCheckinMapper.selectList(wrapper);

        Map<LocalDate, List<DailyCheckin>> grouped = records.stream()
                .collect(Collectors.groupingBy(DailyCheckin::getCheckDate));

        List<String> xAxis = new ArrayList<>();
        List<BigDecimal> completeRate = new ArrayList<>();
        List<Integer> totalDays = new ArrayList<>();

        for (LocalDate d = startDate; !d.isAfter(LocalDate.now()); d = d.plusDays(1)) {
            xAxis.add(d.toString());
            List<DailyCheckin> dayRecords = grouped.getOrDefault(d, List.of());
            totalDays.add(dayRecords.size());

            if (dayRecords.isEmpty()) {
                completeRate.add(BigDecimal.ZERO);
            } else {
                long fullCount = dayRecords.stream()
                        .filter(r -> r.getExerciseStatus() != null && r.getExerciseStatus() == 2
                                && r.getDietStatus() != null && r.getDietStatus() == 2)
                        .count();
                BigDecimal rate = BigDecimal.valueOf(fullCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(dayRecords.size()), 1, RoundingMode.HALF_UP);
                completeRate.add(rate);
            }
        }

        CheckinTrendVO vo = new CheckinTrendVO();
        vo.setXAxis(xAxis);
        vo.setCompleteRate(completeRate);
        vo.setTotalDays(totalDays);

        redisTemplate.opsForValue().set(cacheKey, vo, STATS_CACHE_TTL_HOURS, TimeUnit.HOURS);
        return vo;
    }

    @Override
    public ExerciseTrendVO getExerciseTrend(Long userId, Integer days) {
        String cacheKey = STATS_CACHE_PREFIX + "exercise:" + userId + ":" + days;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (ExerciseTrendVO) cached;
        }

        int range = resolveDays(days);
        LocalDate startDate = LocalDate.now().minusDays(range - 1);

        LambdaQueryWrapper<DailyCheckin> wrapper = new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .ge(DailyCheckin::getCheckDate, startDate)
                .orderByAsc(DailyCheckin::getCheckDate);
        List<DailyCheckin> records = dailyCheckinMapper.selectList(wrapper);

        Map<LocalDate, List<DailyCheckin>> grouped = records.stream()
                .collect(Collectors.groupingBy(DailyCheckin::getCheckDate));

        List<String> xAxis = new ArrayList<>();
        List<BigDecimal> completeRate = new ArrayList<>();

        for (LocalDate d = startDate; !d.isAfter(LocalDate.now()); d = d.plusDays(1)) {
            xAxis.add(d.toString());
            List<DailyCheckin> dayRecords = grouped.getOrDefault(d, List.of());
            if (dayRecords.isEmpty()) {
                completeRate.add(BigDecimal.ZERO);
            } else {
                long completeCount = dayRecords.stream()
                        .filter(r -> r.getExerciseStatus() != null && r.getExerciseStatus() == 2)
                        .count();
                BigDecimal rate = BigDecimal.valueOf(completeCount)
                        .multiply(BigDecimal.valueOf(100))
                        .divide(BigDecimal.valueOf(dayRecords.size()), 1, RoundingMode.HALF_UP);
                completeRate.add(rate);
            }
        }

        ExerciseTrendVO vo = new ExerciseTrendVO();
        vo.setXAxis(xAxis);
        vo.setCompleteRate(completeRate);

        redisTemplate.opsForValue().set(cacheKey, vo, STATS_CACHE_TTL_HOURS, TimeUnit.HOURS);
        return vo;
    }

    @Override
    public CalorieTrendVO getCalorieTrend(Long userId, Integer days) {
        String cacheKey = STATS_CACHE_PREFIX + "calorie:" + userId + ":" + days;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return (CalorieTrendVO) cached;
        }

        int range = resolveDays(days);
        LocalDate startDate = LocalDate.now().minusDays(range - 1);

        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId)
                .ge(HealthRecord::getCreateTime, startDate.atStartOfDay())
                .orderByAsc(HealthRecord::getCreateTime);
        List<HealthRecord> records = healthRecordMapper.selectList(wrapper);

        List<String> xAxis = new ArrayList<>();
        List<BigDecimal> yAxis = new ArrayList<>();

        for (HealthRecord record : records) {
            xAxis.add(record.getCreateTime().toLocalDate().toString());
            yAxis.add(record.getDailyCalorie());
        }

        CalorieTrendVO vo = new CalorieTrendVO();
        vo.setXAxis(xAxis);
        vo.setYAxis(yAxis);

        redisTemplate.opsForValue().set(cacheKey, vo, STATS_CACHE_TTL_HOURS, TimeUnit.HOURS);
        return vo;
    }

    @Override
    public ProgressVO getProgress(Long userId) {
        BigDecimal totalCheckinRate = BigDecimal.ZERO;
        BigDecimal exerciseCompleteRate = BigDecimal.ZERO;
        BigDecimal dietCompleteRate = BigDecimal.ZERO;
        BigDecimal weightChange = BigDecimal.ZERO;
        BigDecimal targetProgressPercent = BigDecimal.ZERO;
        String goal = null;

        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);

        LambdaQueryWrapper<DailyCheckin> checkinWrapper = new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .ge(DailyCheckin::getCheckDate, thirtyDaysAgo);
        List<DailyCheckin> checkinRecords = dailyCheckinMapper.selectList(checkinWrapper);

        int totalCheckins = checkinRecords.size();
        if (totalCheckins > 0) {
            long exerciseComplete = checkinRecords.stream()
                    .filter(r -> r.getExerciseStatus() != null && r.getExerciseStatus() == 2)
                    .count();
            long dietComplete = checkinRecords.stream()
                    .filter(r -> r.getDietStatus() != null && r.getDietStatus() == 2)
                    .count();
            long fullComplete = checkinRecords.stream()
                    .filter(r -> r.getExerciseStatus() != null && r.getExerciseStatus() == 2
                            && r.getDietStatus() != null && r.getDietStatus() == 2)
                    .count();

            exerciseCompleteRate = BigDecimal.valueOf(exerciseComplete)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalCheckins), 1, RoundingMode.HALF_UP);
            dietCompleteRate = BigDecimal.valueOf(dietComplete)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalCheckins), 1, RoundingMode.HALF_UP);
            totalCheckinRate = BigDecimal.valueOf(fullComplete)
                    .multiply(BigDecimal.valueOf(100))
                    .divide(BigDecimal.valueOf(totalCheckins), 1, RoundingMode.HALF_UP);
        }

        LambdaQueryWrapper<HealthRecord> healthWrapper = new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId)
                .ge(HealthRecord::getCreateTime, thirtyDaysAgo.atStartOfDay())
                .orderByAsc(HealthRecord::getCreateTime);
        List<HealthRecord> healthRecords = healthRecordMapper.selectList(healthWrapper);

        if (!healthRecords.isEmpty()) {
            HealthRecord latest = healthRecords.get(healthRecords.size() - 1);
            goal = latest.getGoal();

            List<DailyCheckin> weightRecords = checkinRecords.stream()
                    .filter(r -> r.getCurrentWeight() != null)
                    .toList();
            if (!weightRecords.isEmpty()) {
                BigDecimal startWeight = weightRecords.get(0).getCurrentWeight();
                BigDecimal latestWeight = weightRecords.get(weightRecords.size() - 1).getCurrentWeight();
                weightChange = latestWeight.subtract(startWeight).setScale(1, RoundingMode.HALF_UP);
            }

            if (latest.getWeight() != null && latest.getGoal() != null
                    && latest.getGoal().contains("减重") && latest.getBmi() != null) {
                BigDecimal targetBmi = new BigDecimal("24.0");
                BigDecimal currentBmi = latest.getBmi();
                BigDecimal startBmi = healthRecords.get(0).getBmi();
                if (startBmi != null && startBmi.compareTo(currentBmi) > 0) {
                    BigDecimal totalDiff = startBmi.subtract(targetBmi);
                    BigDecimal progress = startBmi.subtract(currentBmi);
                    if (totalDiff.compareTo(BigDecimal.ZERO) > 0) {
                        targetProgressPercent = progress.multiply(BigDecimal.valueOf(100))
                                .divide(totalDiff, 1, RoundingMode.HALF_UP);
                    }
                }
            }
        }

        ProgressVO vo = new ProgressVO();
        vo.setTotalCheckinRate(totalCheckinRate);
        vo.setExerciseCompleteRate(exerciseCompleteRate);
        vo.setDietCompleteRate(dietCompleteRate);
        vo.setWeightChange(weightChange);
        vo.setTargetProgressPercent(targetProgressPercent);
        vo.setGoal(goal);
        return vo;
    }

    private int resolveDays(Integer days) {
        if (days == null || days <= 0) {
            return 30;
        }
        return Math.min(days, 365);
    }
}
