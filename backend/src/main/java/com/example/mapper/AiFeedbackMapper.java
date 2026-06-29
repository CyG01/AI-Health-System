package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.AiFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * AI反馈数据访问层。
 */
@Mapper
public interface AiFeedbackMapper extends BaseMapper<AiFeedback> {

    /**
     * 查询待审核的反馈列表。
     */
    @Select("SELECT * FROM ai_feedback WHERE manual_reviewed = 0 ORDER BY created_at DESC")
    List<AiFeedback> selectPendingReview();
}