package com.example.convert;

import com.example.dto.DietRecordSubmitDTO;
import com.example.entity.DietRecord;
import com.example.entity.FoodItem;
import com.example.vo.DietRecordVO;
import com.example.vo.FoodItemVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface FoodConvert {

    FoodItemVO toVO(FoodItem entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    DietRecord toEntity(DietRecordSubmitDTO dto);

    @Mapping(target = "itemName", ignore = true)
    DietRecordVO toVO(DietRecord entity);
}