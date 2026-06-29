-- ============================================
-- AI智能化升级 - 数据库迁移脚本
-- 版本: 3.0.0
-- ============================================

-- AI健康咨询聊天会话表
DROP TABLE IF EXISTS `chat_session`;
CREATE TABLE `chat_session` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`      BIGINT       NOT NULL COMMENT '用户ID',
    `title`        VARCHAR(100) DEFAULT '新对话' COMMENT '会话标题',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`   TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user_id_update_time` (`user_id`, `update_time` DESC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天会话表';

-- AI健康咨询聊天消息表
DROP TABLE IF EXISTS `chat_message`;
CREATE TABLE `chat_message` (
    `id`           BIGINT       NOT NULL AUTO_INCREMENT,
    `session_id`   BIGINT       NOT NULL COMMENT '会话ID',
    `role`         VARCHAR(20)  NOT NULL COMMENT '角色 user/assistant',
    `content`      TEXT         NOT NULL COMMENT '消息内容',
    `create_time`  DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_session_id_create_time` (`session_id`, `create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI聊天消息表';

-- AI健康报告表
DROP TABLE IF EXISTS `health_report`;
CREATE TABLE `health_report` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `report_type`   VARCHAR(20)  NOT NULL COMMENT '报告类型: weekly/monthly',
    `report_period` VARCHAR(30)  NOT NULL COMMENT '报告周期(如: 2026W23 / 2026-06)',
    `ai_content`    TEXT         NOT NULL COMMENT 'AI生成的报告内容(JSON)',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_read`       TINYINT      DEFAULT 0 COMMENT '已读 0-否 1-是',
    `is_deleted`    TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user_id_create_time` (`user_id`, `create_time` DESC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI健康报告表';

-- 睡眠记录表
DROP TABLE IF EXISTS `sleep_record`;
CREATE TABLE `sleep_record` (
    `id`             BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`        BIGINT       NOT NULL COMMENT '用户ID',
    `record_date`    DATE         NOT NULL COMMENT '记录日期',
    `sleep_time`     TIME         NOT NULL COMMENT '入睡时间',
    `wake_time`      TIME         NOT NULL COMMENT '起床时间',
    `duration_min`   INT          NOT NULL COMMENT '睡眠时长(分钟)',
    `quality`        TINYINT      NOT NULL COMMENT '睡眠质量 1-很差 2-较差 3-一般 4-较好 5-很好',
    `dream_notes`    VARCHAR(500) DEFAULT NULL COMMENT '梦境/备注',
    `create_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_user_date` (`user_id`, `record_date`) USING BTREE,
    INDEX `idx_user_create_time` (`user_id`, `create_time` DESC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='睡眠记录表';

-- 饮水记录表
DROP TABLE IF EXISTS `water_record`;
CREATE TABLE `water_record` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `record_date`   DATE         NOT NULL COMMENT '记录日期',
    `amount_ml`     INT          NOT NULL COMMENT '饮水量(ml)',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`    TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user_date` (`user_id`, `record_date`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮水记录表';

-- 为运动字典表增加字段: 目标肌群、难度等级
ALTER TABLE `exercise_item`
    ADD COLUMN IF NOT EXISTS `target_muscle` VARCHAR(50) DEFAULT NULL COMMENT '目标肌群: 胸/背/肩/腿/核心/全身' AFTER `type`,
    ADD COLUMN IF NOT EXISTS `difficulty`    VARCHAR(20) DEFAULT '初级' COMMENT '难度: 初级/中级/高级' AFTER `target_muscle`,
    ADD COLUMN IF NOT EXISTS `ai_guidance`   TEXT        DEFAULT NULL COMMENT 'AI动作指导' AFTER `video_url`;

-- 更新运动字典种子数据
UPDATE `exercise_item` SET `target_muscle` = '全身',   `difficulty` = '初级' WHERE `name` = '跑步';
UPDATE `exercise_item` SET `target_muscle` = '全身',   `difficulty` = '初级' WHERE `name` = '快走';
UPDATE `exercise_item` SET `target_muscle` = '全身',   `difficulty` = '中级' WHERE `name` = '游泳';
UPDATE `exercise_item` SET `target_muscle` = '全身',   `difficulty` = '中级' WHERE `name` = '跳绳';
UPDATE `exercise_item` SET `target_muscle` = '腿',     `difficulty` = '初级' WHERE `name` = '骑行';
UPDATE `exercise_item` SET `target_muscle` = '核心',   `difficulty` = '初级' WHERE `name` = '瑜伽';
UPDATE `exercise_item` SET `target_muscle` = '腿',     `difficulty` = '初级' WHERE `name` = '深蹲';
UPDATE `exercise_item` SET `target_muscle` = '胸',     `difficulty` = '初级' WHERE `name` = '俯卧撑';
UPDATE `exercise_item` SET `target_muscle` = '核心',   `difficulty` = '初级' WHERE `name` = '仰卧起坐';
UPDATE `exercise_item` SET `target_muscle` = '背',     `difficulty` = '中级' WHERE `name` = '引体向上';