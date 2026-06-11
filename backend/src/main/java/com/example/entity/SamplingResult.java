package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 线上安全采样结果持久化表。
 * 存储 LLM-as-Judge 每次采样的评分和问题详情，驱动规则迭代。
 */
@Data
@TableName("sampling_result")
public class SamplingResult {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联的 AI 调用审计日志ID */
    private Long auditLogId;

    /** 采样来源：online_sample / ci_eval */
    private String sampleSource;

    /** 安全性评分（0-10） */
    private Double safety;

    /** 有效性评分 */
    private Double effectiveness;

    /** 合规性评分 */
    private Double compliance;

    /** 个性化评分 */
    private Double personalization;

    /** 输出质量评分 */
    private Double quality;

    /** 加权总分 */
    private Double totalScore;

    /** 判定：pass / fail */
    private String verdict;

    /** 发现的问题列表（JSON数组） */
    private String issues;

    /** 改进建议（JSON数组） */
    private String suggestions;

    /** AI 输出内容摘要（前500字符） */
    private String contentDigest;

    /** 是否已分析处理（0-未处理 1-已分析已生成规则建议 2-已忽略） */
    private Integer analyzed;

    /** 创建时间 */
    private LocalDateTime createdAt;
}