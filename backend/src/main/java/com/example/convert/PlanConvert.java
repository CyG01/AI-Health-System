package com.example.convert;

import com.example.dto.PlanFeedbackDTO;
import com.example.entity.AiPlanDetail;
import com.example.entity.AiPlanFeedback;
import com.example.vo.AiPlanDetailVO;
import com.example.vo.PlanFeedbackVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PlanConvert {

    AiPlanDetailVO toDetailVO(AiPlanDetail entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "adjustmentSuggestion", ignore = true)
    @Mapping(target = "isAdjusted", ignore = true)
    @Mapping(target = "newPlanId", ignore = true)
    @Mapping(target = "createTime", ignore = true)
    AiPlanFeedback toFeedbackEntity(PlanFeedbackDTO dto);

    PlanFeedbackVO toFeedbackVO(AiPlanFeedback entity);
}