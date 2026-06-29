package com.example.convert;

import com.example.dto.HealthCreateDTO;
import com.example.entity.HealthRecord;
import com.example.vo.HealthAssessmentVO;
import com.example.vo.HealthHistoryVO;
import com.example.vo.HealthRecordVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface HealthConvert {

    HealthRecordVO toHealthRecordVO(HealthRecord record);

    @Mapping(target = "bmiLevel", ignore = true)
    @Mapping(target = "risks", ignore = true)
    HealthAssessmentVO toHealthAssessmentVO(HealthRecord record);

    HealthHistoryVO toHealthHistoryVO(HealthRecord record);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "bmi", ignore = true)
    @Mapping(target = "bmr", ignore = true)
    @Mapping(target = "dailyCalorie", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    HealthRecord toEntity(HealthCreateDTO dto);
}
