package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.SysNotification;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysNotificationMapper extends BaseMapper<SysNotification> {

    @Insert("<script>" +
            "INSERT INTO sys_notification (user_id, title, content, type, is_read) VALUES " +
            "<foreach collection='list' item='item' separator=','>" +
            "(#{item.userId}, #{item.title}, #{item.content}, #{item.type}, #{item.isRead})" +
            "</foreach>" +
            "</script>")
    int batchInsert(@Param("list") List<SysNotification> list);
}
