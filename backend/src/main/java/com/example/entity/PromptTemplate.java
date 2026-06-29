package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Prompt 模板 —— 支持热更新和 A/B 测试
 */
@Data
@TableName("prompt_template")
public class PromptTemplate {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 模板标识（唯一） */
    private String templateKey;

    /** 模板名称 */
    private String templateName;

    /** 模板内容（支持 %s/%d 等占位符） */
    private String templateContent;

    /** 版本号 */
    private Integer version;

    /** 是否启用 */
    private Boolean isActive;

    /** A/B 测试分组 */
    private String abGroup;

    /** 描述 */
    private String description;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;
}