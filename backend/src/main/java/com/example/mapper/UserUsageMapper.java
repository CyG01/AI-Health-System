package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.UserUsage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UserUsageMapper extends BaseMapper<UserUsage> {

    @Select("SELECT * FROM user_usage WHERE user_id = #{userId} AND usage_date = #{date}")
    UserUsage findByUserIdAndDate(Long userId, LocalDate date);

    /** 汇总用户指定日期区间内的总Token消耗（inputTokens + outputTokens） */
    @Select("SELECT COALESCE(SUM(COALESCE(input_tokens, 0) + COALESCE(output_tokens, 0)), 0) " +
            "FROM user_usage WHERE user_id = #{userId} AND usage_date BETWEEN #{start} AND #{end}")
    long sumTokensByUserIdAndDateRange(@Param("userId") Long userId,
                                        @Param("start") LocalDate start,
                                        @Param("end") LocalDate end);

    /** 汇总用户指定日期区间内的总费用 */
    @Select("SELECT COALESCE(SUM(COALESCE(daily_cost, 0)), 0) " +
            "FROM user_usage WHERE user_id = #{userId} AND usage_date BETWEEN #{start} AND #{end}")
    BigDecimal sumCostByUserIdAndDateRange(@Param("userId") Long userId,
                                            @Param("start") LocalDate start,
                                            @Param("end") LocalDate end);

    /** 查询用户指定日期区间内的每日用量记录 */
    @Select("SELECT * FROM user_usage WHERE user_id = #{userId} AND usage_date BETWEEN #{start} AND #{end} ORDER BY usage_date DESC")
    List<UserUsage> listByUserIdAndDateRange(@Param("userId") Long userId,
                                             @Param("start") LocalDate start,
                                             @Param("end") LocalDate end);
}