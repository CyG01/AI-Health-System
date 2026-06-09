package com.example.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "评论VO")
public class CommunityCommentVO {

    @Schema(description = "评论ID")
    private Long id;

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "用户头像")
    private String userAvatar;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "回复的评论ID")
    private Long replyTo;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "相对时间")
    private String timeAgo;
}