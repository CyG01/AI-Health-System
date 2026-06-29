package com.example.convert;

import com.example.dto.ExerciseRecordSubmitDTO;
import com.example.entity.ExerciseItem;
import com.example.entity.ExerciseRecord;
import com.example.vo.ExerciseItemVO;
import com.example.vo.ExerciseRecordVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ExerciseConvert {

    ExerciseItemVO toVO(ExerciseItem entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    ExerciseRecord toEntity(ExerciseRecordSubmitDTO dto);

    @Mapping(target = "itemName", ignore = true)
    ExerciseRecordVO toVO(ExerciseRecord entity);
}