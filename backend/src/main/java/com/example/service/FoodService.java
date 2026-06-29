package com.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dto.DietRecordSubmitDTO;
import com.example.dto.FoodItemCreateDTO;
import com.example.dto.FoodItemUpdateDTO;
import com.example.vo.DietRecordVO;
import com.example.vo.FoodItemVO;

import java.time.LocalDate;
import java.util.List;

public interface FoodService {

    List<FoodItemVO> listActiveItems();

    List<FoodItemVO> listItemsByCategory(String category);

    DietRecordVO submitRecord(Long userId, DietRecordSubmitDTO dto);

    List<DietRecordVO> getRecordsByCheckinId(Long userId, Long checkinId);

    List<DietRecordVO> getRecordsByUserId(Long userId, Integer limit);

    // 用户饮食记录分页查询
    Page<DietRecordVO> getRecordsPage(Long userId, int page, int size);

    Page<DietRecordVO> getRecordsByDate(Long userId, LocalDate date, int page, int size);

    DietRecordVO updateRecord(Long userId, Long recordId, DietRecordSubmitDTO dto);

    void deleteRecord(Long userId, Long recordId);

    // 管理员食物字典管理
    List<FoodItemVO> listAllItems();

    void createFoodItem(FoodItemCreateDTO dto);

    void updateFoodItem(FoodItemUpdateDTO dto);

    void deleteFoodItem(Long id);

    /** 查询用户常用食物（根据历史饮食记录频率排序） */
    List<FoodItemVO> getFrequentItems(Long userId, int limit);

    /** 文字快捷录入：解析自然语言文本并返回匹配的食物项 */
    FoodItemVO parseFoodText(Long userId, String text);
}