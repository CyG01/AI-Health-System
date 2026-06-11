package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.LlmTestCase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LlmTestCaseMapper extends BaseMapper<LlmTestCase> {

    @Select("SELECT * FROM llm_test_case WHERE is_active = 1 AND safety_level = #{safetyLevel}")
    List<LlmTestCase> findBySafetyLevel(String safetyLevel);

    @Select("SELECT * FROM llm_test_case WHERE is_active = 1 AND category = #{category}")
    List<LlmTestCase> findByCategory(String category);

    @Select("SELECT * FROM llm_test_case WHERE is_active = 1 ORDER BY RAND() LIMIT #{limit}")
    List<LlmTestCase> randomSample(int limit);
}