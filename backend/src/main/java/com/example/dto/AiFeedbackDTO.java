package com.example.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 用户提交AI反馈的请求DTO。
 */
@Data
public class AiFeedbackDTO {

    /** 关联的AI响应ID */
    @NotBlank(message = "AI响应ID不能为空")
    private String aiResponseId;

    /** 评价：useful/useless/incorrect */
    @NotBlank(message = "评价不能为空")
    private String rating;

    /** 用户详细反馈（可选） */
    private String comment;
}