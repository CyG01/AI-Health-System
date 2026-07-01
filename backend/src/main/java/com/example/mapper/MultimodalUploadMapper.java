package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.MultimodalUpload;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 多模态上传记录Mapper
 */
@Mapper
public interface MultimodalUploadMapper extends BaseMapper<MultimodalUpload> {

    /**
     * 查询用户最近的多模态上传记录
     */
    @Select("SELECT * FROM multimodal_upload " +
            "WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<MultimodalUpload> selectRecentByUserId(
            @Param("userId") Long userId,
            @Param("limit") int limit);

    /**
     * 查询待处理的多模态任务
     */
    @Select("SELECT * FROM multimodal_upload " +
            "WHERE processing_status = 'PENDING' " +
            "ORDER BY created_at ASC " +
            "LIMIT #{limit}")
    List<MultimodalUpload> selectPendingTasks(@Param("limit") int limit);
}
