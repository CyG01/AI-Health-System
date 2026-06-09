package com.example.convert;

import com.example.entity.AiPlan;
import com.example.entity.AiPlanDetail;
import com.example.vo.AiPlanDetailVO;
import com.example.vo.AiPlanVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AiPlanConvert {

    AiPlanVO toAiPlanVO(AiPlan plan);

    @Mapping(target = "details", ignore = true)
    AiPlanDetailVO toAiPlanDetailVO(AiPlan plan);

    AiPlanDetailVO.DetailItem toDetailVO(AiPlanDetail entity);
}
