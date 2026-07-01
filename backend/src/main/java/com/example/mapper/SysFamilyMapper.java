package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.SysFamily;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 家庭组Mapper
 */
@Mapper
public interface SysFamilyMapper extends BaseMapper<SysFamily> {

    /**
     * 根据用户ID查询所属的家庭列表
     */
    @Select("SELECT f.* FROM sys_family f " +
            "INNER JOIN sys_family_user fu ON f.id = fu.family_id " +
            "WHERE fu.user_id = #{userId} AND fu.status = 1 AND f.status = 1")
    List<SysFamily> selectFamiliesByUserId(@Param("userId") Long userId);

    /**
     * 查询用户是否拥有某个家庭的有效订阅
     */
    @Select("SELECT COUNT(*) FROM sys_family f " +
            "INNER JOIN sys_family_user fu ON f.id = fu.family_id " +
            "INNER JOIN subscription s ON f.subscription_id = s.id " +
            "WHERE fu.user_id = #{userId} " +
            "AND fu.status = 1 " +
            "AND f.status = 1 " +
            "AND s.status = 'active' " +
            "AND s.tier IN ('family', 'enterprise') " +
            "AND s.expire_time > NOW()")
    int countActiveFamilySubscription(@Param("userId") Long userId);
}
