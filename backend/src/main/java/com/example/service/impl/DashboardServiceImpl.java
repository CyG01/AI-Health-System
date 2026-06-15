package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.AiPlan;
import com.example.entity.AiPlanDetail;
import com.example.entity.DailyCheckin;
import com.example.entity.DietRecord;
import com.example.entity.ExerciseRecord;
import com.example.entity.HealthRecord;
import com.example.mapper.AiPlanDetailMapper;
import com.example.mapper.AiPlanMapper;
import com.example.mapper.DailyCheckinMapper;
import com.example.mapper.DietRecordMapper;
import com.example.mapper.ExerciseRecordMapper;
import com.example.mapper.HealthRecordMapper;
import com.example.service.CheckinService;
import com.example.service.DashboardService;
import com.example.vo.CheckinStatsVO;
import com.example.vo.DashboardMonthVO;
import com.example.vo.DashboardTodayVO;
import com.example.vo.DashboardWeekVO;
import com.example.vo.DashboardGreetingVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DailyCheckinMapper dailyCheckinMapper;
    private final AiPlanMapper aiPlanMapper;
    private final AiPlanDetailMapper aiPlanDetailMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final DietRecordMapper dietRecordMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final CheckinService checkinService;

    @Override
    public DashboardTodayVO getTodayOverview(Long userId) {
        LocalDate today = LocalDate.now();
        DashboardTodayVO vo = new DashboardTodayVO();

        // 1. 今日打卡状态
        LambdaQueryWrapper<DailyCheckin> checkinWrapper = new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .eq(DailyCheckin::getCheckDate, today);
        DailyCheckin checkin = dailyCheckinMapper.selectOne(checkinWrapper);
        vo.setIsCheckedIn(checkin != null);

        // 1.5 连续打卡天数
        CheckinStatsVO stats = checkinService.getStats(userId);
        vo.setStreakDays(stats.getConsecutiveDays());

        // 2. 当前生效的计划
        LambdaQueryWrapper<AiPlan> planWrapper = new LambdaQueryWrapper<AiPlan>()
                .eq(AiPlan::getUserId, userId)
                .eq(AiPlan::getStatus, 1);
        AiPlan activePlan = aiPlanMapper.selectOne(planWrapper);
        if (activePlan != null) {
            vo.setPlanId(activePlan.getId());
            vo.setPlanName(activePlan.getPlanName());

            LambdaQueryWrapper<AiPlanDetail> detailWrapper = new LambdaQueryWrapper<AiPlanDetail>()
                    .eq(AiPlanDetail::getPlanId, activePlan.getId());
            List<AiPlanDetail> details = aiPlanDetailMapper.selectList(detailWrapper);
            List<DashboardTodayVO.TaskItem> tasks = details.stream().map(d -> {
                DashboardTodayVO.TaskItem task = new DashboardTodayVO.TaskItem();
                task.setDetailId(d.getId());
                task.setItemType(d.getItemType());
                task.setItemName(d.getItemName());
                task.setTargetAmount(d.getTargetAmount());
                task.setStatus(d.getStatus());
                return task;
            }).collect(Collectors.toList());
            vo.setTasks(tasks);
            vo.setTotalTasks(details.size());
            vo.setCompletedTasks((int) details.stream().filter(d -> d.getStatus() != null && d.getStatus() == 1).count());
        } else {
            vo.setTasks(List.of());
            vo.setTotalTasks(0);
            vo.setCompletedTasks(0);
        }

        // 3. 今日运动统计
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        LambdaQueryWrapper<ExerciseRecord> exerciseWrapper = new LambdaQueryWrapper<ExerciseRecord>()
                .eq(ExerciseRecord::getUserId, userId)
                .between(ExerciseRecord::getCreateTime, startOfDay, endOfDay);
        List<ExerciseRecord> exerciseRecords = exerciseRecordMapper.selectList(exerciseWrapper);
        vo.setExerciseRecordsCount(exerciseRecords.size());
        vo.setExerciseCaloriesBurned(exerciseRecords.stream()
                .mapToInt(r -> r.getCaloriesBurned() != null ? r.getCaloriesBurned() : 0)
                .sum());

        // 4. 今日饮食统计
        LambdaQueryWrapper<DietRecord> dietWrapper = new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getUserId, userId)
                .between(DietRecord::getCreateTime, startOfDay, endOfDay);
        List<DietRecord> dietRecords = dietRecordMapper.selectList(dietWrapper);
        vo.setDietRecordsCount(dietRecords.size());
        vo.setDietCaloriesConsumed(dietRecords.stream()
                .mapToInt(r -> r.getCaloriesConsumed() != null ? r.getCaloriesConsumed() : 0)
                .sum());

        // 5. 最新体重和BMI
        LambdaQueryWrapper<HealthRecord> healthWrapper = new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId)
                .isNotNull(HealthRecord::getWeight)
                .orderByDesc(HealthRecord::getCreateTime)
                .last("LIMIT 1");
        HealthRecord latestHealth = healthRecordMapper.selectOne(healthWrapper);
        if (latestHealth != null) {
            vo.setWeight(latestHealth.getWeight());
            vo.setBmi(latestHealth.getBmi());
        }

        return vo;
    }

    @Override
    public DashboardWeekVO getWeekOverview(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate weekEnd = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        DashboardWeekVO vo = new DashboardWeekVO();
        vo.setWeekStart(weekStart.toString());
        vo.setWeekEnd(weekEnd.toString());

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM-dd");

        // 本周打卡天数
        LambdaQueryWrapper<DailyCheckin> checkinWrapper = new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .between(DailyCheckin::getCheckDate, weekStart, weekEnd)
                .orderByAsc(DailyCheckin::getCheckDate);
        List<DailyCheckin> weekCheckins = dailyCheckinMapper.selectList(checkinWrapper);
        vo.setCheckinDays(weekCheckins.size());

        // 本周运动/饮食统计
        LocalDateTime weekStartTime = weekStart.atStartOfDay();
        LocalDateTime weekEndTime = weekEnd.atTime(LocalTime.MAX);

        List<ExerciseRecord> weekExercises = exerciseRecordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, userId)
                        .between(ExerciseRecord::getCreateTime, weekStartTime, weekEndTime));
        vo.setExerciseRecordsCount(weekExercises.size());
        vo.setExerciseCalories(weekExercises.stream()
                .mapToInt(r -> r.getCaloriesBurned() != null ? r.getCaloriesBurned() : 0)
                .sum());

        List<DietRecord> weekDiets = dietRecordMapper.selectList(
                new LambdaQueryWrapper<DietRecord>()
                        .eq(DietRecord::getUserId, userId)
                        .between(DietRecord::getCreateTime, weekStartTime, weekEndTime));
        vo.setDietRecordsCount(weekDiets.size());
        vo.setDietCalories(weekDiets.stream()
                .mapToInt(r -> r.getCaloriesConsumed() != null ? r.getCaloriesConsumed() : 0)
                .sum());

        // 每日明细
        List<DashboardWeekVO.DaySummary> dailySummary = new ArrayList<>();
        for (LocalDate d = weekStart; !d.isAfter(today); d = d.plusDays(1)) {
            final LocalDate dayDate = d;
            DashboardWeekVO.DaySummary day = new DashboardWeekVO.DaySummary();
            day.setDate(dayDate.format(fmt));

            boolean checkedIn = weekCheckins.stream().anyMatch(c -> c.getCheckDate().equals(dayDate));
            day.setCheckedIn(checkedIn);

            final LocalDateTime dayStart = dayDate.atStartOfDay();
            final LocalDateTime dayEnd = dayDate.atTime(LocalTime.MAX);
            day.setExerciseCalories(weekExercises.stream()
                    .filter(r -> !r.getCreateTime().isBefore(dayStart) && !r.getCreateTime().isAfter(dayEnd))
                    .mapToInt(r -> r.getCaloriesBurned() != null ? r.getCaloriesBurned() : 0)
                    .sum());
            day.setDietCalories(weekDiets.stream()
                    .filter(r -> !r.getCreateTime().isBefore(dayStart) && !r.getCreateTime().isAfter(dayEnd))
                    .mapToInt(r -> r.getCaloriesConsumed() != null ? r.getCaloriesConsumed() : 0)
                    .sum());
            day.setExerciseCount((int) weekExercises.stream()
                    .filter(r -> !r.getCreateTime().isBefore(dayStart) && !r.getCreateTime().isAfter(dayEnd))
                    .count());
            day.setDietCount((int) weekDiets.stream()
                    .filter(r -> !r.getCreateTime().isBefore(dayStart) && !r.getCreateTime().isAfter(dayEnd))
                    .count());

            dailySummary.add(day);
        }
        vo.setDailySummary(dailySummary);

        return vo;
    }

    @Override
    public DashboardMonthVO getMonthOverview(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate monthEnd = YearMonth.from(today).atEndOfMonth();

        DashboardMonthVO vo = new DashboardMonthVO();
        vo.setMonth(today.format(DateTimeFormatter.ofPattern("yyyy-MM")));

        // 本月打卡
        LambdaQueryWrapper<DailyCheckin> checkinWrapper = new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .between(DailyCheckin::getCheckDate, monthStart, monthEnd);
        List<DailyCheckin> monthCheckins = dailyCheckinMapper.selectList(checkinWrapper);
        vo.setCheckinDays(monthCheckins.size());

        int totalMonthDays = (int) ChronoUnit.DAYS.between(monthStart, monthEnd) + 1;
        int passedDays = (int) ChronoUnit.DAYS.between(monthStart, today) + 1;
        vo.setTotalDays(passedDays);
        vo.setCheckinRate(passedDays > 0 ? Math.round(monthCheckins.size() * 1000.0 / passedDays) / 10.0 : 0);

        // 本月运动/饮食
        LocalDateTime monthStartTime = monthStart.atStartOfDay();
        LocalDateTime monthEndTime = monthEnd.atTime(LocalTime.MAX);

        List<ExerciseRecord> monthExercises = exerciseRecordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, userId)
                        .between(ExerciseRecord::getCreateTime, monthStartTime, monthEndTime));
        vo.setExerciseRecordsCount(monthExercises.size());
        vo.setExerciseCalories(monthExercises.stream()
                .mapToInt(r -> r.getCaloriesBurned() != null ? r.getCaloriesBurned() : 0)
                .sum());

        List<DietRecord> monthDiets = dietRecordMapper.selectList(
                new LambdaQueryWrapper<DietRecord>()
                        .eq(DietRecord::getUserId, userId)
                        .between(DietRecord::getCreateTime, monthStartTime, monthEndTime));
        vo.setDietRecordsCount(monthDiets.size());
        vo.setDietCalories(monthDiets.stream()
                .mapToInt(r -> r.getCaloriesConsumed() != null ? r.getCaloriesConsumed() : 0)
                .sum());

        // 按周汇总
        List<DashboardMonthVO.WeekSummary> weeklySummary = new ArrayList<>();
        LocalDate weekStart = monthStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        if (weekStart.isBefore(monthStart)) {
            weekStart = monthStart;
        }

        int weekIndex = 1;
        for (LocalDate ws = weekStart; !ws.isAfter(monthEnd) && !ws.isAfter(today); ws = ws.plusWeeks(1)) {
            final LocalDate wsFinal = ws;
            LocalDate we = ws.plusDays(6);
            if (we.isAfter(today)) we = today;
            if (we.isAfter(monthEnd)) we = monthEnd;
            final LocalDate weFinal = we;

            DashboardMonthVO.WeekSummary wsVO = new DashboardMonthVO.WeekSummary();
            wsVO.setWeekLabel("第" + weekIndex + "周 (" + wsFinal.format(DateTimeFormatter.ofPattern("MM/dd")) + "-" + weFinal.format(DateTimeFormatter.ofPattern("MM/dd")) + ")");
            wsVO.setCheckinDays((int) monthCheckins.stream()
                    .filter(c -> !c.getCheckDate().isBefore(wsFinal) && !c.getCheckDate().isAfter(weFinal))
                    .count());
            wsVO.setExerciseCalories(monthExercises.stream()
                    .filter(r -> !r.getCreateTime().isBefore(wsFinal.atStartOfDay()) && !r.getCreateTime().isAfter(weFinal.atTime(LocalTime.MAX)))
                    .mapToInt(r -> r.getCaloriesBurned() != null ? r.getCaloriesBurned() : 0)
                    .sum());
            wsVO.setDietCalories(monthDiets.stream()
                    .filter(r -> !r.getCreateTime().isBefore(wsFinal.atStartOfDay()) && !r.getCreateTime().isAfter(weFinal.atTime(LocalTime.MAX)))
                    .mapToInt(r -> r.getCaloriesConsumed() != null ? r.getCaloriesConsumed() : 0)
                    .sum());

            weeklySummary.add(wsVO);
            weekIndex++;
        }
        vo.setWeeklySummary(weeklySummary);

        return vo;
    }

    @Override
    public DashboardGreetingVO generateGreeting(Long userId) {
        LocalDate today = LocalDate.now();

        // 1. 查询今日打卡状态
        LambdaQueryWrapper<DailyCheckin> checkinWrapper = new LambdaQueryWrapper<DailyCheckin>()
                .eq(DailyCheckin::getUserId, userId)
                .eq(DailyCheckin::getCheckDate, today);
        DailyCheckin todayCheckin = dailyCheckinMapper.selectOne(checkinWrapper);
        boolean isCheckedIn = todayCheckin != null;

        // 2. 连续打卡天数
        CheckinStatsVO stats = checkinService.getStats(userId);
        int streakDays = stats.getConsecutiveDays();

        // 3. 查询当前活跃计划
        LambdaQueryWrapper<AiPlan> planWrapper = new LambdaQueryWrapper<AiPlan>()
                .eq(AiPlan::getUserId, userId)
                .eq(AiPlan::getStatus, 1);
        AiPlan activePlan = aiPlanMapper.selectOne(planWrapper);
        boolean hasActivePlan = activePlan != null;
        String planName = hasActivePlan ? activePlan.getPlanName() : null;
        Long planId = hasActivePlan ? activePlan.getId() : null;

        // 4. 今日任务完成情况
        int completedTasks = 0;
        int totalTasks = 0;
        if (hasActivePlan) {
            LambdaQueryWrapper<AiPlanDetail> detailWrapper = new LambdaQueryWrapper<AiPlanDetail>()
                    .eq(AiPlanDetail::getPlanId, activePlan.getId());
            List<AiPlanDetail> details = aiPlanDetailMapper.selectList(detailWrapper);
            totalTasks = details.size();
            completedTasks = (int) details.stream()
                    .filter(d -> d.getStatus() != null && d.getStatus() == 1)
                    .count();
        }

        // 5. 今日运动/饮食消耗
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        int exerciseCal = exerciseRecordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>()
                        .eq(ExerciseRecord::getUserId, userId)
                        .between(ExerciseRecord::getCreateTime, startOfDay, endOfDay))
                .stream()
                .mapToInt(r -> r.getCaloriesBurned() != null ? r.getCaloriesBurned() : 0)
                .sum();
        int dietCal = dietRecordMapper.selectList(
                new LambdaQueryWrapper<DietRecord>()
                        .eq(DietRecord::getUserId, userId)
                        .between(DietRecord::getCreateTime, startOfDay, endOfDay))
                .stream()
                .mapToInt(r -> r.getCaloriesConsumed() != null ? r.getCaloriesConsumed() : 0)
                .sum();

        // 6. 调用规则引擎生成卡片
        return com.example.engine.GreetingRuleEngine.evaluate(
                isCheckedIn, streakDays,
                hasActivePlan, planName, planId,
                completedTasks, totalTasks,
                exerciseCal, dietCal
        );
    }
}