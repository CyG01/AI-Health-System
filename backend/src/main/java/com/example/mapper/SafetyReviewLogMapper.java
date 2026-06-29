package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.SafetyReviewLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 安全审查审计日志 Mapper。
 */
@Mapper
public interface SafetyReviewLogMapper extends BaseMapper<SafetyReviewLog> {
}