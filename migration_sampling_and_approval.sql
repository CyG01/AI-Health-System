-- ============================================================
-- 缺陷修复迁移脚本：安全采样结果持久化 + 管理员审批流程
-- ============================================================

-- 1. 安全采样结果持久化表
CREATE TABLE IF NOT EXISTS sampling_result (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    audit_log_id BIGINT NULL COMMENT '关联AI调用审计日志ID',
    sample_source VARCHAR(32) NOT NULL DEFAULT 'online_sample' COMMENT '采样来源',
    safety DOUBLE NULL COMMENT '安全性评分(0-10)',
    effectiveness DOUBLE NULL COMMENT '有效性评分',
    compliance DOUBLE NULL COMMENT '合规性评分',
    personalization DOUBLE NULL COMMENT '个性化评分',
    quality DOUBLE NULL COMMENT '输出质量评分',
    total_score DOUBLE NULL COMMENT '加权总分',
    verdict VARCHAR(16) NULL COMMENT '判定: pass/fail',
    issues TEXT NULL COMMENT '发现的问题(JSON数组)',
    suggestions TEXT NULL COMMENT '改进建议(JSON数组)',
    content_digest VARCHAR(500) NULL COMMENT 'AI输出内容摘要',
    analyzed TINYINT NOT NULL DEFAULT 0 COMMENT '分析状态: 0未处理 1已生成规则 2已忽略',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_analyzed_created (analyzed, created_at),
    INDEX idx_verdict (verdict),
    INDEX idx_safety (safety)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='线上安全采样结果';

-- 2. 规则建议表（采样分析后生成的待审批规则）
CREATE TABLE IF NOT EXISTS rule_suggestion (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    suggestion_type VARCHAR(32) NOT NULL COMMENT '建议类型: safety_rule / compliance_rule',
    rule_category VARCHAR(64) NULL COMMENT '规则分类',
    trigger_pattern VARCHAR(500) NOT NULL COMMENT '触发关键词/正则',
    action VARCHAR(32) NOT NULL DEFAULT 'block' COMMENT '动作: block/warning/flag',
    priority INT NOT NULL DEFAULT 5 COMMENT '优先级1-10',
    reason TEXT NULL COMMENT '建议原因（关联采样数据）',
    source_sample_ids VARCHAR(500) NULL COMMENT '来源采样ID列表(逗号分隔)',
    hit_count INT NOT NULL DEFAULT 0 COMMENT '命中次数',
    status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/approved/rejected',
    reviewed_by VARCHAR(64) NULL COMMENT '审核人',
    reviewed_at DATETIME NULL COMMENT '审核时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_status (status),
    INDEX idx_type_status (suggestion_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='规则建议(采样分析生成)';

-- 3. 管理员审批表
CREATE TABLE IF NOT EXISTS admin_approval (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    operator_id BIGINT NOT NULL COMMENT '操作发起人ID',
    operator_name VARCHAR(64) NOT NULL COMMENT '操作发起人',
    action_type VARCHAR(64) NOT NULL COMMENT '操作类型: ban_user/unban_user/delete_food/delete_exercise/export_users/send_notification/batch_update_user',
    target_description VARCHAR(500) NOT NULL COMMENT '操作目标描述',
    request_payload TEXT NULL COMMENT '请求参数JSON',
    status VARCHAR(16) NOT NULL DEFAULT 'pending' COMMENT '状态: pending/approved/rejected',
    approver_id BIGINT NULL COMMENT '审批人ID',
    approver_name VARCHAR(64) NULL COMMENT '审批人名称',
    approve_reason TEXT NULL COMMENT '审批意见',
    requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    approved_at DATETIME NULL COMMENT '审批时间',
    executed TINYINT NOT NULL DEFAULT 0 COMMENT '是否已执行 0未执行 1已执行',
    INDEX idx_status (status),
    INDEX idx_operator (operator_id),
    INDEX idx_approver (approver_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员审批表';