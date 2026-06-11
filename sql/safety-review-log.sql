-- 安全审查审计日志表
DROP TABLE IF EXISTS `safety_review_log`;
CREATE TABLE `safety_review_log` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
    `verdict`        VARCHAR(10)  NOT NULL COMMENT '审查判定: PASS/MODIFY/BLOCK',
    `risk_level`     VARCHAR(10)  DEFAULT 'none' COMMENT '风险等级: high/medium/low/none',
    `issues`         TEXT         COMMENT '发现的问题列表(JSON数组)',
    `suggestions`    TEXT         COMMENT '修改建议(JSON数组)',
    `fallback_mode`  TINYINT      DEFAULT 0 COMMENT '是否规则引擎兜底 0-否 1-是',
    `content_digest` VARCHAR(500) COMMENT '待审查内容摘要',
    `latency_ms`     BIGINT       COMMENT '审查耗时(毫秒)',
    `created_at`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user_created` (`user_id`, `created_at` DESC) USING BTREE,
    INDEX `idx_verdict` (`verdict`, `created_at` DESC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='安全审查审计日志表';