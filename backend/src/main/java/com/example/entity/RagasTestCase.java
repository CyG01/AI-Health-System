package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * RAGAS 测试用例实体。
 * 用于 RAG 系统的质量监控评测。
 */
@Data
@TableName("ragas_test_case")
public class RagasTestCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 测试查询文本 */
    private String query;

    /** 期望检索到的上下文内容 */
    private String expectedContext;

    /** 期望的AI答案 */
    private String expectedAnswer;

    /** 测试类别：recall/faithfulness/hallucination */
    private String testCategory;

    /** 创建时间 */
    private java.time.LocalDateTime createdAt;
}