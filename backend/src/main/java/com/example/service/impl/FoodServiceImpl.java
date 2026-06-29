package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.convert.FoodConvert;
import com.example.dto.DietRecordSubmitDTO;
import com.example.dto.FoodItemCreateDTO;
import com.example.dto.FoodItemUpdateDTO;
import com.example.entity.DailyCheckin;
import com.example.entity.DietRecord;
import com.example.entity.FoodItem;
import com.example.mapper.DailyCheckinMapper;
import com.example.mapper.DietRecordMapper;
import com.example.mapper.FoodItemMapper;
import com.example.service.FoodService;
import com.example.vo.DietRecordVO;
import com.example.vo.FoodItemVO;
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
public class FoodServiceImpl implements FoodService {

    private static final String FOOD_ITEM_LIST_CACHE = "admin:food:item:list";

    private final FoodItemMapper foodItemMapper;
    private final DietRecordMapper dietRecordMapper;
    private final DailyCheckinMapper dailyCheckinMapper;
    private final FoodConvert foodConvert;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    public List<FoodItemVO> listActiveItems() {
        LambdaQueryWrapper<FoodItem> wrapper = new LambdaQueryWrapper<FoodItem>()
                .eq(FoodItem::getStatus, 1)
                .orderByAsc(FoodItem::getSort);
        return foodItemMapper.selectList(wrapper).stream()
                .map(foodConvert::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<FoodItemVO> listItemsByCategory(String category) {
        LambdaQueryWrapper<FoodItem> wrapper = new LambdaQueryWrapper<FoodItem>()
                .eq(FoodItem::getStatus, 1)
                .eq(FoodItem::getCategory, category)
                .orderByAsc(FoodItem::getSort);
        return foodItemMapper.selectList(wrapper).stream()
                .map(foodConvert::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DietRecordVO submitRecord(Long userId, DietRecordSubmitDTO dto) {
        // 校验打卡归属
        DailyCheckin checkin = dailyCheckinMapper.selectById(dto.getCheckinId());
        if (checkin == null || !checkin.getUserId().equals(userId)) {
            throw new BusinessException("无效的打卡记录");
        }

        DietRecord record = foodConvert.toEntity(dto);
        record.setUserId(userId);
        dietRecordMapper.insert(record);

        FoodItem item = foodItemMapper.selectById(dto.getItemId());
        DietRecordVO vo = foodConvert.toVO(record);
        if (item != null) {
            vo.setItemName(item.getName());
        }

        log.info("提交饮食记录 userId={} recordId={} caloriesConsumed={}", userId, record.getId(), record.getCaloriesConsumed());
        return vo;
    }

    @Override
    public List<DietRecordVO> getRecordsByCheckinId(Long userId, Long checkinId) {
        LambdaQueryWrapper<DietRecord> wrapper = new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getCheckinId, checkinId)
                .eq(DietRecord::getUserId, userId)
                .orderByDesc(DietRecord::getCreateTime);
        List<DietRecord> records = dietRecordMapper.selectList(wrapper);
        return mapToRecordVOs(records);
    }

    @Override
    public List<DietRecordVO> getRecordsByUserId(Long userId, Integer limit) {
        int queryLimit = limit != null && limit > 0 ? limit : 30;
        Page<DietRecord> pageParam = new Page<>(1, queryLimit);
        LambdaQueryWrapper<DietRecord> wrapper = new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getUserId, userId)
                .orderByDesc(DietRecord::getCreateTime);
        List<DietRecord> records = dietRecordMapper.selectPage(pageParam, wrapper).getRecords();
        return mapToRecordVOs(records);
    }

    @Override
    public Page<DietRecordVO> getRecordsPage(Long userId, int page, int size) {
        Page<DietRecord> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<DietRecord> wrapper = new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getUserId, userId)
                .orderByDesc(DietRecord::getCreateTime);
        Page<DietRecord> result = dietRecordMapper.selectPage(pageParam, wrapper);

        Map<Long, String> itemNameMap = batchLoadItemNames(result.getRecords());

        Page<DietRecordVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(r -> {
            DietRecordVO vo = foodConvert.toVO(r);
            vo.setItemName(itemNameMap.getOrDefault(r.getItemId(), "未知食物"));
            return vo;
        }).toList());
        return voPage;
    }

