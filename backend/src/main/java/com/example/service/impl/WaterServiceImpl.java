package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dto.WaterRecordSubmitDTO;
import com.example.entity.WaterRecord;
import com.example.mapper.WaterRecordMapper;
import com.example.service.WaterService;
import com.example.vo.WaterRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WaterServiceImpl implements WaterService {

    private final WaterRecordMapper waterRecordMapper;

    public WaterServiceImpl(WaterRecordMapper waterRecordMapper) {
        this.waterRecordMapper = waterRecordMapper;
    }

    @Override
    @Transactional
    public WaterRecordVO submit(Long userId, WaterRecordSubmitDTO dto) {
        // 查找当日是否已有记录，有则累加
        LambdaQueryWrapper<WaterRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WaterRecord::getUserId, userId)
                .eq(WaterRecord::getRecordDate, dto.getRecordDate());
        WaterRecord existing = waterRecordMapper.selectOne(wrapper);

        if (existing != null) {
            existing.setAmountMl(existing.getAmountMl() + dto.getAmountMl());
            waterRecordMapper.updateById(existing);
            log.info("饮水记录累加 userId={} date={} amount={}ml total={}ml",
                    userId, dto.getRecordDate(), dto.getAmountMl(), existing.getAmountMl());
            return toVO(existing);
        }

        WaterRecord record = new WaterRecord();
        record.setUserId(userId);
        record.setRecordDate(dto.getRecordDate());
        record.setAmountMl(dto.getAmountMl());
        waterRecordMapper.insert(record);

        log.info("饮水记录提交 userId={} date={} amount={}ml", userId, dto.getRecordDate(), dto.getAmountMl());
        return toVO(record);
    }

    @Override
    public WaterRecordVO getToday(Long userId) {
        LambdaQueryWrapper<WaterRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WaterRecord::getUserId, userId)
                .eq(WaterRecord::getRecordDate, LocalDate.now());
        WaterRecord record = waterRecordMapper.selectOne(wrapper);
        return record != null ? toVO(record) : null;
    }

    @Override
    public List<WaterRecordVO> getList(Long userId, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);
        LambdaQueryWrapper<WaterRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WaterRecord::getUserId, userId)
                .between(WaterRecord::getRecordDate, start, end)
                .orderByDesc(WaterRecord::getRecordDate);
        return waterRecordMapper.selectList(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public int getDailyTotal(Long userId, LocalDate date) {
        LambdaQueryWrapper<WaterRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WaterRecord::getUserId, userId)
                .eq(WaterRecord::getRecordDate, date);
        WaterRecord record = waterRecordMapper.selectOne(wrapper);
        return record != null ? record.getAmountMl() : 0;
    }

    private WaterRecordVO toVO(WaterRecord record) {
        WaterRecordVO vo = new WaterRecordVO();
        vo.setId(record.getId());
        vo.setRecordDate(record.getRecordDate());
        vo.setAmountMl(record.getAmountMl());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }
}