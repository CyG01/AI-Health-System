package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.DietRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;

@Mapper
public interface DietRecordMapper extends BaseMapper<DietRecord> {

    @Select("SELECT COALESCE(SUM(calories_consumed), 0) FROM diet_record " +
            "WHERE user_id = #{userId} AND create_time BETWEEN #{start} AND #{end}")
    int sumCaloriesConsumed(@Param("userId") Long userId,
                            @Param("start") LocalDateTime start,
                            @Param("end") LocalDateTime end);
}