-- Phase 3: RAGAS测试用例表
-- 用于RAG系统质量监控的黄金测试集
CREATE TABLE IF NOT EXISTS ragas_test_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    query VARCHAR(500) NOT NULL COMMENT '测试查询文本',
    expected_context TEXT COMMENT '期望检索到的上下文内容',
    expected_answer TEXT COMMENT '期望的AI答案',
    test_category VARCHAR(50) DEFAULT 'recall' COMMENT '测试类别: recall/faithfulness/hallucination',
    created_at DATETIME DEFAULT NOW() COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAGAS质量监控测试用例表';