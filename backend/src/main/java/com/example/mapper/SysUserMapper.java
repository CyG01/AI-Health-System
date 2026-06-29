package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /**
     * 抽样获取 N 个活跃用户 ID（用于数据一致性校验）。
     */
    @Select("SELECT id FROM sys_user WHERE status = 1 ORDER BY RAND() LIMIT #{limit}")
    List<Long> sampleActiveUserIds(@Param("limit") int limit);
}
