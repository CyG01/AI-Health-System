package com.example.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "帖子VO")
public class CommunityPostVO {

    @Schema(description = "帖子ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "用户昵称")
    private String userNickname;

    @Schema(description = "用户头像")
    private String userAvatar;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "图片列表")
    private String images;

    @Schema(description = "运动类型")
    private String exerciseType;

    @Schema(description = "运动时长")
    private Integer exerciseDuration;

    @Schema(description = "消耗热量")
    private Integer caloriesBurned;

    @Schema(description = "点赞数")
    private Integer likeCount;

    @Schema(description = "评论数")
    private Integer commentCount;

    @Schema(description = "当前用户是否已点赞")
    private Boolean isLiked;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "相对时间")
    private String timeAgo;
}