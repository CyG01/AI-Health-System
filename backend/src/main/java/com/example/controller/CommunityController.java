package com.example.controller;

import com.example.common.Result;
import com.example.dto.CommentCreateDTO;
import com.example.dto.PostCreateDTO;
import com.example.service.CommunityService;
import com.example.vo.CommunityCommentVO;
import com.example.vo.CommunityPostVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "社区社交")
@RestController
@RequestMapping("/api/community")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    @Operation(summary = "发布帖子")
    @PostMapping("/post")
    public Result<CommunityPostVO> createPost(@Validated @RequestBody PostCreateDTO dto,
                                              @RequestAttribute("userId") Long userId) {
        return Result.success(communityService.createPost(userId, dto));
    }

    @Operation(summary = "删除帖子")
    @DeleteMapping("/post/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId,
                                    @RequestAttribute("userId") Long userId) {
        communityService.deletePost(userId, postId);
        return Result.success();
    }

    @Operation(summary = "帖子列表")
    @GetMapping("/posts")
    public Result<List<CommunityPostVO>> postList(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "10") int size,
                                                   @RequestAttribute("userId") Long userId) {
        return Result.success(communityService.getPostList(userId, page, size));
    }

    @Operation(summary = "帖子详情")
    @GetMapping("/post/{postId}")
    public Result<CommunityPostVO> postDetail(@PathVariable Long postId,
                                               @RequestAttribute("userId") Long userId) {
        return Result.success(communityService.getPostDetail(userId, postId));
    }

    @Operation(summary = "点赞/取消点赞")
    @PostMapping("/like/{postId}")
    public Result<Map<String, Object>> toggleLike(@PathVariable Long postId,
                                                   @RequestAttribute("userId") Long userId) {
        return Result.success(communityService.toggleLike(userId, postId));
    }

    @Operation(summary = "发表评论")
    @PostMapping("/comment")
    public Result<CommunityCommentVO> createComment(@Validated @RequestBody CommentCreateDTO dto,
                                                     @RequestAttribute("userId") Long userId) {
        return Result.success(communityService.createComment(userId, dto));
    }

    @Operation(summary = "删除评论")
    @DeleteMapping("/comment/{commentId}")
    public Result<Void> deleteComment(@PathVariable Long commentId,
                                       @RequestAttribute("userId") Long userId) {
        communityService.deleteComment(userId, commentId);
        return Result.success();
    }

    @Operation(summary = "帖子评论列表")
    @GetMapping("/comments/{postId}")
    public Result<List<CommunityCommentVO>> comments(@PathVariable Long postId) {
        return Result.success(communityService.getComments(postId));
    }

    @Operation(summary = "排行榜")
    @GetMapping("/ranking")
    public Result<List<Map<String, Object>>> ranking(@RequestParam(defaultValue = "calories") String type,
                                                      @RequestParam(defaultValue = "20") int limit) {
        return Result.success(communityService.getRanking(type, limit));
    }
}