package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.convert.HealthConvert;
import com.example.dto.HealthCreateDTO;
import com.example.dto.HealthUpdateDTO;
import com.example.entity.HealthRecord;
import com.example.mapper.HealthRecordMapper;
import com.example.service.HealthService;
import com.example.vo.HealthAssessmentVO;
import com.example.vo.HealthHistoryVO;
import com.example.vo.HealthRecordVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class HealthServiceImpl implements HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthServiceImpl.class);

    @Autowired
    private HealthRecordMapper healthRecordMapper;

    @Autowired
    private HealthConvert healthConvert;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthRecordVO createHealthRecord(Long userId, HealthCreateDTO dto) {
        HealthRecord record = healthConvert.toEntity(dto);
        record.setUserId(userId);

        BigDecimal bmi = calculateBmi(dto.getHeight(), dto.getWeight());
        record.setBmi(bmi);

        BigDecimal bmr = calculateBmr(dto.getHeight(), dto.getWeight(), BigDecimal.ZERO);
        record.setBmr(bmr);

        record.setDailyCalorie(calculateDailyCalorie(bmr));

        healthRecordMapper.insert(record);
        log.warn("创建健康档案 userId={} recordId={} bmi={} bmr={}", userId, record.getId(), bmi, bmr);
        return healthConvert.toHealthRecordVO(record);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public HealthRecordVO updateHealthRecord(Long userId, HealthUpdateDTO dto) {
        HealthRecord latest = getLatest(userId);
        if (latest == null) {
            throw new BusinessException(404, "请先创建健康档案");
        }

        if (dto.getHeight() != null) {
            latest.setHeight(dto.getHeight());
        }
        if (dto.getWeight() != null) {
            latest.setWeight(dto.getWeight());
        }
        if (dto.getGoal() != null) {
            latest.setGoal(dto.getGoal());
        }
        if (dto.getDiseaseHistory() != null) {
            latest.setDiseaseHistory(dto.getDiseaseHistory());
        }
        if (dto.getAllergyHistory() != null) {
            latest.setAllergyHistory(dto.getAllergyHistory());
        }
        if (dto.getExerciseHabit() != null) {
            latest.setExerciseHabit(dto.getExerciseHabit());
        }
        if (dto.getDietHabit() != null) {
            latest.setDietHabit(dto.getDietHabit());
        }

        BigDecimal height = latest.getHeight();
        BigDecimal weight = latest.getWeight();
        if (height != null && weight != null && height.compareTo(BigDecimal.ZERO) > 0) {
            latest.setBmi(calculateBmi(height, weight));
            latest.setBmr(calculateBmr(height, weight, BigDecimal.ZERO));
            latest.setDailyCalorie(calculateDailyCalorie(latest.getBmr()));
        }

        healthRecordMapper.updateById(latest);
        log.warn("更新健康档案 userId={} recordId={}", userId, latest.getId());
        return healthConvert.toHealthRecordVO(latest);
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
        return vo;
    }

    private HealthRecord getLatest(Long userId) {
        LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId)
                .orderByDesc(HealthRecord::getCreateTime)
                .last("LIMIT 1");
        return healthRecordMapper.selectOne(wrapper);
    }

    private BigDecimal calculateBmi(BigDecimal heightCm, BigDecimal weightKg) {
        BigDecimal heightM = heightCm.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal heightSquared = heightM.multiply(heightM);
        if (heightSquared.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return weightKg.divide(heightSquared, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateBmr(BigDecimal heightCm, BigDecimal weightKg, BigDecimal genderFactor) {
        return BigDecimal.valueOf(10)
                .multiply(weightKg)
                .add(BigDecimal.valueOf(6.25).multiply(heightCm))
                .add(BigDecimal.valueOf(5).multiply(genderFactor))
                .add(BigDecimal.valueOf(5))
                .setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateDailyCalorie(BigDecimal bmr) {
        if (bmr == null) {
            return BigDecimal.ZERO;
        }
        return bmr.multiply(BigDecimal.valueOf(1.2)).setScale(0, RoundingMode.HALF_UP);
    }

    private String evaluateBmiLevel(BigDecimal bmi) {
        double v = bmi.doubleValue();
        if (v < 18.5) {
            return "偏瘦";
        }
        if (v < 24.0) {
            return "正常";
        }
        if (v < 28.0) {
            return "偏胖";
        }
        return "肥胖";
    }

    private List<String> evaluateHealthRisks(BigDecimal bmi, String diseaseHistory, String allergyHistory) {
        List<String> risks = new ArrayList<>();
        double v = bmi.doubleValue();
        if (v < 18.5) {
            risks.add("体重偏低，可能存在营养不良风险，建议增加营养摄入");
        }
        if (v >= 24.0 && v < 28.0) {
            risks.add("体重偏重，建议加强运动锻炼，控制饮食热量摄入");
        }
        if (v >= 28.0) {
            risks.add("肥胖状态，心血管疾病、糖尿病风险升高，请尽快咨询医生制定减重计划");
        }
        if (diseaseHistory != null && !diseaseHistory.isBlank()) {
            risks.add("有既往病史记录，请结合自身情况在专业医生指导下制定健康计划");
        }
        if (allergyHistory != null && !allergyHistory.isBlank()) {
            risks.add("有过敏史记录，制定饮食计划时请注意避开过敏原");
        }
        if (risks.isEmpty()) {
            risks.add("当前健康状态良好，请继续保持健康生活习惯");
        }
        return risks;
    }
}
