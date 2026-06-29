-- =====================================================
-- Phase 2 数据库迁移：感知闭环与情感智能
-- 前提条件：MySQL 8.0.37+
-- 注意：需要启用 VECTOR 支持
-- =====================================================

USE ai_health_system;

-- ----------------------------
-- 2.1: 用户长期记忆表（MySQL 原生 VECTOR）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_memory` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `memory_type` VARCHAR(30) NOT NULL COMMENT '记忆类型：PREFERENCE/INJURY/FEEDBACK/HABIT/ONBOARDING/HEALTH',
    `content` TEXT NOT NULL COMMENT '记忆内容（自然语言）',
    `embedding` LONGTEXT COMMENT '向量表示（1536维），存储为JSON数组字符串。如需原生VECTOR支持请使用pgvector或MySQL HeatWave Vector Store',
    `importance` TINYINT NOT NULL DEFAULT 5 COMMENT '重要性 1-10（≥7 永不删除）',
    `source` VARCHAR(30) NOT NULL COMMENT '来源：USER_INPUT/AI_GENERATED/SYSTEM_RECORD/ONBOARDING',
    `access_count` INT DEFAULT 1 COMMENT '访问次数',
    `last_accessed_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后访问时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_type` (`user_id`, `memory_type`),
    INDEX `idx_user_importance` (`user_id`, `importance`),
    INDEX `idx_cleanup` (`last_accessed_at`, `importance`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户长期记忆表（向量存储）';

-- ----------------------------
-- 2.2: 医疗知识库表（MySQL 原生 VECTOR）
-- ----------------------------
CREATE TABLE IF NOT EXISTS `knowledge_doc` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(300) NOT NULL COMMENT '文档标题',
    `content` TEXT NOT NULL COMMENT '文档内容',
    `category` VARCHAR(30) NOT NULL COMMENT '分类：EXERCISE/NUTRITION/REHABILITATION/PSYCHOLOGY/MEDICAL',
    `source_name` VARCHAR(200) COMMENT '来源名称（如 ACSM 运动处方指南）',
    `authority_level` CHAR(1) NOT NULL COMMENT '权威等级：A/B/C/D',
    `embedding` LONGTEXT COMMENT '向量表示（1536维），存储为JSON数组字符串。如需原生VECTOR支持请使用pgvector或MySQL HeatWave Vector Store',
    `version` VARCHAR(20) COMMENT '文档版本',
    `is_active` TINYINT DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_category` (`category`),
    INDEX `idx_authority` (`authority_level`),
    INDEX `idx_category_authority` (`category`, `authority_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='医疗知识库表（向量存储+分级）';

-- ----------------------------
-- 2.5: 用户画像表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_profile` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    `health_goal` VARCHAR(30) COMMENT '健康目标：LOSE_WEIGHT/GAIN_MUSCLE/STAY_HEALTHY/REHABILITATION/STRESS_RELIEF',
    `fitness_level` VARCHAR(30) COMMENT '运动基础：SEDENTARY/OCCASIONAL/REGULAR/ADVANCED',
    `chronic_diseases` VARCHAR(500) COMMENT '慢性疾病（逗号分隔）',
    `injuries` VARCHAR(500) COMMENT '运动损伤（逗号分隔）',
    `diet_preferences` VARCHAR(500) COMMENT '饮食偏好/忌口（逗号分隔）',
    `daily_available_min` INT COMMENT '每日可用运动时间(分钟)',
    `sleep_quality` VARCHAR(20) COMMENT '睡眠质量自评：POOR/AVERAGE/GOOD',
    `stress_level` VARCHAR(20) COMMENT '压力水平：LOW/MEDIUM/HIGH',
    `preferred_tone` VARCHAR(20) COMMENT '偏好语气：STRICT/COMFORTING/CELEBRATORY/NEUTRAL',
    `onboarding_completed` TINYINT DEFAULT 0 COMMENT '是否完成新手引导',
    `onboarding_completed_at` DATETIME COMMENT '完成新手引导时间',
    `registration_day` INT DEFAULT 0 COMMENT '注册第几天',
    `last_active_at` DATETIME COMMENT '最后活跃时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_onboarding` (`onboarding_completed`, `registration_day`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户画像表';

-- ----------------------------
-- 2.3: 情绪记录表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `emotion_record` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `session_id` BIGINT COMMENT '关联的聊天会话ID',
    `emotion_type` VARCHAR(20) NOT NULL COMMENT '情绪类型：TIRED/FRUSTRATED/EXCITED/ANXIOUS/PAIN/NEUTRAL',
    `confidence` DECIMAL(3,2) COMMENT '置信度 0.00-1.00',
    `original_text` TEXT COMMENT '原始用户输入',
    `triggered_tone` VARCHAR(20) COMMENT '触发切换的语气',
    `action_taken` VARCHAR(50) COMMENT '采取的动作：NONE/ADJUST_PLAN/REDUCE_INTENSITY/PUSH_ENCOURAGEMENT',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_time` (`user_id`, `created_at`),
    INDEX `idx_user_emotion` (`user_id`, `emotion_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='情绪识别记录表';

-- ----------------------------
-- 2.4: 推送频率管控表
-- ----------------------------
CREATE TABLE IF NOT EXISTS `push_control` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `push_type` VARCHAR(30) NOT NULL COMMENT '推送类型：CHECKIN/WATER/EXERCISE/SLEEP/CALORIE_ALERT/WEEKLY_REPORT/SAFETY_ALERT',
    `priority` VARCHAR(5) NOT NULL DEFAULT 'P3' COMMENT '优先级：P0/P1/P2/P3',
    `title` VARCHAR(200) NOT NULL COMMENT '推送标题',
    `content` VARCHAR(500) NOT NULL COMMENT '推送内容',
    `is_sent` TINYINT DEFAULT 0 COMMENT '是否已发送',
    `is_read` TINYINT DEFAULT 0 COMMENT '是否已读',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_type_time` (`user_id`, `push_type`, `created_at`),
    INDEX `idx_user_sent` (`user_id`, `is_sent`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='推送频率管控表';

-- ----------------------------
-- 2.4: 推送免打扰时段配置
-- ----------------------------
CREATE TABLE IF NOT EXISTS `user_push_preference` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL UNIQUE COMMENT '用户ID',
    `dnd_enabled` TINYINT DEFAULT 0 COMMENT '是否启用免打扰',
    `dnd_start_time` TIME COMMENT '免打扰开始时间',
    `dnd_end_time` TIME COMMENT '免打扰结束时间',
    `daily_max_push` INT DEFAULT 5 COMMENT '每日最大推送数量',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户推送偏好表';

-- =====================================================
-- 播种数据
-- =====================================================

-- 医疗知识库种子数据（A类权威）
INSERT INTO knowledge_doc (title, content, category, source_name, authority_level, version) VALUES
('ACSM 高血压运动建议', '高血压患者应避免高强度间歇训练（HIIT）、大重量力量训练、倒立等头部低于心脏的动作。推荐进行中等强度有氧运动如快走、游泳、骑行，每次30-40分钟，每周5-7天。运动前应测量血压，收缩压≥160mmHg或舒张压≥100mmHg时应暂停运动。力量训练应以低重量、高重复次数为主，避免憋气（避免Valsalva动作）。', 'EXERCISE', 'ACSM 运动处方指南 2024', 'A', '2024'),
('中国居民膳食指南 核心推荐', '食物多样，谷类为主，每天摄入谷薯类食物250-400g。多吃蔬菜和水果，保证每天摄入300-500g蔬菜，深色蔬菜应占1/2。每天摄入200-350g新鲜水果。适量吃鱼、禽、蛋、瘦肉，每周吃鱼280-525g，畜禽肉280-525g，蛋类280-350g。少盐少油，控糖限酒，成人每天食盐不超过6g，烹调油25-30g，添加糖不超过50g，最好在25g以下。', 'NUTRITION', '中国居民膳食指南 2022', 'A', '2022'),
('WHO 糖尿病运动指南', '2型糖尿病患者应每周进行至少150分钟的中等强度有氧运动，每周至少3天，连续不运动的天数不应超过2天。推荐进行抗阻训练，每周2-3次，每次包括8-10个动作，每个动作1-3组。运动前应检测血糖，血糖<5.6mmol/L时应补充碳水化合物。避免在胰岛素作用高峰期运动。合并视网膜病变时应避免剧烈运动和头部低于心脏的动作。', 'EXERCISE', 'WHO 慢性病运动指南', 'A', '2020'),
('ACSM 减重运动处方', '减重目标应将有氧运动和抗阻训练结合。有氧运动每周至少250-300分钟中等强度才能产生显著的体重减轻。高强度间歇训练（HIIT）可有效减少内脏脂肪。抗阻训练每周2-3次，有助于保持瘦体重。逐步增加运动量，每周增加不超过10%。配合饮食控制效果更佳，单纯运动减重效果有限。', 'EXERCISE', 'ACSM 运动处方指南 2024', 'A', '2024');

-- 提示模板：4种语气模板
INSERT INTO prompt_template (template_name, template_content, version, status) VALUES
('coach_tone_strict', '你是一名严格但专业的健康教练。请用坚定、直接的语气回应用户。鼓励严格要求，不使用过于柔软的表达。当用户找借口时，温和但坚定地推动其完成计划。', 1, 1),
('coach_tone_comforting', '你是一名温暖、富有同理心的健康教练。用户可能正处于疲惫、沮丧或情绪低落中。请使用安抚、理解的语气，先共情再给建议。降低用户压力，强调小步前进的价值。不要说教或批评，多用鼓励性语言。', 1, 1),
('coach_tone_celebratory', '你是一名充满热情的健康教练。用户完成了目标或取得了进步。请使用兴奋、庆祝的语气，毫不吝啬赞美和正面反馈。帮助用户感受成就感和自豪感，鼓励其继续保持好状态。', 1, 1),
('coach_tone_neutral', '你是一名理性、分析型的健康教练。用户需要客观的数据分析和专业的建议。请使用冷静、专业的语气，重点放在数据解读和科学建议上。避免过多的情绪化表达，以事实和数据说话。', 1, 1);