package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.dto.BloodSugarSubmitDTO;
import com.example.entity.BloodSugar;
import com.example.entity.SysNotification;
import com.example.event.BloodSugarAbnormalEvent;
import com.example.mapper.BloodSugarMapper;
import com.example.mapper.SysNotificationMapper;
import com.example.service.BloodSugarService;
import com.example.vo.BloodSugarVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BloodSugarServiceImpl implements BloodSugarService {

    // 血糖正常范围 (mmol/L)
    private static final BigDecimal GLUCOSE_HIGH = new BigDecimal("11.1");
    private static final BigDecimal GLUCOSE_LOW = new BigDecimal("3.9");

    private final BloodSugarMapper bloodSugarMapper;
    private final SysNotificationMapper sysNotificationMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BloodSugarVO submitRecord(Long userId, BloodSugarSubmitDTO dto) {
        BloodSugar record = new BloodSugar();
        record.setUserId(userId);
        record.setRecordDate(dto.getRecordDate());
        record.setRecordTime(dto.getRecordTime() != null ? dto.getRecordTime() : LocalTime.now());
        record.setMeasureType(dto.getMeasureType());
        record.setGlucoseValue(dto.getGlucoseValue());
        record.setNote(dto.getNote());

        // 判断异常
        int abnormalFlag = 0;
        if (dto.getGlucoseValue().compareTo(GLUCOSE_HIGH) > 0) {
            abnormalFlag = 1; // 偏高
        } else if (dto.getGlucoseValue().compareTo(GLUCOSE_LOW) < 0) {
            abnormalFlag = 2; // 偏低
        }
        record.setAbnormalFlag(abnormalFlag);
        bloodSugarMapper.insert(record);

        // 实时告警推送
        if (abnormalFlag > 0) {
            pushAbnormalAlert(userId, dto.getGlucoseValue(), abnormalFlag, record.getId());
            // 发布事件供其他模块监听
            eventPublisher.publishEvent(new BloodSugarAbnormalEvent(this, userId,
                    dto.getGlucoseValue(), abnormalFlag, record.getId()));
        }

        log.info("血糖记录提交 userId={} glucose={} abnormal={}", userId, dto.getGlucoseValue(), abnormalFlag);
        return toVO(record);
    }

    @Override
    public Page<BloodSugarVO> getRecordsPage(Long userId, int page, int size) {
        Page<BloodSugar> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<BloodSugar> wrapper = new LambdaQueryWrapper<BloodSugar>()
                .eq(BloodSugar::getUserId, userId)
                .orderByDesc(BloodSugar::getCreateTime);
        Page<BloodSugar> result = bloodSugarMapper.selectPage(pageParam, wrapper);

        Page<BloodSugarVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    @Override
    public List<BloodSugarVO> getRecordsByDate(Long userId, LocalDate date) {
        LambdaQueryWrapper<BloodSugar> wrapper = new LambdaQueryWrapper<BloodSugar>()
                .eq(BloodSugar::getUserId, userId)
                .eq(BloodSugar::getRecordDate, date)
                .orderByDesc(BloodSugar::getRecordTime);
        return bloodSugarMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    public List<BloodSugarVO> getTrend(Long userId, int days) {
        LocalDate start = LocalDate.now().minusDays(days - 1);
        LambdaQueryWrapper<BloodSugar> wrapper = new LambdaQueryWrapper<BloodSugar>()
                .eq(BloodSugar::getUserId, userId)
                .ge(BloodSugar::getRecordDate, start)
                .orderByAsc(BloodSugar::getRecordDate);
        return bloodSugarMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .sorted(Comparator.comparing(BloodSugarVO::getRecordDate))
                .toList();
    }

    @Override
    public void deleteRecord(Long userId, Long recordId) {
        BloodSugar record = bloodSugarMapper.selectById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(404, "血糖记录不存在");
        }
        bloodSugarMapper.deleteById(recordId);
        log.info("删除血糖记录 userId={} recordId={}", userId, recordId);
    }

    private void pushAbnormalAlert(Long userId, BigDecimal value, int flag, Long recordId) {
        String title = flag == 1 ? "血糖偏高提醒" : "血糖偏低提醒";
        String desc = flag == 1 ? "偏高" : "偏低";
        String content = String.format("您的血糖值为 %.1f mmol/L，属于%s状态，请注意监测并咨询医生。", value, desc);

        SysNotification notification = new SysNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setType("HEALTH_ALERT");
        notification.setTargetType("blood_sugar");
        notification.setTargetId(recordId);
        notification.setIsRead(0);
        sysNotificationMapper.insert(notification);

        log.info("血糖异常实时告警已推送 userId={} glucose={} title={}", userId, value, title);
    }

    private BloodSugarVO toVO(BloodSugar entity) {
        BloodSugarVO vo = new BloodSugarVO();
        vo.setId(entity.getId());
        vo.setUserId(entity.getUserId());
        vo.setRecordDate(entity.getRecordDate());
        vo.setRecordTime(entity.getRecordTime());
        vo.setMeasureType(entity.getMeasureType());
        vo.setGlucoseValue(entity.getGlucoseValue());
        vo.setNote(entity.getNote());
        vo.setAbnormalFlag(entity.getAbnormalFlag());
        vo.setCreateTime(entity.getCreateTime());
        return vo;
    }
}