    @Override
    public Page<DietRecordVO> getRecordsByDate(Long userId, LocalDate date, int page, int size) {
        Page<DietRecord> pageParam = new Page<>(page, size);
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);
        LambdaQueryWrapper<DietRecord> wrapper = new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getUserId, userId)
                .between(DietRecord::getCreateTime, startOfDay, endOfDay)
                .orderByDesc(DietRecord::getCreateTime);
        Page<DietRecord> result = dietRecordMapper.selectPage(pageParam, wrapper);

        Map<Long, String> itemNameMap = batchLoadItemNames(result.getRecords());

        Page<DietRecordVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(r -> {
            DietRecordVO vo = foodConvert.toVO(r);
            vo.setItemName(itemNameMap.getOrDefault(r.getItemId(), "未知食物"));
            return vo;
        }).toList());
        return voPage;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DietRecordVO updateRecord(Long userId, Long recordId, DietRecordSubmitDTO dto) {
        DietRecord record = dietRecordMapper.selectById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(404, "饮食记录不存在");
        }
        record.setCheckinId(dto.getCheckinId());
        record.setMealType(dto.getMealType());
        record.setItemId(dto.getItemId());
        record.setWeightGrams(dto.getWeightGrams());
        record.setCaloriesConsumed(dto.getCaloriesConsumed());
        record.setProtein(dto.getProtein());
        record.setFat(dto.getFat());
        record.setCarbs(dto.getCarbs());
        record.setFoodName(dto.getFoodName());
        record.setCategory(dto.getCategory());
        record.setNote(dto.getNote());
        record.setRemark(dto.getRemark());
        dietRecordMapper.updateById(record);

        FoodItem item = foodItemMapper.selectById(dto.getItemId());
        DietRecordVO vo = foodConvert.toVO(record);
        if (item != null) {
            vo.setItemName(item.getName());
        }
        log.info("更新饮食记录 userId={} recordId={}", userId, recordId);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteRecord(Long userId, Long recordId) {
        DietRecord record = dietRecordMapper.selectById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BusinessException(404, "饮食记录不存在");
        }
        dietRecordMapper.deleteById(recordId);
        log.info("删除饮食记录 userId={} recordId={}", userId, recordId);
    }

    // --- Admin 食物项目管理 ---

    @Override
    public List<FoodItemVO> listAllItems() {
        LambdaQueryWrapper<FoodItem> wrapper = new LambdaQueryWrapper<FoodItem>()
                .orderByAsc(FoodItem::getSort);
        return foodItemMapper.selectList(wrapper).stream()
                .map(foodConvert::toVO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createFoodItem(FoodItemCreateDTO dto) {
        FoodItem item = new FoodItem();
        item.setName(dto.getName());
        item.setCategory(dto.getCategory());
        item.setCaloriePer100g(dto.getCaloriePer100g());
        item.setProteinPer100g(dto.getProteinPer100g());
        item.setCarbsPer100g(dto.getCarbsPer100g());
        item.setFatPer100g(dto.getFatPer100g());
        item.setImageUrl(dto.getImageUrl());
        item.setSort(dto.getSort() != null ? dto.getSort() : 0);
        item.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        foodItemMapper.insert(item);
        stringRedisTemplate.delete(FOOD_ITEM_LIST_CACHE);
        log.info("新增食物项目 id={} name={}", item.getId(), item.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateFoodItem(FoodItemUpdateDTO dto) {
        FoodItem item = foodItemMapper.selectById(dto.getId());
        if (item == null) {
            throw new BusinessException(404, "食物项目不存在");
        }
        item.setName(dto.getName());
        item.setCategory(dto.getCategory());
        item.setCaloriePer100g(dto.getCaloriePer100g());
        item.setProteinPer100g(dto.getProteinPer100g());
        item.setCarbsPer100g(dto.getCarbsPer100g());
        item.setFatPer100g(dto.getFatPer100g());
        item.setImageUrl(dto.getImageUrl());
        item.setSort(dto.getSort());
        item.setStatus(dto.getStatus());
        foodItemMapper.updateById(item);
        stringRedisTemplate.delete(FOOD_ITEM_LIST_CACHE);
        log.info("修改食物项目 id={} name={}", dto.getId(), dto.getName());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFoodItem(Long id) {
        FoodItem item = foodItemMapper.selectById(id);
        if (item == null) {
            throw new BusinessException(404, "食物项目不存在");
        }
        foodItemMapper.deleteById(id);
        stringRedisTemplate.delete(FOOD_ITEM_LIST_CACHE);
        log.info("删除食物项目 id={}", id);
    }

    // --- 私有方法 ---

    @Override
    public List<FoodItemVO> getFrequentItems(Long userId, int limit) {
        int queryLimit = Math.max(limit, 1);
        // 查询用户最近60天饮食记录中最常出现的食物
        LocalDateTime since = LocalDateTime.now().minusDays(60);
        LambdaQueryWrapper<DietRecord> wrapper = new LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getUserId, userId)
                .ge(DietRecord::getCreateTime, since)
                .isNotNull(DietRecord::getItemId);
        List<DietRecord> records = dietRecordMapper.selectList(wrapper);

        // 按食物ID分组计数，取频率最高的
        Map<Long, Long> freqMap = records.stream()
                .collect(Collectors.groupingBy(DietRecord::getItemId, Collectors.counting()));

        List<Long> topIds = freqMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
                .limit(queryLimit)
                .map(Map.Entry::getKey)
                .toList();

        if (topIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<FoodItem> items = foodItemMapper.selectBatchIds(topIds);
        // 按频率排序
        Map<Long, FoodItem> itemMap = items.stream()
                .collect(Collectors.toMap(FoodItem::getId, f -> f));
        return topIds.stream()
                .filter(itemMap::containsKey)
                .map(id -> foodConvert.toVO(itemMap.get(id)))
                .toList();
    }

    @Override
    public FoodItemVO parseFoodText(Long userId, String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        // 简单解析: "食物名 重量 单位 餐次" 如 "米饭 200 克 午餐"
        String[] parts = text.trim().split("\\s+");
        if (parts.length < 1) return null;

        String keyword = parts[0];
        // 在食物库中模糊匹配
        LambdaQueryWrapper<FoodItem> wrapper = new LambdaQueryWrapper<FoodItem>()
                .eq(FoodItem::getStatus, 1)
                .like(FoodItem::getName, keyword)
                .last("LIMIT 1");
        FoodItem item = foodItemMapper.selectOne(wrapper);
        return item != null ? foodConvert.toVO(item) : null;
    }

    private Map<Long, String> batchLoadItemNames(List<DietRecord> records) {
        if (records.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> itemIds = records.stream()
                .map(DietRecord::getItemId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());
        if (itemIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return foodItemMapper.selectBatchIds(itemIds).stream()
                .collect(Collectors.toMap(FoodItem::getId, FoodItem::getName, (a, b) -> a));
    }

    private List<DietRecordVO> mapToRecordVOs(List<DietRecord> records) {
        Map<Long, String> itemNameMap = batchLoadItemNames(records);
        return records.stream().map(r -> {
            DietRecordVO vo = foodConvert.toVO(r);
            vo.setItemName(itemNameMap.getOrDefault(r.getItemId(), "未知食物"));
            return vo;
        }).toList();
    }
}