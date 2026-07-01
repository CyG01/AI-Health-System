package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.PrivacyAuditLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 隐私审计日志Mapper
 */
@Mapper
public interface PrivacyAuditLogMapper extends BaseMapper<PrivacyAuditLog> {

    /**
     * 查询用户隐私操作日志
     */
    @Select("SELECT * FROM privacy_audit_log " +
            "WHERE user_id = #{userId} " +
            "ORDER BY created_at DESC " +
            "LIMIT #{limit}")
    List<PrivacyAuditLog> selectByUserId(@Param("userId") Long userId, @Param("limit") int limit);
}
