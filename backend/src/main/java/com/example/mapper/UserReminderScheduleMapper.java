package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.UserReminderSchedule;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户提醒配置Mapper
 */
@Mapper
public interface UserReminderScheduleMapper extends BaseMapper<UserReminderSchedule> {

    /**
     * 查询用户所有提醒配置
     */
    @Select("SELECT * FROM user_reminder_schedule WHERE user_id = #{userId} ORDER BY reminder_time")
    List<UserReminderSchedule> selectByUserId(@Param("userId") Long userId);

    /**
     * 查询用户指定类型的提醒配置
     */
    @Select("SELECT * FROM user_reminder_schedule " +
            "WHERE user_id = #{userId} AND reminder_type = #{reminderType} " +
            "AND is_enabled = 1 ORDER BY reminder_time")
    List<UserReminderSchedule> selectByUserIdAndType(
            @Param("userId") Long userId,
            @Param("reminderType") String reminderType);
}
