package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.SysFamilyUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 家庭成员关系Mapper
 */
@Mapper
public interface SysFamilyUserMapper extends BaseMapper<SysFamilyUser> {

    /**
     * 查询用户的主家庭ID
     */
    @Select("SELECT family_id FROM sys_family_user " +
            "WHERE user_id = #{userId} AND status = 1 " +
            "ORDER BY member_role = 'OWNER' DESC, join_time ASC " +
            "LIMIT 1")
    Long selectPrimaryFamilyId(@Param("userId") Long userId);

    /**
     * 查询家庭所有成员
     */
    @Select("SELECT * FROM sys_family_user " +
            "WHERE family_id = #{familyId} AND status = 1 " +
            "ORDER BY FIELD(member_role, 'OWNER', 'ADMIN', 'MEMBER', 'CHILD', 'ELDER'), join_time")
    List<SysFamilyUser> selectMembersByFamilyId(@Param("familyId") Long familyId);

    /**
     * 查询家庭成员数量
     */
    @Select("SELECT COUNT(*) FROM sys_family_user WHERE family_id = #{familyId} AND status = 1")
    int countMembersByFamilyId(@Param("familyId") Long familyId);

    /**
     * 查询用户在家庭中的角色
     */
    @Select("SELECT member_role FROM sys_family_user " +
            "WHERE family_id = #{familyId} AND user_id = #{userId} AND status = 1")
    String selectMemberRole(@Param("familyId") Long familyId, @Param("userId") Long userId);
}
