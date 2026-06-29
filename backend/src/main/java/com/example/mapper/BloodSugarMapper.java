package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.BloodSugar;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface BloodSugarMapper extends BaseMapper<BloodSugar> {

    /**
     * 查询用户某天的血糖平均值。
     */
    @Select("SELECT AVG(glucose_value) FROM blood_sugar " +
            "WHERE user_id = #{userId} AND record_date = #{date} AND is_deleted = 0")
    BigDecimal getDailyAvg(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 抽样获取 N 个有血糖记录的用户 ID。
     */
    @Select("SELECT DISTINCT user_id FROM blood_sugar " +
            "WHERE is_deleted = 0 ORDER BY RAND() LIMIT #{limit}")
    List<Long> sampleUserIds(@Param("limit") int limit);

    /**
     * 查询用户某天的体重平均值（从 health_record 表联合查询）。
     */
    @Select("SELECT AVG(weight) FROM health_record " +
            "WHERE user_id = #{userId} AND DATE(create_time) = #{date} AND is_deleted = 0")
    BigDecimal getDailyAvgWeight(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 查询用户某天的运动总卡路里。
     */
    @Select("SELECT COALESCE(SUM(calories_burned), 0) FROM exercise_record " +
            "WHERE user_id = #{userId} AND DATE(create_time) = #{date} AND is_deleted = 0")
    BigDecimal getDailyExerciseCalories(@Param("userId") Long userId, @Param("date") LocalDate date);

    /**
     * 查询用户某天的饮食总卡路里。
     */
    @Select("SELECT COALESCE(SUM(calories_consumed), 0) FROM diet_record " +
            "WHERE user_id = #{userId} AND DATE(create_time) = #{date} AND is_deleted = 0")
    BigDecimal getDailyDietCalories(@Param("userId") Long userId, @Param("date") LocalDate date);
}