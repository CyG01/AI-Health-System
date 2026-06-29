package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户每日用量统计。
 * 按 Token 和调用次数两个维度计费。
 */
@Data
@TableName("user_usage")
public class UserUsage {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 使用日期 */
    private LocalDate usageDate;

    /** 输入 token 数 */
    private Integer inputTokens;

    /** 输出 token 数 */
    private Integer outputTokens;

    /** API 调用次数 */
    private Integer apiCallCount;

    /** 计划生成次数 */
    private Integer planGenCount;

    /** 食物识别次数 */
    private Integer foodRecogCount;

    /** 聊天次数 */
    private Integer chatCount;

    /** 当日总费用（元） */
    private java.math.BigDecimal dailyCost;

    /** 创建时间 */
    private LocalDateTime createdAt;
}