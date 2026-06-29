package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.ExerciseRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Mapper
public interface ExerciseRecordMapper extends BaseMapper<ExerciseRecord> {

    @Select("SELECT user_id AS userId, SUM(calories_burned) AS totalCalories " +
            "FROM exercise_record GROUP BY user_id ORDER BY totalCalories DESC LIMIT #{limit}")
    List<Map<String, Object>> selectCaloriesRanking(int limit);

    @Select("SELECT COALESCE(SUM(calories_burned), 0) FROM exercise_record " +
            "WHERE user_id = #{userId} AND create_time BETWEEN #{start} AND #{end}")
    int sumCaloriesBurned(@Param("userId") Long userId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end);
}