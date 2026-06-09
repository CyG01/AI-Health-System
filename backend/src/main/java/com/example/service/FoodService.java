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

    List<DietRecordVO> getRecordsByCheckinId(Long checkinId);

    List<DietRecordVO> getRecordsByUserId(Long userId, Integer limit);

    // 用户饮食记录分页查询
    Page<DietRecordVO> getRecordsPage(Long userId, int page, int size);

    Page<DietRecordVO> getRecordsByDate(Long userId, LocalDate date, int page, int size);

    // 管理员食物字典管理
    List<FoodItemVO> listAllItems();

    void createFoodItem(FoodItemCreateDTO dto);

    void updateFoodItem(FoodItemUpdateDTO dto);

    void deleteFoodItem(Long id);
}