package com.example.convert;

import com.example.dto.CheckinSubmitDTO;
import com.example.dto.CheckinSupplementDTO;
import com.example.entity.DailyCheckin;
import com.example.vo.CheckinVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface DailyCheckinConvert {

    CheckinVO toCheckinVO(DailyCheckin checkin);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "checkDate", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    DailyCheckin toEntity(CheckinSubmitDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    @Mapping(target = "version", ignore = true)
    DailyCheckin toEntity(CheckinSupplementDTO dto);
}