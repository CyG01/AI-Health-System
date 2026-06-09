package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.dto.CommentCreateDTO;
import com.example.dto.PostCreateDTO;
import com.example.entity.CommunityComment;
import com.example.entity.CommunityLike;
import com.example.entity.CommunityPost;
import com.example.entity.SysUser;
import com.example.mapper.CommunityCommentMapper;
import com.example.mapper.CommunityLikeMapper;
import com.example.mapper.CommunityPostMapper;
import com.example.mapper.ExerciseRecordMapper;
import com.example.mapper.SysUserMapper;
import com.example.service.CommunityService;
import com.example.vo.CommunityCommentVO;
import com.example.vo.CommunityPostVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CommunityServiceImpl implements CommunityService {

    private final CommunityPostMapper postMapper;
    private final CommunityCommentMapper commentMapper;
    private final CommunityLikeMapper likeMapper;
    private final SysUserMapper userMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;

    public CommunityServiceImpl(CommunityPostMapper postMapper,
                                 CommunityCommentMapper commentMapper,
                                 CommunityLikeMapper likeMapper,
                                 SysUserMapper userMapper,
                                 ExerciseRecordMapper exerciseRecordMapper) {
        this.postMapper = postMapper;
        this.commentMapper = commentMapper;
        this.likeMapper = likeMapper;
        this.userMapper = userMapper;
        this.exerciseRecordMapper = exerciseRecordMapper;
    }

    @Override
    @Transactional
    public CommunityPostVO createPost(Long userId, PostCreateDTO dto) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        CommunityPost post = new CommunityPost();
        post.setUserId(userId);
        post.setUserNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
        post.setUserAvatar(user.getAvatar());
        post.setContent(dto.getContent());
        post.setImages(dto.getImages());
        post.setExerciseType(dto.getExerciseType());
        post.setExerciseDuration(dto.getExerciseDuration());
        post.setCaloriesBurned(dto.getCaloriesBurned());
        post.setLikeCount(0);
        post.setCommentCount(0);
        post.setStatus(1);
        postMapper.insert(post);

        log.info("发布帖子 userId={} postId={}", userId, post.getId());
        return toPostVO(post, userId);
    }

    @Override
    @Transactional
    public void deletePost(Long userId, Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) throw new BusinessException("帖子不存在");
        if (!post.getUserId().equals(userId)) throw new BusinessException("只能删除自己的帖子");

        // 删除相关评论和点赞
        commentMapper.delete(new LambdaQueryWrapper<CommunityComment>().eq(CommunityComment::getPostId, postId));
        likeMapper.delete(new LambdaQueryWrapper<CommunityLike>().eq(CommunityLike::getPostId, postId));
        postMapper.deleteById(postId);
        log.info("删除帖子 userId={} postId={}", userId, postId);
    }

    @Override
    public List<CommunityPostVO> getPostList(Long userId, int page, int size) {
        Page<CommunityPost> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<CommunityPost> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityPost::getStatus, 1)
                .orderByDesc(CommunityPost::getCreateTime);
        Page<CommunityPost> result = postMapper.selectPage(pageParam, wrapper);

        return result.getRecords().stream().map(p -> toPostVO(p, userId)).collect(Collectors.toList());
    }

    @Override
    public CommunityPostVO getPostDetail(Long userId, Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) throw new BusinessException("帖子不存在");
        return toPostVO(post, userId);
    }

    @Override
    @Transactional
    public Map<String, Object> toggleLike(Long userId, Long postId) {
        CommunityPost post = postMapper.selectById(postId);
        if (post == null) throw new BusinessException("帖子不存在");

        LambdaQueryWrapper<CommunityLike> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityLike::getPostId, postId).eq(CommunityLike::getUserId, userId);
        CommunityLike existing = likeMapper.selectOne(wrapper);

        if (existing != null) {
            // 取消点赞
            likeMapper.deleteById(existing.getId());
            post.setLikeCount(Math.max(0, post.getLikeCount() - 1));
            postMapper.updateById(post);
            return Map.of("isLiked", false, "likeCount", post.getLikeCount());
        } else {
            // 点赞
            CommunityLike like = new CommunityLike();
            like.setPostId(postId);
            like.setUserId(userId);
            likeMapper.insert(like);
            post.setLikeCount(post.getLikeCount() + 1);
            postMapper.updateById(post);
            return Map.of("isLiked", true, "likeCount", post.getLikeCount());
        }
    }

    @Override
    @Transactional
    public CommunityCommentVO createComment(Long userId, CommentCreateDTO dto) {
        SysUser user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        CommunityPost post = postMapper.selectById(dto.getPostId());
        if (post == null) throw new BusinessException("帖子不存在");

        CommunityComment comment = new CommunityComment();
        comment.setPostId(dto.getPostId());
        comment.setUserId(userId);
        comment.setUserNickname(user.getNickname() != null ? user.getNickname() : user.getUsername());
        comment.setUserAvatar(user.getAvatar());
        comment.setContent(dto.getContent());
        comment.setReplyTo(dto.getReplyTo() != null ? dto.getReplyTo() : 0L);
        commentMapper.insert(comment);

        // 更新帖子评论数
        post.setCommentCount(post.getCommentCount() + 1);
        postMapper.updateById(post);

        log.info("发表评论 userId={} postId={}", userId, dto.getPostId());
        return toCommentVO(comment);
    }

    @Override
    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        CommunityComment comment = commentMapper.selectById(commentId);
        if (comment == null) throw new BusinessException("评论不存在");
        if (!comment.getUserId().equals(userId)) throw new BusinessException("只能删除自己的评论");

        CommunityPost post = postMapper.selectById(comment.getPostId());
        if (post != null) {
            post.setCommentCount(Math.max(0, post.getCommentCount() - 1));
            postMapper.updateById(post);
        }
        commentMapper.deleteById(commentId);
    }

    @Override
    public List<CommunityCommentVO> getComments(Long postId) {
        LambdaQueryWrapper<CommunityComment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CommunityComment::getPostId, postId)
                .orderByAsc(CommunityComment::getCreateTime);
        return commentMapper.selectList(wrapper).stream().map(this::toCommentVO).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> getRanking(String type, int limit) {
        if ("calories".equals(type)) {
            // 从 exercise_record 聚合用户总消耗热量
            return getCaloriesRanking(limit);
        }
        // 连续打卡榜
        return getCheckinRanking(limit);
    }

    // ====================== 辅助方法 ======================

    private CommunityPostVO toPostVO(CommunityPost post, Long currentUserId) {
        CommunityPostVO vo = new CommunityPostVO();
        vo.setId(post.getId());
        vo.setUserId(post.getUserId());
        vo.setUserNickname(post.getUserNickname());
        vo.setUserAvatar(post.getUserAvatar());
        vo.setContent(post.getContent());
        vo.setImages(post.getImages());
        vo.setExerciseType(post.getExerciseType());
        vo.setExerciseDuration(post.getExerciseDuration());
        vo.setCaloriesBurned(post.getCaloriesBurned());
        vo.setLikeCount(post.getLikeCount());
        vo.setCommentCount(post.getCommentCount());
        vo.setCreateTime(post.getCreateTime());
        vo.setTimeAgo(formatTimeAgo(post.getCreateTime()));

        // 检查当前用户是否已点赞
        if (currentUserId != null) {
            LambdaQueryWrapper<CommunityLike> likeWrapper = new LambdaQueryWrapper<>();
            likeWrapper.eq(CommunityLike::getPostId, post.getId())
                    .eq(CommunityLike::getUserId, currentUserId);
            vo.setIsLiked(likeMapper.selectCount(likeWrapper) > 0);
        } else {
            vo.setIsLiked(false);
        }

        return vo;
    }

    private CommunityCommentVO toCommentVO(CommunityComment comment) {
        CommunityCommentVO vo = new CommunityCommentVO();
        vo.setId(comment.getId());
        vo.setPostId(comment.getPostId());
        vo.setUserId(comment.getUserId());
        vo.setUserNickname(comment.getUserNickname());
        vo.setUserAvatar(comment.getUserAvatar());
        vo.setContent(comment.getContent());
        vo.setReplyTo(comment.getReplyTo());
        vo.setCreateTime(comment.getCreateTime());
        vo.setTimeAgo(formatTimeAgo(comment.getCreateTime()));
        return vo;
    }

    private String formatTimeAgo(LocalDateTime time) {
        if (time == null) return "";
        long minutes = ChronoUnit.MINUTES.between(time, LocalDateTime.now());
        if (minutes < 1) return "刚刚";
        if (minutes < 60) return minutes + "分钟前";
        long hours = minutes / 60;
        if (hours < 24) return hours + "小时前";
        long days = hours / 24;
        if (days < 30) return days + "天前";
        return (days / 30) + "个月前";
    }

    private List<Map<String, Object>> getCaloriesRanking(int limit) {
        // 聚合用户总消耗热量
        List<Map<String, Object>> result = new ArrayList<>();
        List<com.example.entity.ExerciseRecord> records = exerciseRecordMapper.selectList(null);

        Map<Long, Integer> userCalories = new HashMap<>();
        for (com.example.entity.ExerciseRecord r : records) {
            userCalories.merge(r.getUserId(), r.getCaloriesBurned(), Integer::sum);
        }

        // 排序取top
        userCalories.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(limit)
                .forEach(e -> {
                    SysUser user = userMapper.selectById(e.getKey());
                    Map<String, Object> item = new HashMap<>();
                    item.put("userId", e.getKey());
                    item.put("nickname", user != null ? (user.getNickname() != null ? user.getNickname() : user.getUsername()) : "未知");
                    item.put("avatar", user != null ? user.getAvatar() : null);
                    item.put("calories", e.getValue());
                    result.add(item);
                });

        return result;
    }

    private List<Map<String, Object>> getCheckinRanking(int limit) {
        // 简化实现：返回最近活跃用户
        List<SysUser> users = userMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1).last("LIMIT " + limit));
        return users.stream().map(u -> {
            Map<String, Object> item = new HashMap<>();
            item.put("userId", u.getId());
            item.put("nickname", u.getNickname() != null ? u.getNickname() : u.getUsername());
            item.put("avatar", u.getAvatar());
            item.put("streakDays", 0); // 后续可扩展计算连续打卡天数
            return item;
        }).collect(Collectors.toList());
    }
}