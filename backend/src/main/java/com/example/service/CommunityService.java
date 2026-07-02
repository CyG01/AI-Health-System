package com.example.service;

import com.example.dto.CommentCreateDTO;
import com.example.dto.PostCreateDTO;
import com.example.vo.CommunityCommentVO;
import com.example.vo.CommunityPostVO;

import java.util.List;
import java.util.Map;

public interface CommunityService {

    /**
     * 发布帖子
     */
    CommunityPostVO createPost(Long userId, PostCreateDTO dto);

    /**
     * 删除帖子
     */
    void deletePost(Long userId, Long postId);

    /**
     * 获取帖子列表 (首页动态) - 传统分页
     */
    List<CommunityPostVO> getPostList(Long userId, int page, int size);

    /**
     * 获取帖子列表 - 游标分页（Feed流推荐）
     * @param userId 当前用户ID
     * @param lastId 上一页最后一条记录的ID，为null时查询第一页
     * @param size   每页大小
     * @return 包含 list（帖子列表）和 hasMore（是否还有更多）的Map
     */
    Map<String, Object> getPostListByCursor(Long userId, Long lastId, int size);

    /**
     * 获取帖子详情
     */
    CommunityPostVO getPostDetail(Long userId, Long postId);

    /**
     * 点赞/取消点赞
     */
    Map<String, Object> toggleLike(Long userId, Long postId);

    /**
     * 发表评论
     */
    CommunityCommentVO createComment(Long userId, CommentCreateDTO dto);

    /**
     * 删除评论
     */
    void deleteComment(Long userId, Long commentId);

    /**
     * 获取帖子评论列表
     */
    List<CommunityCommentVO> getComments(Long postId);

    /**
     * 获取排行榜 (热量消耗榜)
     */
    List<Map<String, Object>> getRanking(String type, int limit);
}