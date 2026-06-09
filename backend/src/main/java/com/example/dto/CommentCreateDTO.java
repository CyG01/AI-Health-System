package com.example.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "评论DTO")
public class CommentCreateDTO {

    @Schema(description = "帖子ID")
    private Long postId;

    @Schema(description = "评论内容")
    private String content;

    @Schema(description = "回复的评论ID (0表示直接评论)")
    private Long replyTo;
}