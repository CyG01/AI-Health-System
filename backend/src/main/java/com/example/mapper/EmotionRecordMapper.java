package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.EmotionRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EmotionRecordMapper extends BaseMapper<EmotionRecord> {

    /**
     * 查询用户最近N条情绪记录
     */
    @Select("SELECT * FROM emotion_record WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<EmotionRecord> findRecentByUser(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查询用户近N天连续负面情绪天数
     */
    @Select("SELECT COUNT(DISTINCT DATE(created_at)) FROM emotion_record " +
            "WHERE user_id = #{userId} AND emotion_type IN ('TIRED', 'FRUSTRATED', 'ANXIOUS', 'PAIN') " +
            "AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY)")
    int countNegativeDays(@Param("userId") Long userId, @Param("days") int days);
}