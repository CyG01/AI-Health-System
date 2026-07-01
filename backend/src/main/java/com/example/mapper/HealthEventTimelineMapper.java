package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.HealthEventTimeline;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 健康事件时间线Mapper
 */
@Mapper
public interface HealthEventTimelineMapper extends BaseMapper<HealthEventTimeline> {

    /**
     * 查询用户指定时间范围内的健康事件
     */
    @Select("SELECT * FROM health_event_timeline " +
            "WHERE user_id = #{userId} " +
            "AND event_date BETWEEN #{startDate} AND #{endDate} " +
            "ORDER BY event_date DESC")
    List<HealthEventTimeline> selectByUserIdAndDateRange(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * 查询用户最近N条健康事件
     */
    @Select("SELECT * FROM health_event_timeline " +
            "WHERE user_id = #{userId} " +
            "ORDER BY event_date DESC " +
            "LIMIT #{limit}")
    List<HealthEventTimeline> selectRecentByUserId(
            @Param("userId") Long userId,
            @Param("limit") int limit);

    /**
     * 按类型查询用户健康事件
     */
    @Select("SELECT * FROM health_event_timeline " +
            "WHERE user_id = #{userId} AND event_type = #{eventType} " +
            "ORDER BY event_date DESC")
    List<HealthEventTimeline> selectByUserIdAndType(
            @Param("userId") Long userId,
            @Param("eventType") String eventType);
}
