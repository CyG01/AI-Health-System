-- Phase 3: 用户反馈表
-- 用于收集用户对AI建议的评价，支持人工审核和知识库优化
CREATE TABLE IF NOT EXISTS ai_feedback (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    ai_response_id VARCHAR(100) NOT NULL COMMENT '关联的AI响应ID',
    rating VARCHAR(20) NOT NULL COMMENT '评价: useful/useless/incorrect',
    comment TEXT COMMENT '用户详细反馈',
    manual_reviewed TINYINT DEFAULT 0 COMMENT '是否已人工审核: 0=未审核,1=已审核',
    reviewer_id BIGINT COMMENT '审核人ID',
    review_result VARCHAR(50) COMMENT '审核结果: valid/invalid/duplicate',
    resolved_at DATETIME COMMENT '问题解决时间',
    created_at DATETIME DEFAULT NOW() COMMENT '创建时间',
    updated_at DATETIME DEFAULT NOW() ON UPDATE NOW() COMMENT '更新时间',
    INDEX idx_user_time (user_id, created_at),
    INDEX idx_review_status (manual_reviewed)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户对AI建议的反馈表';