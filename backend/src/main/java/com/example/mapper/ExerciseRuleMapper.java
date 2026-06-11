package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.ExerciseRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface ExerciseRuleMapper extends BaseMapper<ExerciseRule> {

    /**
     * 根据健康目标和 BMI 匹配推荐运动规则
     */
    @Select("SELECT * FROM exercise_rules WHERE is_active = 1 " +
            "AND goal = #{goal} " +
            "AND bmi_min <= #{bmi} AND bmi_max >= #{bmi} " +
            "ORDER BY priority ASC, id ASC")
    List<ExerciseRule> matchByGoalAndBmi(@Param("goal") String goal, @Param("bmi") double bmi);
}