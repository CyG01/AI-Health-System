package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.ComplianceRule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ComplianceRuleMapper extends BaseMapper<ComplianceRule> {

    /**
     * 检查文本是否命中合规规则。
     */
    @Select("SELECT * FROM compliance_rule WHERE is_active = 1 AND #{text} REGEXP match_pattern")
    List<ComplianceRule> matchByText(@Param("text") String text);
}