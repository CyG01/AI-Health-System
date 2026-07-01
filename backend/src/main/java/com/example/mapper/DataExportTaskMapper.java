package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.DataExportTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 数据导出任务Mapper
 */
@Mapper
public interface DataExportTaskMapper extends BaseMapper<DataExportTask> {

    /**
     * 查询用户的导出任务
     */
    @Select("SELECT * FROM data_export_task " +
            "WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<DataExportTask> selectByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}
