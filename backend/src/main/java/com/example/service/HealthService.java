package com.example.service;

import com.example.dto.HealthCreateDTO;
import com.example.dto.HealthUpdateDTO;
import com.example.vo.HealthAssessmentVO;
import com.example.vo.HealthHistoryVO;
import com.example.vo.HealthProgressVO;
import com.example.vo.HealthRecordVO;

import java.util.List;

public interface HealthService {

    HealthRecordVO createHealthRecord(Long userId, HealthCreateDTO dto);

    HealthRecordVO updateHealthRecord(Long userId, HealthUpdateDTO dto);

    HealthRecordVO getLatestHealthRecord(Long userId);

    List<HealthHistoryVO> getHealthHistory(Long userId, Integer page, Integer size);

    HealthAssessmentVO getHealthAssessment(Long userId);

    HealthProgressVO getHealthProgress(Long userId);
}
