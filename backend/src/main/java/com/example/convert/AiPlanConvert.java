package com.example.convert;

import com.example.entity.AiPlan;
import com.example.vo.AiPlanDetailVO;
import com.example.vo.AiPlanVO;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AiPlanConvert {

    AiPlanVO toAiPlanVO(AiPlan plan);

    AiPlanDetailVO toAiPlanDetailVO(AiPlan plan);
}
