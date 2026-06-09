package com.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.dto.ExerciseItemCreateDTO;
import com.example.dto.ExerciseItemUpdateDTO;
import com.example.dto.ExerciseRecordSubmitDTO;
import com.example.vo.ExerciseItemVO;
import com.example.vo.ExerciseRecordVO;

import java.time.LocalDate;
import java.util.List;

public interface ExerciseService {

    List<ExerciseItemVO> listActiveItems();

    List<ExerciseItemVO> listItemsByType(String type);

    ExerciseRecordVO submitRecord(Long userId, ExerciseRecordSubmitDTO dto);

    List<ExerciseRecordVO> getRecordsByCheckinId(Long checkinId);

    List<ExerciseRecordVO> getRecordsByUserId(Long userId, Integer limit);

    // 用户运动记录分页查询
    Page<ExerciseRecordVO> getRecordsPage(Long userId, int page, int size);

    Page<ExerciseRecordVO> getRecordsByDate(Long userId, LocalDate date, int page, int size);

    // 管理员运动字典管理
    List<ExerciseItemVO> listAllItems();

    void createExerciseItem(ExerciseItemCreateDTO dto);

    void updateExerciseItem(ExerciseItemUpdateDTO dto);

    void deleteExerciseItem(Long id);
}