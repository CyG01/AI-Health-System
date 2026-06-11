package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.convert.HealthConvert;
import com.example.dto.HealthCreateDTO;
import com.example.dto.HealthUpdateDTO;
import com.example.entity.HealthRecord;
import com.example.entity.SysUser;
import com.example.entity.UserProfile;
import com.example.mapper.HealthRecordMapper;
import com.example.mapper.SysUserMapper;
import com.example.mapper.UserProfileMapper;
import com.example.service.DeepSeekService;
import com.example.service.HealthService;
import com.example.util.DataMaskingService;
import com.example.vo.HealthAssessmentVO;
import com.example.vo.HealthHistoryVO;
import com.example.vo.HealthProgressVO;
import com.example.vo.HealthRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {

    private final HealthRecordMapper healthRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final HealthConvert healthConvert;
    private final StringRedisTemplate stringRedisTemplate;
    private final DeepSeekService deepSeekService;
    private final DataMaskingService dataMaskingService;

    private static final String USER_GENDER_CACHE_PREFIX = "user:gender:";
    private static final long USER_GENDER_CACHE_TTL = 1;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthRecordVO createHealthRecord(Long userId, HealthCreateDTO dto) {
        // 将旧的 is_latest=1 标记为历史快照
        markOldRecordsAsHistory(userId);

        HealthRecord record = healthConvert.toEntity(dto);
        record.setUserId(userId);
        record.setIsLatest(1);

        // 合并 UserProfile 中的 chronicDiseases 和 injuries 到 diseaseHistory
        record.setDiseaseHistory(mergeDiseaseHistory(dto.getDiseaseHistory(), userId));

        BigDecimal bmi = calculateBmi(dto.getHeight(), dto.getWeight());
        record.setBmi(bmi);

        int bmr = calculateBmr(dto.getHeight(), dto.getWeight(), userId);
        record.setBmr(bmr);

        record.setDailyCalorie(calculateDailyCalorie(bmr));

        healthRecordMapper.insert(record);
        log.info("创建健康档案 userId={} recordId={} bmi={} bmr={}", userId, record.getId(), bmi, bmr);
        return healthConvert.toHealthRecordVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthRecordVO updateHealthRecord(Long userId, HealthUpdateDTO dto) {
        HealthRecord latest = getLatest(userId);
        if (latest == null) {
            throw new BusinessException(404, "请先创建健康档案");
        }

        // 将旧记录标记为历史
        latest.setIsLatest(0);
        healthRecordMapper.updateById(latest);

        // 创建新记录
        HealthRecord newRecord = new HealthRecord();
        newRecord.setUserId(userId);
        newRecord.setIsLatest(1);
        newRecord.setHeight(dto.getHeight() != null ? dto.getHeight() : latest.getHeight());
        newRecord.setWeight(dto.getWeight() != null ? dto.getWeight() : latest.getWeight());
        newRecord.setGoal(dto.getGoal() != null ? dto.getGoal() : latest.getGoal());
        newRecord.setDiseaseHistory(dto.getDiseaseHistory() != null ? dto.getDiseaseHistory() : latest.getDiseaseHistory());
        newRecord.setAllergyHistory(dto.getAllergyHistory() != null ? dto.getAllergyHistory() : latest.getAllergyHistory());
        newRecord.setAllergyType(dto.getAllergyType() != null ? dto.getAllergyType() : latest.getAllergyType());
        newRecord.setFamilyHistory(dto.getFamilyHistory() != null ? dto.getFamilyHistory() : latest.getFamilyHistory());
        newRecord.setMedication(dto.getMedication() != null ? dto.getMedication() : latest.getMedication());
        newRecord.setExerciseHabit(dto.getExerciseHabit() != null ? dto.getExerciseHabit() : latest.getExerciseHabit());
        newRecord.setDietHabit(dto.getDietHabit() != null ? dto.getDietHabit() : latest.getDietHabit());
        newRecord.setTargetWeight(dto.getTargetWeight() != null ? dto.getTargetWeight() : latest.getTargetWeight());

        if (newRecord.getHeight() != null && newRecord.getWeight() != null && newRecord.getHeight() > 0) {
            newRecord.setBmi(calculateBmi(newRecord.getHeight(), newRecord.getWeight()));
            newRecord.setBmr(calculateBmr(newRecord.getHeight(), newRecord.getWeight(), userId));
            newRecord.setDailyCalorie(calculateDailyCalorie(newRecord.getBmr()));
        }

        healthRecordMapper.insert(newRecord);
        log.info("更新健康档案 userId={} oldRecordId={} newRecordId={}", userId, latest.getId(), newRecord.getId());
        return healthConvert.toHealthRecordVO(newRecord);
    }

    @Override
    public HealthRecordVO getLatestHealthRecord(Long userId) {
        HealthRecord latest = getLatest(userId);
        if (latest == null) {
            throw new BusinessException(404, "未找到健康档案");
        }
        return healthConvert.toHealthRecordVO(latest);
    }

    @Override
    public List<HealthHistoryVO> getHealthHistory(Long userId, Integer page, Integer size) {
        int pageNum = page != null && page > 0 ? page : 1;
        int pageSize = size != null && size > 0 ? size : 10;

        Page<HealthRecord> pageObj = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId)
                .orderByDesc(HealthRecord::getCreateTime);

        Page<HealthRecord> result = healthRecordMapper.selectPage(pageObj, wrapper);
        return result.getRecords().stream()
                .map(healthConvert::toHealthHistoryVO)
                .toList();
    }

    @Override
    public HealthAssessmentVO getHealthAssessment(Long userId) {
        HealthRecord latest = getLatest(userId);
        if (latest == null) {
            throw new BusinessException(404, "请先创建健康档案");
        }
        HealthAssessmentVO vo = healthConvert.toHealthAssessmentVO(latest);

        BigDecimal bmi = latest.getBmi();
        if (bmi != null) {
            vo.setBmiLevel(evaluateBmiLevel(bmi));
            vo.setRisks(evaluateHealthRisks(bmi, latest.getDiseaseHistory(), latest.getAllergyHistory()));
        }

        // 健康评分
        vo.setHealthScore(calculateHealthScore(latest, bmi));

        // 趋势分析
        List<HealthRecord> historyRecords = getRecentHistory(userId, 10);
        vo.setWeightTrend(buildTrend(historyRecords, r -> r.getWeight() != null ? BigDecimal.valueOf(r.getWeight()) : null));
        vo.setBmiTrend(buildTrend(historyRecords, HealthRecord::getBmi));

        // AI 改善建议
        try {
            String aiPrompt = buildHealthAiPrompt(latest, bmi, vo.getHealthScore(), historyRecords);
            String aiResponse = deepSeekService.chat(aiPrompt);
            vo.setAiSuggestion(aiResponse);
        } catch (Exception e) {
            log.warn("AI健康建议生成失败 userId={}", userId, e);
            vo.setAiSuggestion("AI建议暂时不可用，请稍后再试");
        }

        // === 新增: 体脂率估算 ===
        BigDecimal bodyFatRate = estimateBodyFatRate(userId, latest);
        vo.setEstimatedBodyFatRate(bodyFatRate);
        vo.setBodyFatLevel(evaluateBodyFatLevel(bodyFatRate, userId));

        // === 新增: 基础代谢评估 ===
        int bmr = latest.getBmr() != null ? latest.getBmr() : 0;
        vo.setBmrAssessment(assessBmr(bmr, userId, latest));

        // === 新增: 心血管风险评估 ===
        vo.setCardiovascularRisk(assessCardiovascularRisk(latest));

        // === 新增: 运动能力评估 ===
        vo.setExerciseAbility(assessExerciseAbility(latest, historyRecords));

        return vo;
    }

    private int calculateHealthScore(HealthRecord record, BigDecimal bmi) {
        int score = 100;

        // BMI 评分
        if (bmi != null) {
            double bmiVal = bmi.doubleValue();
            if (bmiVal < 18.5) score -= 10;
            else if (bmiVal < 25) score -= 0;
            else if (bmiVal < 28) score -= 10;
            else if (bmiVal < 32) score -= 20;
            else score -= 30;
        }

        // 疾病史扣分
        if (record.getDiseaseHistory() != null && !record.getDiseaseHistory().isBlank()) {
            score -= 10;
        }
        // 过敏史扣分
        if (record.getAllergyHistory() != null && !record.getAllergyHistory().isBlank()) {
            score -= 5;
        }

        return Math.max(0, Math.min(100, score));
    }

    private List<HealthRecord> getRecentHistory(Long userId, int limit) {
        Page<HealthRecord> page = new Page<>(1, limit);
        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId)
                .orderByDesc(HealthRecord::getCreateTime);
        return healthRecordMapper.selectPage(page, wrapper).getRecords();
    }

    private List<HealthAssessmentVO.TrendPoint> buildTrend(List<HealthRecord> records,
                                                            java.util.function.Function<HealthRecord, BigDecimal> extractor) {
        List<HealthAssessmentVO.TrendPoint> trend = new ArrayList<>();
        for (int i = records.size() - 1; i >= 0; i--) {
            HealthRecord r = records.get(i);
            BigDecimal val = extractor.apply(r);
            if (val != null) {
                HealthAssessmentVO.TrendPoint point = new HealthAssessmentVO.TrendPoint();
                point.setDate(r.getCreateTime() != null ? r.getCreateTime().toLocalDate().format(java.time.format.DateTimeFormatter.ofPattern("MM-dd")) : "");
                point.setValue(val);
                trend.add(point);
            }
        }
        return trend;
    }

    private String buildHealthAiPrompt(HealthRecord record, BigDecimal bmi,
                                        int score, List<HealthRecord> history) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户健康数据: ");
        sb.append("身高").append(record.getHeight()).append("cm, ");
        sb.append("体重").append(record.getWeight()).append("kg, ");
        sb.append("BMI").append(bmi).append(", ");

        // 疾病史脱敏
        if (record.getDiseaseHistory() != null && !record.getDiseaseHistory().isBlank()) {
            String masked = dataMaskingService.maskDiseaseHistory(record.getDiseaseHistory());
            sb.append("疾病史: ").append(masked).append(", ");
        } else {
            sb.append("疾病史: 无, ");
        }
        // 过敏史脱敏
        if (record.getAllergyHistory() != null && !record.getAllergyHistory().isBlank()) {
            String masked = dataMaskingService.maskAllergyHistory(record.getAllergyHistory());
            sb.append("过敏史: ").append(masked).append(", ");
        } else {
            sb.append("过敏史: 无, ");
        }
        // 家族病史脱敏
        if (record.getFamilyHistory() != null && !record.getFamilyHistory().isBlank()) {
            String masked = dataMaskingService.maskFamilyHistory(record.getFamilyHistory());
            sb.append("家族病史: ").append(masked).append(", ");
        }
        // 当前用药脱敏
        if (record.getMedication() != null && !record.getMedication().isBlank()) {
            String masked = dataMaskingService.maskMedication(record.getMedication());
            sb.append("当前用药: ").append(masked).append(", ");
        }
        sb.append("健康评分: ").append(score).append("/100. ");

        // 最近体重变化
        if (history.size() >= 2) {
            HealthRecord oldest = history.get(history.size() - 1);
            HealthRecord newest = history.get(0);
            if (oldest.getWeight() != null && newest.getWeight() != null && !oldest.getCreateTime().equals(newest.getCreateTime())) {
                int diff = newest.getWeight() - oldest.getWeight();
                sb.append("近").append(history.size()).append("次记录体重变化: ").append(diff > 0 ? "+" : "").append(diff).append("kg. ");
            }
        }

        sb.append("请基于以上数据，用200字以内给出3条个性化健康改善建议（包括饮食、运动方向），并给出简单的可执行目标。直接输出建议内容，不要格式标记。");
        return sb.toString();
    }

    private void markOldRecordsAsHistory(Long userId) {
        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId)
                .eq(HealthRecord::getIsLatest, 1);
        List<HealthRecord> oldList = healthRecordMapper.selectList(wrapper);
        for (HealthRecord old : oldList) {
            old.setIsLatest(0);
            healthRecordMapper.updateById(old);
        }
    }

    private HealthRecord getLatest(Long userId) {
        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId)
                .eq(HealthRecord::getIsLatest, 1)
                .last("LIMIT 1");
        return healthRecordMapper.selectOne(wrapper);
    }

    private BigDecimal calculateBmi(int heightCm, int weightKg) {
        BigDecimal heightM = BigDecimal.valueOf(heightCm).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal heightSquared = heightM.multiply(heightM);
        if (heightSquared.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(weightKg).divide(heightSquared, 1, RoundingMode.HALF_UP);
    }

    private int calculateBmr(int heightCm, int weightKg, Long userId) {
        int age = 30;
        int genderFactor = 5;

        if (userId != null) {
            // 先从缓存获取性别和年龄
            String cacheKey = USER_GENDER_CACHE_PREFIX + userId;
            String cachedGender = stringRedisTemplate.opsForValue().get(cacheKey + ":gender");
            String cachedAge = stringRedisTemplate.opsForValue().get(cacheKey + ":age");

            if (cachedGender != null && cachedAge != null) {
                genderFactor = Integer.parseInt(cachedGender);
                age = Integer.parseInt(cachedAge);
            } else {
                SysUser user = sysUserMapper.selectById(userId);
                if (user != null) {
                    if (user.getAge() != null && user.getAge() > 0) {
                        age = user.getAge();
                    }
                    int gf = (user.getGender() != null && user.getGender() == 0) ? -161 : 5;
                    genderFactor = gf;

                    // 写入缓存（TTL 1小时）
                    stringRedisTemplate.opsForValue().set(cacheKey + ":gender", String.valueOf(gf),
                            USER_GENDER_CACHE_TTL, TimeUnit.HOURS);
                    stringRedisTemplate.opsForValue().set(cacheKey + ":age", String.valueOf(age),
                            USER_GENDER_CACHE_TTL, TimeUnit.HOURS);
                }
            }
        }

        return (int) (10 * weightKg + 6.25 * heightCm - 5 * age + genderFactor);
    }

    private int calculateDailyCalorie(int bmr) {
        return (int) (bmr * 1.2);
    }

    private String evaluateBmiLevel(BigDecimal bmi) {
        double v = bmi.doubleValue();
        if (v < 18.5) return "偏瘦";
        if (v < 24.0) return "正常";
        if (v < 28.0) return "偏胖";
        return "肥胖";
    }

    private List<String> evaluateHealthRisks(BigDecimal bmi, String diseaseHistory, String allergyHistory) {
        List<String> risks = new ArrayList<>();
        double bmiValue = bmi.doubleValue();
        if (bmiValue >= 28.0) {
            risks.add("BMI属于肥胖范围，建议咨询营养师制定减重计划");
        }
        if (bmiValue < 18.5) {
            risks.add("BMI偏瘦，建议增加营养摄入");
        }
        if (diseaseHistory != null && !diseaseHistory.isBlank()) {
            risks.add("您有既往病史，运动前请咨询医生");
        }
        if (allergyHistory != null && !allergyHistory.isBlank()) {
            risks.add("您有过敏史，请注意饮食成分");
        }
        if (risks.isEmpty()) {
            risks.add("暂无特殊风险提示，请继续保持健康生活方式");
        }
        return risks;
    }

    @Override
    public HealthProgressVO getHealthProgress(Long userId) {
        HealthRecord latest = getLatest(userId);
        if (latest == null || latest.getTargetWeight() == null || latest.getWeight() == null) {
            throw new BusinessException(404, "请先完善健康档案（需设置目标体重和当前体重）");
        }

        HealthProgressVO vo = new HealthProgressVO();
        vo.setCurrentWeight(latest.getWeight());
        vo.setTargetWeight(latest.getTargetWeight());

        // 找最早设置了 targetWeight 的记录作为初始体重
        List<HealthRecord> historyRecords = getRecentHistory(userId, 100);
        Integer initialWeight = latest.getWeight();
        for (int i = historyRecords.size() - 1; i >= 0; i--) {
            HealthRecord r = historyRecords.get(i);
            if (r.getTargetWeight() != null && r.getWeight() != null) {
                initialWeight = r.getWeight();
                break;
            }
        }
        vo.setInitialWeight(initialWeight);

        // 计算进度
        int totalNeed = vo.getTargetWeight() - initialWeight; // 正=增重目标 负=减重目标
        int actualDone = vo.getCurrentWeight() - initialWeight;

        if (totalNeed < 0) {
            // 减重目标
            vo.setLostWeight(initialWeight - vo.getCurrentWeight());
            vo.setRemainingWeight(vo.getCurrentWeight() - vo.getTargetWeight());
            int percent = totalNeed == 0 ? 100 : Math.min(100, Math.max(0, actualDone * 100 / totalNeed));
            vo.setProgressPercent(percent);
        } else if (totalNeed > 0) {
            // 增重目标
            vo.setLostWeight(vo.getCurrentWeight() - initialWeight);
            vo.setRemainingWeight(vo.getTargetWeight() - vo.getCurrentWeight());
            int percent = Math.min(100, Math.max(0, actualDone * 100 / totalNeed));
            vo.setProgressPercent(percent);
        } else {
            vo.setProgressPercent(100);
            vo.setRemainingWeight(0);
            vo.setLostWeight(0);
        }
        vo.setCompleted(vo.getRemainingWeight() <= 0 && vo.getProgressPercent() >= 100);

        // 体重趋势
        List<HealthAssessmentVO.TrendPoint> trend = buildTrend(historyRecords,
                r -> r.getWeight() != null ? BigDecimal.valueOf(r.getWeight()) : null);
        vo.setWeightTrend(trend.size() > 10 ? trend.subList(trend.size() - 10, trend.size()) : trend);

        return vo;
    }

    // ======== 新增健康评估方法 ========

    /**
     * 估算体脂率 (基于BMI公式: 体脂率 = 1.2*BMI + 0.23*年龄 - 5.4 - 10.8*性别)
     * 男性性别因子=1, 女性=0
     */
    private BigDecimal estimateBodyFatRate(Long userId, HealthRecord record) {
        if (record.getBmi() == null || record.getBmi().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        int age = 30;
        int gender = 1; // 默认男性: gender=1
        if (userId != null) {
            String ageStr = stringRedisTemplate.opsForValue().get(USER_GENDER_CACHE_PREFIX + userId + ":age");
            String genderStr = stringRedisTemplate.opsForValue().get(USER_GENDER_CACHE_PREFIX + userId + ":gender");
            if (ageStr != null) age = Integer.parseInt(ageStr);
            if (genderStr != null) {
                // gender: 5=女(-161), 5=男 → 反推: 男=1, 女=0
                gender = Integer.parseInt(genderStr) == -161 ? 0 : 1;
            }
        }
        double bmi = record.getBmi().doubleValue();
        double bf = 1.2 * bmi + 0.23 * age - 5.4 - 10.8 * gender;
        return BigDecimal.valueOf(Math.max(0, Math.round(bf * 10.0) / 10.0));
    }

    private String evaluateBodyFatLevel(BigDecimal bodyFatRate, Long userId) {
        if (bodyFatRate == null) return "暂无数据";
        double bf = bodyFatRate.doubleValue();
        // 简化: 按通用标准
        if (bf < 10) return "偏瘦";
        if (bf < 20) return "健康";
        if (bf < 25) return "偏高";
        if (bf < 30) return "过高";
        return "肥胖";
    }

    private String assessBmr(int bmr, Long userId, HealthRecord record) {
        if (bmr <= 0) return "暂未计算，请完善身高体重信息";
        int age = 30;
        String ageStr = stringRedisTemplate.opsForValue().get(USER_GENDER_CACHE_PREFIX + userId + ":age");
        if (ageStr != null) age = Integer.parseInt(ageStr);
        int expectedBmr = age < 30 ? 1800 : age < 45 ? 1700 : 1500;
        if (bmr < expectedBmr * 0.85) return "基础代谢偏低，建议增加肌肉量、适当进行力量训练";
        if (bmr > expectedBmr * 1.15) return "基础代谢较高，新陈代谢旺盛，保持当前状态";
        return "基础代谢处于正常范围，继续保持均衡饮食与规律运动";
    }

    private String assessCardiovascularRisk(HealthRecord record) {
        if (record.getBmi() == null) return "暂无数据";
        double bmi = record.getBmi().doubleValue();
        boolean hasDisease = record.getDiseaseHistory() != null && !record.getDiseaseHistory().isBlank();
        if (bmi >= 28 && hasDisease) return "BMI超标且有既往病史，心血管风险较高，建议定期体检并咨询医生";
        if (bmi >= 28) return "BMI超标可能增加心血管负担，建议控制体重、加强有氧运动";
        if (bmi >= 24 && hasDisease) return "BMI偏高且有既往病史，建议关注血压血脂，适当增加有氧运动";
        if (hasDisease) return "有既往病史，建议保持定期复查和健康生活方式";
        return "当前心血管风险较低，继续保持良好的生活习惯";
    }

    private String assessExerciseAbility(HealthRecord record, List<HealthRecord> history) {
        double bmi = record.getBmi() != null ? record.getBmi().doubleValue() : 24;
        boolean hasHabit = record.getExerciseHabit() != null && !record.getExerciseHabit().isBlank();
        if (bmi >= 28 && !hasHabit) return "建议从低强度有氧运动开始（快走、游泳），每周3-4次，每次30分钟";
        if (bmi >= 28) return "建议以中低强度有氧运动为主，搭配每周2次力量训练";
        if (bmi < 18.5) return "体重偏轻，建议以力量训练增肌为主，减少长时间有氧运动";
        if (!hasHabit) return "建议每周进行3-5次中等强度运动，可从快走、慢跑开始";
        return "运动习惯良好，可根据目标肌群制定针对性训练计划";
    }

    /**
     * 合并 HealthRecord 输入的疾病史和 UserProfile 中 Onboarding 阶段收集的慢性病/损伤，
     * 避免两套数据割裂导致安全检查遗漏。
     */
    private String mergeDiseaseHistory(String inputDiseaseHistory, Long userId) {
        UserProfile profile = userProfileMapper.selectById(userId);
        if (profile == null) {
            return inputDiseaseHistory;
        }

        StringBuilder merged = new StringBuilder();
        if (inputDiseaseHistory != null && !inputDiseaseHistory.isBlank()) {
            merged.append(inputDiseaseHistory);
        }

        // 追加 UserProfile 中的慢性病
        if (profile.getChronicDiseases() != null && !profile.getChronicDiseases().isBlank()
                && !"无".equals(profile.getChronicDiseases())) {
            if (merged.length() > 0) merged.append("；");
            merged.append(profile.getChronicDiseases());
        }

        // 追加 UserProfile 中的运动损伤
        if (profile.getInjuries() != null && !profile.getInjuries().isBlank()
                && !"无".equals(profile.getInjuries())) {
            if (merged.length() > 0) merged.append("；");
            merged.append(profile.getInjuries());
        }

        String result = merged.toString();
        return result.isBlank() ? inputDiseaseHistory : result;
    }

}