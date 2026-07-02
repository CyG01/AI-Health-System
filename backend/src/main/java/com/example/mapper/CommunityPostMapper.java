package com.example.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.entity.CommunityPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CommunityPostMapper extends BaseMapper<CommunityPost> {

    @Update("UPDATE community_post SET like_count = like_count + 1 WHERE id = #{postId}")
    int incrementLikeCount(@Param("postId") Long postId);

    @Update("UPDATE community_post SET like_count = GREATEST(like_count - 1, 0) WHERE id = #{postId}")
    int decrementLikeCount(@Param("postId") Long postId);

    /**
     * 游标分页查询帖子列表
     * @param lastId 上一页最后一条记录的ID，为null时查询第一页
     * @param size   每页大小
     */
    @Select("<script>" +
            "SELECT * FROM community_post " +
            "WHERE status = 1 " +
            "<if test='lastId != null'> AND id &lt; #{lastId} </if>" +
            "ORDER BY id DESC " +
            "LIMIT #{size}" +
            "</script>")
    List<CommunityPost> selectByCursor(@Param("lastId") Long lastId, @Param("size") int size);
}