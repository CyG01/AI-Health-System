package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.SafetyRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SafetyRuleMapper extends BaseMapper<SafetyRule> {

    /**
     * 根据用户健康状况匹配安全规则（模糊匹配）。
     */
    @Select("SELECT * FROM safety_rule WHERE is_active = 1 AND INSTR(CONCAT(',', #{conditions}, ','), CONCAT(',', user_condition, ',')) > 0")
    List<SafetyRule> matchByConditions(@Param("conditions") String conditions);
}