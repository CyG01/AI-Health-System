package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.convert.UserConvert;
import com.example.entity.AiPlan;
import com.example.entity.DailyCheckin;
import com.example.entity.DietRecord;
import com.example.entity.ExerciseRecord;
import com.example.entity.HealthRecord;
import com.example.entity.SysUser;
import com.example.mapper.AiPlanMapper;
import com.example.mapper.DailyCheckinMapper;
import com.example.mapper.DietRecordMapper;
import com.example.mapper.ExerciseRecordMapper;
import com.example.mapper.HealthRecordMapper;
import com.example.mapper.SysUserMapper;
import com.example.service.AdminUserService;
import com.example.vo.AdminUserDetailVO;
import com.example.vo.UserInfoVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AdminUserServiceImpl implements AdminUserService {

    private static final Logger log = LoggerFactory.getLogger(AdminUserServiceImpl.class);

    @Autowired
    private SysUserMapper sysUserMapper;
    @Autowired
    private HealthRecordMapper healthRecordMapper;
    @Autowired
    private AiPlanMapper aiPlanMapper;
    @Autowired
    private DailyCheckinMapper dailyCheckinMapper;
    @Autowired
    private ExerciseRecordMapper exerciseRecordMapper;
    @Autowired
    private DietRecordMapper dietRecordMapper;
    @Autowired
    private UserConvert userConvert;

    @Override
    public Page<UserInfoVO> listUsers(int page, int size, String keyword,
                                      Integer status, String startDate, String endDate) {
        Page<SysUser> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getPhone, keyword));
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(SysUser::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le(SysUser::getCreateTime, LocalDate.parse(endDate).atTime(23, 59, 59));
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        Page<SysUser> result = sysUserMapper.selectPage(pageParam, wrapper);
        Page<UserInfoVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(userConvert::toUserInfoVO).toList());
        return voPage;
    }

    @Override
    public AdminUserDetailVO getUserDetail(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }

        AdminUserDetailVO vo = new AdminUserDetailVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());

        // 健康档案
        LambdaQueryWrapper<HealthRecord> healthWrapper = new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId)
                .eq(HealthRecord::getIsLatest, 1)
                .last("LIMIT 1");
        HealthRecord health = healthRecordMapper.selectOne(healthWrapper);
        if (health != null) {
            vo.setHeight(health.getHeight());
            vo.setWeight(health.getWeight());
            vo.setAge(user.getAge());
            vo.setGender(user.getGender());

            // BMI level
            if (health.getHeight() != null && health.getWeight() != null && health.getHeight() > 0) {
                double heightM = health.getHeight() / 100.0;
                BigDecimal bmi = BigDecimal.valueOf(health.getWeight())
                        .divide(BigDecimal.valueOf(heightM * heightM), 1, RoundingMode.HALF_UP);
                double bmiValue = bmi.doubleValue();
                if (bmiValue < 18.5) vo.setBmiLevel("偏瘦");
                else if (bmiValue < 24) vo.setBmiLevel("正常");
                else if (bmiValue < 28) vo.setBmiLevel("偏胖");
                else vo.setBmiLevel("肥胖");
            }
        }

        // 计划统计
        List<AiPlan> plans = aiPlanMapper.selectList(
                new LambdaQueryWrapper<AiPlan>().eq(AiPlan::getUserId, userId));
        vo.setTotalPlans(plans.size());
        vo.setActivePlanCount((int) plans.stream().filter(p -> p.getStatus() != null && p.getStatus() == 1).count());

        // 打卡统计
        List<DailyCheckin> checkins = dailyCheckinMapper.selectList(
                new LambdaQueryWrapper<DailyCheckin>()
                        .eq(DailyCheckin::getUserId, userId)
                        .orderByDesc(DailyCheckin::getCheckDate));
        vo.setTotalCheckinDays(checkins.size());
        vo.setConsecutiveDays(calculateConsecutiveDays(checkins, LocalDate.now()));
        if (!checkins.isEmpty()) {
            vo.setLastCheckinDate(checkins.get(0).getCheckDate());
        }

        // 运动统计
        List<ExerciseRecord> exercises = exerciseRecordMapper.selectList(
                new LambdaQueryWrapper<ExerciseRecord>().eq(ExerciseRecord::getUserId, userId));
        vo.setTotalExerciseRecords(exercises.size());
        vo.setTotalExerciseCalories(exercises.stream()
                .mapToInt(r -> r.getCaloriesBurned() != null ? r.getCaloriesBurned() : 0).sum());

        // 饮食统计
        List<DietRecord> diets = dietRecordMapper.selectList(
                new LambdaQueryWrapper<DietRecord>().eq(DietRecord::getUserId, userId));
        vo.setTotalDietRecords(diets.size());
        vo.setTotalDietCalories(diets.stream()
                .mapToInt(r -> r.getCaloriesConsumed() != null ? r.getCaloriesConsumed() : 0).sum());

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void banUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        if ("admin".equals(user.getRole())) {
            throw new BusinessException("不能禁用管理员账号");
        }
        user.setStatus(0);
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        log.info("管理员禁用用户 userId={}", userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbanUser(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setStatus(1);
        user.setUpdateTime(LocalDateTime.now());
        sysUserMapper.updateById(user);
        log.info("管理员启用用户 userId={}", userId);
    }

    @Override
    public List<UserInfoVO> exportUsers(String keyword, Integer status,
                                         String startDate, String endDate) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(SysUser::getUsername, keyword)
                    .or()
                    .like(SysUser::getPhone, keyword));
        }
        if (status != null) {
            wrapper.eq(SysUser::getStatus, status);
        }
        if (startDate != null && !startDate.isBlank()) {
            wrapper.ge(SysUser::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
        }
        if (endDate != null && !endDate.isBlank()) {
            wrapper.le(SysUser::getCreateTime, LocalDate.parse(endDate).atTime(23, 59, 59));
        }
        wrapper.orderByDesc(SysUser::getCreateTime);
        // 导出限制最大 5000 条
        Page<SysUser> pageParam = new Page<>(1, 5000);
        Page<SysUser> result = sysUserMapper.selectPage(pageParam, wrapper);
        return result.getRecords().stream().map(userConvert::toUserInfoVO).toList();
    }

    /**
     * 计算从今天开始的连续打卡天数
     */
    private int calculateConsecutiveDays(List<DailyCheckin> records, LocalDate today) {
        if (records.isEmpty()) return 0;
        int consecutive = 0;
        LocalDate expected = today;
        for (DailyCheckin r : records) {
            if (r.getCheckDate().equals(expected) || r.getCheckDate().equals(expected.minusDays(1 - consecutive - 1))) {
                // 简化：只按日期排序后计算连续
            }
        }
        // 标准连续计算：逐天倒查
        java.time.LocalDate check = today;
        while (true) {
            java.time.LocalDate finalCheck = check;
            boolean found = records.stream().anyMatch(r -> r.getCheckDate().equals(finalCheck));
            if (found) {
                consecutive++;
                check = check.minusDays(1);
            } else {
                break;
            }
        }
        return consecutive;
    }
}
