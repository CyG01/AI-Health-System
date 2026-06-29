-- =====================================================
-- Phase 1-3 数据库迁移：补齐所有缺失表
-- =====================================================

-- ----------------------------
-- Phase 0 补充：管理员审计日志
-- ----------------------------
-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS ai_health_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 使用该数据库
USE ai_health_system;
CREATE TABLE IF NOT EXISTS `admin_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50) COMMENT '操作人名称',
    `action` VARCHAR(100) NOT NULL COMMENT '操作动作',
    `target_type` VARCHAR(50) COMMENT '操作目标类型',
    `target_id` BIGINT COMMENT '操作目标ID',
    `detail` TEXT COMMENT '操作详情',
    `ip` VARCHAR(50) COMMENT '操作IP',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_operator_time` (`operator_id`, `create_time`),
    INDEX `idx_action` (`action`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员操作审计日志';

-- ----------------------------
-- Phase 1.3：安全规则表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `safety_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_condition` VARCHAR(100) COMMENT '用户状况：高血压/膝盖损伤/孕期',
    `forbidden_keywords` VARCHAR(300) COMMENT '禁忌运动关键词，逗号分隔',
    `max_duration` INT COMMENT '最长运动时间(分钟)',
    `max_intensity` VARCHAR(20) COMMENT '最大强度：低/中/高',
    `risk_level` VARCHAR(20) COMMENT '风险等级：HIGH/MEDIUM/LOW',
    `alternative_suggestion` VARCHAR(200) COMMENT '替代建议',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否启用',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全规则表（禁忌运动拦截）';

INSERT INTO safety_rule (user_condition, forbidden_keywords, max_duration, max_intensity, risk_level, alternative_suggestion) VALUES
('高血压', '深蹲,HIIT,倒立,冲刺,大重量', 30, '中', 'HIGH', '快走或低强度有氧'),
('膝盖损伤', '跑步,跳跃,深蹲,箭步蹲,爬楼梯', 20, '低', 'HIGH', '游泳或上肢力量训练'),
('腰部损伤', '硬拉,仰卧起坐,深蹲,跳箱', 20, '低', 'HIGH', '平板支撑或游泳'),
('孕期', '腹部训练,高强度间歇,跳跃,仰卧起坐', 20, '低', 'HIGH', '孕期瑜伽或散步'),
('心脏病', '高强度间歇,冲刺,大重量,潜水', 25, '低', 'HIGH', '散步或太极'),
('骨质疏松', '跳跃,冲击运动,大重量,对抗性运动', 25, '低', 'MEDIUM', '游泳或低强度力量训练'),
('糖尿病', '空腹运动,极高强度间歇', 40, '中', 'MEDIUM', '餐后1小时进行中等强度有氧'),
('颈椎病', '倒立,颈桥,头部负重', 30, '低', 'HIGH', '颈部拉伸或游泳'),
('腰椎间盘', '硬拉,深蹲,仰卧起坐,跳箱', 20, '低', 'HIGH', '平板支撑或游泳'),
('肩周炎', '引体向上,大重量推举,投掷类', 25, '低', 'MEDIUM', '弹力带康复训练'),
('痛风', '长时间跑步,跳跃,高强度下肢训练', 25, '低', 'MEDIUM', '游泳或上肢训练'),
('哮喘', '冲刺跑,HIIT,冷空气户外运动', 30, '中', 'HIGH', '室内温和有氧'),
('肥胖', '跳跃,长时间跑步,大重量深蹲', 30, '中', 'MEDIUM', '快走或椭圆机或游泳'),
('术后', '大重量,高强度,对抗性运动', 20, '低', 'HIGH', '遵医嘱进行康复训练');

-- ----------------------------
-- Phase 1.3：合规校验规则表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `compliance_rule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `rule_type` VARCHAR(50) COMMENT '规则类型：forbidden_term/manual_check_required',
    `match_pattern` VARCHAR(500) COMMENT '匹配模式（正则或关键词）',
    `action` VARCHAR(50) COMMENT '动作：block/warn/append_disclaimer',
    `description` VARCHAR(300) COMMENT '说明',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否启用',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合规校验规则表';

INSERT INTO compliance_rule (rule_type, match_pattern, action, description) VALUES
('forbidden_term', '治愈|根治|药到病除|包治|特效药', 'block', '禁止使用绝对化医疗用语'),
('forbidden_term', '诊断|确诊|处方|开药|用药建议', 'block', '禁止超出健康建议范畴'),
('manman_check', '术后|手术后|出院|康复训练', 'warn', '术后建议需标注遵医嘱'),
('append_disclaimer', '.*', 'append', '所有AI回复统一追加免责声明');

-- ----------------------------
-- Phase 2.3/2.4：聊天会话与消息表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `chat_session` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `title` VARCHAR(200) COMMENT '会话标题',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI聊天会话表';

CREATE TABLE IF NOT EXISTS `chat_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `session_id` BIGINT NOT NULL COMMENT '会话ID',
    `role` VARCHAR(20) NOT NULL COMMENT '角色：user/assistant/system',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_session_id` (`session_id`),
    INDEX `idx_session_time` (`session_id`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI聊天消息表';

-- ----------------------------
-- Phase 2.4：睡眠记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sleep_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `record_date` DATE NOT NULL COMMENT '记录日期',
    `sleep_time` TIME COMMENT '入睡时间',
    `wake_time` TIME COMMENT '醒来时间',
    `duration_min` INT COMMENT '睡眠时长(分钟)',
    `quality` INT DEFAULT 3 COMMENT '睡眠质量(1-5)',
    `dream_notes` VARCHAR(500) COMMENT '梦境/备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `is_deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_date` (`user_id`, `record_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='睡眠记录表';

-- ----------------------------
-- Phase 2.4：AI 健康报告表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `health_report` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `report_type` VARCHAR(50) COMMENT '报告类型：weekly/monthly/summary',
    `report_period` VARCHAR(50) COMMENT '报告周期（如 2026-W23）',
    `ai_content` TEXT COMMENT 'AI生成的报告内容',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `is_read` TINYINT DEFAULT 0 COMMENT '是否已读',
    `is_deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_user_period` (`user_id`, `report_period`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI健康报告表';

-- ----------------------------
-- Phase 3.2：LLM 评测测试用例表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `llm_test_case` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `category` VARCHAR(50) COMMENT '分类：basic/risk/edge',
    `user_profile` TEXT COMMENT '模拟用户画像JSON',
    `user_input` TEXT COMMENT '模拟用户输入',
    `expected_behavior` TEXT COMMENT '期望行为描述',
    `forbidden_content` TEXT COMMENT '不应出现的内容',
    `safety_level` VARCHAR(20) COMMENT '安全等级：safe/risky/critical',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_safety_level` (`safety_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM评测测试用例表';

-- ----------------------------
-- Phase 3.4：用户订阅表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `subscription` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `tier` VARCHAR(20) NOT NULL DEFAULT 'free' COMMENT '订阅等级：free/pro/enterprise',
    `status` VARCHAR(20) NOT NULL DEFAULT 'active' COMMENT '状态：active/expired/cancelled',
    `start_time` DATETIME COMMENT '订阅开始时间',
    `end_time` DATETIME COMMENT '订阅到期时间',
    `auto_renew` TINYINT DEFAULT 0 COMMENT '是否自动续费',
    `order_no` VARCHAR(100) COMMENT '订单号',
    `payment_channel` VARCHAR(50) COMMENT '支付渠道',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_id` (`user_id`),
    INDEX `idx_tier` (`tier`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户订阅表';

-- ----------------------------
-- Phase 3.4：用户用量统计表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_usage` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `usage_date` DATE NOT NULL COMMENT '使用日期',
    `input_tokens` INT DEFAULT 0 COMMENT '输入Token数',
    `output_tokens` INT DEFAULT 0 COMMENT '输出Token数',
    `api_call_count` INT DEFAULT 0 COMMENT 'API调用次数',
    `plan_gen_count` INT DEFAULT 0 COMMENT '计划生成次数',
    `food_recog_count` INT DEFAULT 0 COMMENT '食物识别次数',
    `chat_count` INT DEFAULT 0 COMMENT '聊天次数',
    `daily_cost` DECIMAL(10,4) DEFAULT 0.0000 COMMENT '当日费用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE INDEX `uk_user_date` (`user_id`, `usage_date`),
    INDEX `idx_usage_date` (`usage_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户每日用量统计表';