package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.convert.ExerciseConvert;
import com.example.dto.ExerciseItemCreateDTO;
import com.example.dto.ExerciseItemUpdateDTO;
import com.example.dto.ExerciseRecordSubmitDTO;
import com.example.entity.DailyCheckin;
import com.example.entity.ExerciseItem;
import com.example.entity.ExerciseRecord;
import com.example.mapper.DailyCheckinMapper;
import com.example.mapper.ExerciseItemMapper;
import com.example.mapper.ExerciseRecordMapper;
import com.example.service.ExerciseService;
import com.example.vo.ExerciseItemVO;
import com.example.vo.ExerciseRecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseServiceImpl implements ExerciseService {

    private static final String EXERCISE_ITEM_LIST_CACHE = "admin:exercise:item:list";

    private final ExerciseItemMapper exerciseItemMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final DailyCheckinMapper dailyCheckinMapper;
    private final ExerciseConvert exerciseConvert;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<ExerciseItemVO> listActiveItems() {
        LambdaQueryWrapper<ExerciseItem> wrapper = new LambdaQueryWrapper<ExerciseItem>()
                .eq(ExerciseItem::getStatus, 1);
        return exerciseItemMapper.selectList(wrapper).stream()
                .map(exerciseConvert::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<ExerciseItemVO> listItemsByType(String type) {
        LambdaQueryWrapper<ExerciseItem> wrapper = new LambdaQueryWrapper<ExerciseItem>()
                .eq(ExerciseItem::getStatus, 1)
                .eq(ExerciseItem::getType, type);
        return exerciseItemMapper.selectList(wrapper).stream()
                .map(exerciseConvert::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExerciseRecordVO submitRecord(Long userId, ExerciseRecordSubmitDTO dto) {
        // 校验打卡归属
        if (dto.getCheckinId() != null) {
            DailyCheckin checkin = dailyCheckinMapper.selectById(dto.getCheckinId());
            if (checkin == null || !checkin.getUserId().equals(userId)) {
                throw new BusinessException("无效的打卡记录");
            }
        }

        ExerciseRecord record = exerciseConvert.toEntity(dto);
        record.setUserId(userId);
        exerciseRecordMapper.insert(record);

        ExerciseItem item = exerciseItemMapper.selectById(dto.getItemId());
        ExerciseRecordVO vo = exerciseConvert.toVO(record);
        if (item != null) {
            vo.setItemName(item.getName());
        }

        log.info("提交运动记录 userId={} recordId={} caloriesBurned={}", userId, record.getId(), record.getCaloriesBurned());
        return vo;
    }

    @Override
    public List<ExerciseRecordVO> getRecordsByCheckinId(Long checkinId) {
        LambdaQueryWrapper<ExerciseRecord> wrapper = new LambdaQueryWrapper<ExerciseRecord>()
                .eq(ExerciseRecord::getCheckinId, checkinId)
                .orderByDesc(ExerciseRecord::getCreateTime);
        List<ExerciseRecord> records = exerciseRecordMapper.selectList(wrapper);
        return mapToRecordVOs(records);
    }

    @Override
    public List<ExerciseRecordVO> getRecordsByUserId(Long userId, Integer limit) {
        int queryLimit = limit != null && limit > 0 ? limit : 30;
        Page<ExerciseRecord> pageParam = new Page<>(1, queryLimit);
        LambdaQueryWrapper<ExerciseRecord> wrapper = new LambdaQueryWrapper<ExerciseRecord>()
                .eq(ExerciseRecord::getUserId, userId)
                .orderByDesc(ExerciseRecord::getCreateTime);
        List<ExerciseRecord> records = exerciseRecordMapper.selectPage(pageParam, wrapper).getRecords();
        return mapToRecordVOs(records);
    }

    @Override
    public Page<ExerciseRecordVO> getRecordsPage(Long userId, int page, int size) {
        Page<ExerciseRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<ExerciseRecord> wrapper = new LambdaQueryWrapper<ExerciseRecord>()
                .eq(ExerciseRecord::getUserId, userId)
                .orderByDesc(ExerciseRecord::getCreateTime);
        Page<ExerciseRecord> result = exerciseRecordMapper.selectPage(pageParam, wrapper);

        Map<Long, String> itemNameMap = batchLoadItemNames(result.getRecords());

        Page<ExerciseRecordVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(r -> {
            ExerciseRecordVO vo = exerciseConvert.toVO(r);
            vo.setItemName(itemNameMap.getOrDefault(r.getItemId(), "未知运动"));
            return vo;
        }).toList());
        return voPage;
    }

    @Override
    public Page<ExerciseRecordVO> getRecordsByDate(Long userId, LocalDate date, int page, int size) {
        Page<ExerciseRecord> pageParam = new Page<>(page, size);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        LambdaQueryWrapper<ExerciseRecord> wrapper = new LambdaQueryWrapper<ExerciseRecord>()
                .eq(ExerciseRecord::getUserId, userId)
                .between(ExerciseRecord::getCreateTime, startOfDay, endOfDay)
                .orderByDesc(ExerciseRecord::getCreateTime);
        Page<ExerciseRecord> result = exerciseRecordMapper.selectPage(pageParam, wrapper);

        Map<Long, String> itemNameMap = batchLoadItemNames(result.getRecords());

        Page<ExerciseRecordVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(r -> {
            ExerciseRecordVO vo = exerciseConvert.toVO(r);
            vo.setItemName(itemNameMap.getOrDefault(r.getItemId(), "未知运动"));
            return vo;
        }).toList());
        return voPage;
    }

    // --- Admin 运动项目管理 ---

    @Override
    public List<ExerciseItemVO> listAllItems() {
        LambdaQueryWrapper<ExerciseItem> wrapper = new LambdaQueryWrapper<ExerciseItem>()
                .orderByAsc(ExerciseItem::getId);
        return exerciseItemMapper.selectList(wrapper).stream()
                .map(exerciseConvert::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createExerciseItem(ExerciseItemCreateDTO dto) {
        ExerciseItem item = new ExerciseItem();
        item.setName(dto.getName());
        item.setType(dto.getType());
        item.setCalorieCoefficient(dto.getCalorieCoefficient());
        item.setVideoUrl(dto.getVideoUrl());
        item.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        exerciseItemMapper.insert(item);
        stringRedisTemplate.delete(EXERCISE_ITEM_LIST_CACHE);
        log.info("新增运动项目 id={} name={}", item.getId(), item.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateExerciseItem(ExerciseItemUpdateDTO dto) {
        ExerciseItem item = exerciseItemMapper.selectById(dto.getId());
        if (item == null) {
            throw new BusinessException(404, "运动项目不存在");
        }
        item.setName(dto.getName());
        item.setType(dto.getType());
        item.setCalorieCoefficient(dto.getCalorieCoefficient());
        item.setVideoUrl(dto.getVideoUrl());
        item.setStatus(dto.getStatus());
        exerciseItemMapper.updateById(item);
        stringRedisTemplate.delete(EXERCISE_ITEM_LIST_CACHE);
        log.info("修改运动项目 id={} name={}", dto.getId(), dto.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteExerciseItem(Long id) {
        ExerciseItem item = exerciseItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "运动项目不存在");
        }
        exerciseItemMapper.deleteById(id);
        stringRedisTemplate.delete(EXERCISE_ITEM_LIST_CACHE);
        log.info("删除运动项目 id={}", id);
    }

    // --- 私有方法 ---

    /** 批量加载 item 名称，解决 N+1 查询 */
    private Map<Long, String> batchLoadItemNames(List<ExerciseRecord> records) {
        if (records.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> itemIds = records.stream()
                .map(ExerciseRecord::getItemId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return exerciseItemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(ExerciseItem::getId, ExerciseItem::getName, (a, b) -> a));
    }

    private List<ExerciseRecordVO> mapToRecordVOs(List<ExerciseRecord> records) {
        Map<Long, String> itemNameMap = batchLoadItemNames(records);
        return records.stream().map(r -> {
            ExerciseRecordVO vo = exerciseConvert.toVO(r);
            vo.setItemName(itemNameMap.getOrDefault(r.getItemId(), "未知运动"));
            return vo;
        }).toList();
    }
}