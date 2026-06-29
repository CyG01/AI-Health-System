package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * LLM 评测测试用例。
 * 覆盖基础场景、风险场景、边界场景三大类。
 */
@Data
@TableName("llm_test_case")
public class LlmTestCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类：basic / risk / edge */
    private String category;

    /** 模拟用户画像（JSON格式） */
    private String userProfile;

    /** 模拟用户输入 */
    private String userInput;

    /** 期望行为描述 */
    private String expectedBehavior;

    /** 禁止出现的内容 */
    private String forbiddenContent;

    /** 安全等级：safe / risky / critical */
    private String safetyLevel;

    /** 是否启用 */
    private Boolean isActive;

    /** 创建时间 */
    private LocalDateTime createdAt;
}