package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.BusinessException;
import com.example.dto.BodyMeasurementSubmitDTO;
import com.example.entity.BodyMeasurement;
import com.example.mapper.BodyMeasurementMapper;
import com.example.service.BodyMeasurementService;
import com.example.vo.BodyMeasurementVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class BodyMeasurementServiceImpl implements BodyMeasurementService {

    private final BodyMeasurementMapper bodyMeasurementMapper;

    public BodyMeasurementServiceImpl(BodyMeasurementMapper bodyMeasurementMapper) {
        this.bodyMeasurementMapper = bodyMeasurementMapper;
    }

    @Override
    @Transactional
    public BodyMeasurementVO submit(Long userId, BodyMeasurementSubmitDTO dto) {
        // 检查当日是否已有记录
        LambdaQueryWrapper<BodyMeasurement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BodyMeasurement::getUserId, userId)
                .eq(BodyMeasurement::getRecordDate, dto.getRecordDate());
        BodyMeasurement existing = bodyMeasurementMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setWaist(dto.getWaist());
            existing.setHip(dto.getHip());
            existing.setChest(dto.getChest());
            existing.setThigh(dto.getThigh());
            existing.setArm(dto.getArm());
            existing.setBodyFatRate(dto.getBodyFatRate());
            existing.setNote(dto.getNote());
            bodyMeasurementMapper.updateById(existing);
            log.info("身体围度记录更新 userId={} date={}", userId, dto.getRecordDate());
            return toVO(existing);
        }

        BodyMeasurement record = new BodyMeasurement();
        record.setUserId(userId);
        record.setRecordDate(dto.getRecordDate());
        record.setWaist(dto.getWaist());
        record.setHip(dto.getHip());
        record.setChest(dto.getChest());
        record.setThigh(dto.getThigh());
        record.setArm(dto.getArm());
        record.setBodyFatRate(dto.getBodyFatRate());
        record.setNote(dto.getNote());
        bodyMeasurementMapper.insert(record);

        log.info("身体围度记录提交 userId={} date={}", userId, dto.getRecordDate());
        return toVO(record);
    }

    @Override
    public BodyMeasurementVO getLatest(Long userId) {
        LambdaQueryWrapper<BodyMeasurement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BodyMeasurement::getUserId, userId)
                .orderByDesc(BodyMeasurement::getRecordDate)
                .last("LIMIT 1");
        BodyMeasurement record = bodyMeasurementMapper.selectOne(wrapper);
        return record != null ? toVO(record) : null;
    }

    @Override
    public List<BodyMeasurementVO> getHistory(Long userId, int limit) {
        LambdaQueryWrapper<BodyMeasurement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BodyMeasurement::getUserId, userId)
                .orderByDesc(BodyMeasurement::getRecordDate)
                .last("LIMIT " + limit);
        return bodyMeasurementMapper.selectList(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<BodyMeasurementVO> getTrend(Long userId, int months) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusMonths(months);
        LambdaQueryWrapper<BodyMeasurement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(BodyMeasurement::getUserId, userId)
                .between(BodyMeasurement::getRecordDate, start, end)
                .orderByAsc(BodyMeasurement::getRecordDate);
        return bodyMeasurementMapper.selectList(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public void delete(Long userId, Long id) {
        BodyMeasurement record = bodyMeasurementMapper.selectById(id);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(404, "身体围度记录不存在");
        }
        bodyMeasurementMapper.deleteById(id);
        log.info("删除身体围度记录 userId={} recordId={}", userId, id);
    }

    private BodyMeasurementVO toVO(BodyMeasurement record) {
        BodyMeasurementVO vo = new BodyMeasurementVO();
        vo.setId(record.getId());
        vo.setRecordDate(record.getRecordDate());
        vo.setWaist(record.getWaist());
        vo.setHip(record.getHip());
        vo.setChest(record.getChest());
        vo.setThigh(record.getThigh());
        vo.setArm(record.getArm());
        vo.setBodyFatRate(record.getBodyFatRate());
        vo.setNote(record.getNote());
        vo.setCreateTime(record.getCreateTime());

        // 计算腰臀比
        if (record.getWaist() != null && record.getHip() != null && record.getHip().compareTo(BigDecimal.ZERO) > 0) {
            vo.setWaistHipRatio(record.getWaist().divide(record.getHip(), 2, RoundingMode.HALF_UP));
        }

        return vo;
    }
}