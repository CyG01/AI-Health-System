-- =====================================================
-- Phase 3 数据库迁移：家庭组共享 & 隐私增强 & 多模态演进
-- 前提条件：MySQL 8.0.37+
-- 包含：家庭组架构、记忆分层、时区支持、隐私焚毁
-- =====================================================

USE ai_health_system;

-- =====================================================
-- 1. 家庭组共享架构 (Family Link)
-- =====================================================

-- ----------------------------
-- 1.1: 家庭组表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_family` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `family_name` VARCHAR(100) NOT NULL COMMENT '家庭名称',
    `family_avatar` VARCHAR(500) COMMENT '家庭头像URL',
    `creator_id` BIGINT NOT NULL COMMENT '创建人用户ID',
    `subscription_id` BIGINT COMMENT '关联的家庭订阅ID',
    `max_members` INT DEFAULT 6 COMMENT '最大成员数（默认6人）',
    `share_health_data` TINYINT DEFAULT 0 COMMENT '是否共享健康数据 0=否 1=是',
    `share_reports` TINYINT DEFAULT 1 COMMENT '是否共享周报 0=否 1=是',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0=禁用 1=正常',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_creator` (`creator_id`),
    INDEX `idx_subscription` (`subscription_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭组表';

-- ----------------------------
-- 1.2: 家庭成员关系表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_family_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `family_id` BIGINT NOT NULL COMMENT '家庭ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `member_role` VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT '成员角色：OWNER(主账号)/ADMIN(管理员)/MEMBER(普通成员)/CHILD(儿童)/ELDER(老人)',
    `nickname_in_family` VARCHAR(50) COMMENT '在家庭中的昵称',
    `data_visibility` VARCHAR(20) DEFAULT 'PRIVATE' COMMENT '数据可见性：PRIVATE(仅自己)/FAMILY(全家可见)/REPORT_ONLY(仅周报)',
    `can_view_members` TEXT COMMENT '可查看的成员ID列表（JSON数组，用于细粒度控制）',
    `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    `invited_by` BIGINT COMMENT '邀请人用户ID',
    `status` TINYINT DEFAULT 1 COMMENT '状态 0=已退出 1=正常 2=待确认',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_family_user` (`family_id`, `user_id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_family_role` (`family_id`, `member_role`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭成员关系表';

-- ----------------------------
-- 1.3: 家庭邀请表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `sys_family_invitation` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `family_id` BIGINT NOT NULL COMMENT '家庭ID',
    `inviter_id` BIGINT NOT NULL COMMENT '邀请人ID',
    `invitee_phone` VARCHAR(20) COMMENT '被邀请人手机号',
    `invitee_email` VARCHAR(100) COMMENT '被邀请人邮箱',
    `invite_code` VARCHAR(32) NOT NULL COMMENT '邀请码',
    `member_role` VARCHAR(20) DEFAULT 'MEMBER' COMMENT '邀请的角色',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间',
    `status` TINYINT DEFAULT 0 COMMENT '状态 0=待接受 1=已接受 2=已过期 3=已取消',
    `accepted_user_id` BIGINT COMMENT '接受邀请的用户ID',
    `accepted_at` DATETIME COMMENT '接受时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_invite_code` (`invite_code`),
    INDEX `idx_family` (`family_id`),
    INDEX `idx_inviter` (`inviter_id`),
    INDEX `idx_status_expire` (`status`, `expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='家庭邀请表';

-- =====================================================
-- 2. AI长程记忆增强 (Long-Term Memory)
-- =====================================================

-- ----------------------------
-- 2.1: 扩展 user_memory 表 - 增加记忆分层字段
-- ----------------------------
ALTER TABLE `user_memory` 
    ADD COLUMN IF NOT EXISTS `memory_layer` VARCHAR(20) DEFAULT 'TIMELINE' COMMENT '记忆层级：SESSION(瞬时)/PROFILE(核心画像)/TIMELINE(时间线)' AFTER `memory_type`,
    ADD COLUMN IF NOT EXISTS `event_time` DATETIME COMMENT '事件发生时间（用于时间线记忆）' AFTER `content`,
    ADD COLUMN IF NOT EXISTS `decay_rate` DECIMAL(5,4) DEFAULT 0.0100 COMMENT '时间衰减系数（每天衰减比例）' AFTER `importance`,
    ADD COLUMN IF NOT EXISTS `tags` VARCHAR(500) COMMENT '标签（逗号分隔，用于快速检索）' AFTER `source`,
    ADD INDEX IF NOT EXISTS `idx_user_layer` (`user_id`, `memory_layer`),
    ADD INDEX IF NOT EXISTS `idx_user_event_time` (`user_id`, `event_time`);

-- ----------------------------
-- 2.2: 健康事件时间线表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `health_event_timeline` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `event_type` VARCHAR(30) NOT NULL COMMENT '事件类型：SYMPTOM(症状)/DIAGNOSIS(诊断)/MEDICATION(用药)/SURGERY(手术)/LIFESTYLE_CHANGE(生活方式改变)/CHECKUP(体检)',
    `event_title` VARCHAR(200) NOT NULL COMMENT '事件标题',
    `event_description` TEXT COMMENT '事件详细描述',
    `event_date` DATE NOT NULL COMMENT '事件发生日期',
    `severity` VARCHAR(20) DEFAULT 'MODERATE' COMMENT '严重程度：MILD(轻微)/MODERATE(中等)/SEVERE(严重)',
    `related_indicators` TEXT COMMENT '相关指标（JSON格式）',
    `source` VARCHAR(30) DEFAULT 'USER_INPUT' COMMENT '来源：USER_INPUT/AI_EXTRACTED/MEDICAL_RECORD',
    `is_verified` TINYINT DEFAULT 0 COMMENT '是否已验证 0=否 1=是',
    `embedding` LONGTEXT COMMENT '向量表示（1536维）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_time` (`user_id`, `event_date`),
    INDEX `idx_user_type` (`user_id`, `event_type`),
    INDEX `idx_severity` (`severity`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='健康事件时间线表';

-- =====================================================
-- 3. 动态时区支持 (Chronos Scheduler)
-- =====================================================

-- ----------------------------
-- 3.1: 扩展 user_profile 表 - 增加时区字段
-- ----------------------------
ALTER TABLE `user_profile` 
    ADD COLUMN IF NOT EXISTS `timezone_id` VARCHAR(50) DEFAULT 'Asia/Shanghai' COMMENT '用户时区ID（IANA格式）' AFTER `current_scenario`,
    ADD COLUMN IF NOT EXISTS `last_locate_time` DATETIME COMMENT '最后定位时间' AFTER `timezone_id`,
    ADD COLUMN IF NOT EXISTS `latitude` DECIMAL(10,7) COMMENT '纬度' AFTER `last_locate_time`,
    ADD COLUMN IF NOT EXISTS `longitude` DECIMAL(10,7) COMMENT '经度' AFTER `latitude`,
    ADD COLUMN IF NOT EXISTS `locale` VARCHAR(20) DEFAULT 'zh_CN' COMMENT '用户语言区域' AFTER `longitude`;

-- ----------------------------
-- 3.2: 用户提醒时间表（个性化提醒配置）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_reminder_schedule` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `reminder_type` VARCHAR(30) NOT NULL COMMENT '提醒类型：CHECKIN/WATER/EXERCISE/SLEEP/MEAL/MEDICATION',
    `reminder_time` VARCHAR(5) NOT NULL COMMENT '提醒时间 HH:mm（用户当地时间）',
    `repeat_days` VARCHAR(20) DEFAULT '1,2,3,4,5,6,7' COMMENT '重复星期（1=周一...7=周日）',
    `is_enabled` TINYINT DEFAULT 1 COMMENT '是否启用',
    `snooze_minutes` INT DEFAULT 5 COMMENT '贪睡时长（分钟）',
    `vibration_only` TINYINT DEFAULT 0 COMMENT '仅震动 0=否 1=是',
    `custom_message` VARCHAR(200) COMMENT '自定义提醒消息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_type_time` (`user_id`, `reminder_type`, `reminder_time`),
    INDEX `idx_user_enabled` (`user_id`, `is_enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户个性化提醒时间表';

-- =====================================================
-- 4. 多模态输入支持 (Zero-Effort Ingestion)
-- =====================================================

-- ----------------------------
-- 4.1: 多模态上传记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `multimodal_upload` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `upload_type` VARCHAR(20) NOT NULL COMMENT '上传类型：IMAGE(图片)/AUDIO(语音)/VIDEO(视频)',
    `file_url` VARCHAR(500) NOT NULL COMMENT '文件存储URL',
    `file_size` BIGINT COMMENT '文件大小（字节）',
    `mime_type` VARCHAR(100) COMMENT 'MIME类型',
    `duration_seconds` INT COMMENT '时长（秒，音视频）',
    `processing_status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '处理状态：PENDING/PROCESSING/SUCCESS/FAILED',
    `processing_result` TEXT COMMENT 'AI解析结果（JSON格式）',
    `record_type` VARCHAR(30) COMMENT '关联的记录类型：DIET/EXERCISE/SYMPTOM/BLOOD_SUGAR',
    `record_id` BIGINT COMMENT '关联的记录ID',
    `model_used` VARCHAR(50) COMMENT '使用的模型（如 qwen-vl-max）',
    `confidence` DECIMAL(3,2) COMMENT '解析置信度 0.00-1.00',
    `user_corrected` TINYINT DEFAULT 0 COMMENT '用户是否修正过 0=否 1=是',
    `error_message` TEXT COMMENT '错误信息（处理失败时）',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `processed_at` DATETIME COMMENT '处理完成时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_time` (`user_id`, `created_at`),
    INDEX `idx_status` (`processing_status`),
    INDEX `idx_record` (`record_type`, `record_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='多模态上传记录表';

-- =====================================================
-- 5. 隐私增强与物理焚毁 (Privacy Vault)
-- =====================================================

-- ----------------------------
-- 5.1: 隐私操作审计日志表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `privacy_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `operation_type` VARCHAR(30) NOT NULL COMMENT '操作类型：DATA_EXPORT(数据导出)/DATA_PURGE(数据焚毁)/CONSENT_CHANGE(授权变更)/PROFILE_VIEW(画像查看)/MEMORY_CLEAR(记忆清除)',
    `operation_detail` TEXT COMMENT '操作详情（JSON格式）',
    `scope` VARCHAR(20) DEFAULT 'ALL' COMMENT '数据范围：ALL/HEALTH_RECORDS/CHAT_HISTORY/USER_MEMORY/PROFILE',
    `ip_address` VARCHAR(50) COMMENT '操作IP地址',
    `user_agent` VARCHAR(500) COMMENT '用户代理',
    `status` VARCHAR(20) DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS/FAILED/PROCESSING',
    `operator_id` BIGINT COMMENT '操作人ID（用户自己或管理员）',
    `operator_role` VARCHAR(20) COMMENT '操作人角色：USER/ADMIN/SYSTEM',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `completed_at` DATETIME COMMENT '完成时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_time` (`user_id`, `created_at`),
    INDEX `idx_operation` (`operation_type`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='隐私操作审计日志表';

-- ----------------------------
-- 5.2: 数据导出任务表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `data_export_task` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `export_scope` TEXT COMMENT '导出范围（JSON数组）',
    `export_format` VARCHAR(20) DEFAULT 'JSON' COMMENT '导出格式：JSON/CSV/PDF',
    `file_url` VARCHAR(500) COMMENT '导出文件URL',
    `file_size` BIGINT COMMENT '文件大小（字节）',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态：PENDING/PROCESSING/SUCCESS/FAILED/EXPIRED',
    `expire_time` DATETIME NOT NULL COMMENT '过期时间（默认7天）',
    `download_count` INT DEFAULT 0 COMMENT '下载次数',
    `error_message` TEXT COMMENT '错误信息',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `completed_at` DATETIME COMMENT '完成时间',
    PRIMARY KEY (`id`),
    INDEX `idx_user_status` (`user_id`, `status`),
    INDEX `idx_expire` (`expire_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据导出任务表';

-- ----------------------------
-- 5.3: 扩展 sys_user 表 - 增加隐私沙盒开关
-- ----------------------------
ALTER TABLE `sys_user` 
    ADD COLUMN IF NOT EXISTS `ai_memory_enabled` TINYINT DEFAULT 1 COMMENT 'AI记忆沙盒开关 0=关闭 1=开启' AFTER `disclaimer_accepted_at`,
    ADD COLUMN IF NOT EXISTS `do_not_track` TINYINT DEFAULT 0 COMMENT '不追踪模式 0=关闭 1=开启' AFTER `ai_memory_enabled`,
    ADD COLUMN IF NOT EXISTS `data_retention_days` INT DEFAULT 365 COMMENT '数据保留天数' AFTER `do_not_track`;

-- =====================================================
-- 6. 初始化数据
-- =====================================================

-- 为现有用户创建默认提醒配置（可选，按需执行）
-- INSERT INTO user_reminder_schedule (user_id, reminder_type, reminder_time, is_enabled)
-- SELECT id, 'CHECKIN', '08:00', 1 FROM sys_user WHERE status = 1
-- ON DUPLICATE KEY UPDATE is_enabled = VALUES(is_enabled);

-- =====================================================
-- 迁移完成标记
-- =====================================================
-- 迁移版本: 3.0.0
-- 迁移日期: 2026-07-01
