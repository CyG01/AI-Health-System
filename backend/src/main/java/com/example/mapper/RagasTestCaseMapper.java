package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.RagasTestCase;
import org.apache.ibatis.annotations.Mapper;

/**
 * RAGAS 测试用例数据访问层。
 */
@Mapper
public interface RagasTestCaseMapper extends BaseMapper<RagasTestCase> {
}