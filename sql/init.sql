-- ============================================
-- AI 健康管理系统 - 数据库初始化脚本
-- 版本: 2.0.0
-- ============================================

CREATE DATABASE IF NOT EXISTS ai_health_system
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_health_system;

-- ============================================
-- 系统用户表
-- ============================================
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `username`    VARCHAR(50)  NOT NULL COMMENT '用户名',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt)',
    `phone`       VARCHAR(20)  NOT NULL COMMENT '手机号',
    `nickname`    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    `avatar`      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    `gender`      TINYINT      DEFAULT NULL COMMENT '性别 0-女 1-男',
    `age`         INT          DEFAULT NULL COMMENT '年龄',
    `role`        VARCHAR(20)  DEFAULT 'user' COMMENT '角色 user/admin',
    `status`      TINYINT      DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    `version`     INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `username` (`username`) USING BTREE,
    UNIQUE KEY `phone` (`phone`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ============================================
-- 健康档案表
-- ============================================
DROP TABLE IF EXISTS `health_record`;
CREATE TABLE `health_record` (
    `id`              BIGINT  NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT  NOT NULL COMMENT '用户ID',
    `height`          INT     NOT NULL COMMENT '身高(cm)',
    `weight`          INT     NOT NULL COMMENT '体重(kg)',
    `target_weight`   INT         DEFAULT NULL COMMENT '目标体重(kg)',
    `bmi`             DECIMAL(4,1) DEFAULT NULL COMMENT 'BMI指数',
    `bmr`             INT         DEFAULT NULL COMMENT '基础代谢率(kcal)',
    `daily_calorie`   INT         DEFAULT NULL COMMENT '每日推荐热量(kcal)',
    `goal`            VARCHAR(50) DEFAULT NULL COMMENT '健康目标',
    `disease_history` TEXT        DEFAULT NULL COMMENT '既往病史',
    `allergy_history` TEXT        DEFAULT NULL COMMENT '过敏史',
    `exercise_habit`  TEXT        DEFAULT NULL COMMENT '运动习惯',
    `diet_habit`      TEXT        DEFAULT NULL COMMENT '饮食习惯',
    `create_time`     DATETIME    DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`      TINYINT     DEFAULT 0 COMMENT '逻辑删除',
    `version`         INT         NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    `is_latest`       TINYINT     NOT NULL DEFAULT 1 COMMENT '1-最新 0-历史快照',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user_id_create_time` (`user_id`, `create_time` DESC) USING BTREE,
    INDEX `idx_user_latest` (`user_id`, `is_latest`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康档案表';

-- ============================================
-- AI计划表
-- ============================================
DROP TABLE IF EXISTS `ai_plan`;
CREATE TABLE `ai_plan` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`       BIGINT       NOT NULL COMMENT '用户ID',
    `plan_type`     VARCHAR(20)  NOT NULL COMMENT '计划类型 sport/diet/comprehensive',
    `plan_name`     VARCHAR(100) DEFAULT NULL COMMENT '计划名称',
    `duration_days` INT          NOT NULL COMMENT '持续天数',
    `ai_content`    TEXT         NOT NULL COMMENT 'AI生成的内容(JSON)',
    `start_date`    DATE         DEFAULT NULL COMMENT '开始日期',
    `status`        TINYINT      DEFAULT 1 COMMENT '状态 1-进行中 2-已完成',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    `version`       INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user_id_create_time` (`user_id`, `create_time` DESC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI计划表';

-- ============================================
-- AI计划每日明细表
-- ============================================
DROP TABLE IF EXISTS `ai_plan_detail`;
CREATE TABLE `ai_plan_detail` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `plan_id`       BIGINT       NOT NULL COMMENT '主计划ID',
    `day_sequence`  INT          NOT NULL COMMENT '第几天 (1~N)',
    `item_type`     VARCHAR(20)  NOT NULL COMMENT '类型: exercise/diet',
    `item_id`       BIGINT       DEFAULT NULL COMMENT '关联exercise_item/food_item的ID',
    `item_name`     VARCHAR(100) NOT NULL COMMENT '任务名称',
    `target_amount` VARCHAR(50)  NOT NULL COMMENT '目标量(如: 30分钟 / 200克)',
    `status`        TINYINT      NOT NULL DEFAULT 0 COMMENT '0-未完成 1-已完成',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_plan_day_item` (`plan_id`, `day_sequence`, `item_type`, `item_id`) USING BTREE,
    INDEX `idx_plan_day` (`plan_id`, `day_sequence`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI计划每日明细表';

-- ============================================
-- AI计划反馈表
-- ============================================
DROP TABLE IF EXISTS `ai_plan_feedback`;
CREATE TABLE `ai_plan_feedback` (
    `id`                    BIGINT       NOT NULL AUTO_INCREMENT,
    `plan_id`               BIGINT       NOT NULL COMMENT '关联AI主计划ID',
    `user_id`               BIGINT       NOT NULL COMMENT '用户ID',
    `feedback_type`         VARCHAR(50)  NOT NULL COMMENT '反馈类型：难度过高/强度不够/时间不合适/饮食不合理/其他',
    `content`               TEXT         NOT NULL COMMENT '反馈详细内容',
    `satisfaction_score`    TINYINT      DEFAULT NULL COMMENT '满意度评分(1-5分)',
    `adjustment_suggestion` TEXT         DEFAULT NULL COMMENT 'AI调整建议',
    `is_adjusted`           TINYINT      NOT NULL DEFAULT 0 COMMENT '是否已生成新计划',
    `new_plan_id`           BIGINT       DEFAULT NULL COMMENT '调整后生成的新计划ID',
    `create_time`           DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_plan_id` (`plan_id`) USING BTREE,
    INDEX `idx_user_create_time` (`user_id`, `create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI计划用户反馈表';

-- ============================================
-- 每日打卡表
-- ============================================
DROP TABLE IF EXISTS `daily_checkin`;
CREATE TABLE `daily_checkin` (
    `id`              BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`         BIGINT       NOT NULL COMMENT '用户ID',
    `plan_id`         BIGINT       DEFAULT NULL COMMENT '关联计划ID(可为空)',
    `check_date`      DATE         NOT NULL COMMENT '打卡日期',
    `exercise_status` TINYINT      DEFAULT 0 COMMENT '运动状态 0-未完成 1-部分完成 2-全部完成',
    `diet_status`     TINYINT      DEFAULT 0 COMMENT '饮食状态 0-未完成 1-部分完成 2-全部完成',
    `current_weight`  INT          DEFAULT NULL COMMENT '当前体重(kg)',
    `mood`            VARCHAR(50)  DEFAULT NULL COMMENT '心情',
    `note`            TEXT         DEFAULT NULL COMMENT '备注',
    `create_time`     DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`      TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    `version`         INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_user_id_check_date` (`user_id`, `check_date`) USING BTREE,
    INDEX `idx_user_id_check_date` (`user_id`, `check_date` DESC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日打卡表';

-- ============================================
-- 系统公告表
-- ============================================
DROP TABLE IF EXISTS `sys_announcement`;
CREATE TABLE `sys_announcement` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `title`       VARCHAR(100) NOT NULL COMMENT '标题',
    `content`     TEXT         NOT NULL COMMENT '内容',
    `admin_id`    BIGINT       NOT NULL COMMENT '发布管理员ID',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    `version`     INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告表';

-- ============================================
-- 系统通知表
-- ============================================
DROP TABLE IF EXISTS `sys_notification`;
CREATE TABLE `sys_notification` (
    `id`          BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`     BIGINT       NOT NULL COMMENT '用户ID',
    `title`       VARCHAR(100) NOT NULL COMMENT '标题',
    `content`     TEXT         NOT NULL COMMENT '内容',
    `type`        VARCHAR(20)  DEFAULT 'system' COMMENT '类型 system/plan/checkin',
    `is_read`     TINYINT      DEFAULT 0 COMMENT '已读 0-否 1-是',
    `create_time` DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `is_deleted`  TINYINT      DEFAULT 0 COMMENT '逻辑删除',
    `version`     INT          NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_user_id_is_read` (`user_id`, `is_read`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

-- ============================================
-- 运动字典库
-- ============================================
DROP TABLE IF EXISTS `exercise_item`;
CREATE TABLE `exercise_item` (
    `id`                  BIGINT        NOT NULL AUTO_INCREMENT,
    `name`                VARCHAR(100)  NOT NULL COMMENT '运动名称',
    `type`                VARCHAR(50)   NOT NULL COMMENT '类型: 有氧/无氧/拉伸',
    `calorie_coefficient` DECIMAL(5,2)  NOT NULL COMMENT '卡路里系数(kcal/kg/h)',
    `video_url`           VARCHAR(255)  DEFAULT NULL COMMENT '教学视频/GIF',
    `status`              TINYINT       NOT NULL DEFAULT 1 COMMENT '1-上架 0-下架',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='运动字典库';

-- ============================================
-- 运动执行明细流水
-- ============================================
DROP TABLE IF EXISTS `exercise_record`;
CREATE TABLE `exercise_record` (
    `id`               BIGINT   NOT NULL AUTO_INCREMENT,
    `user_id`          BIGINT   NOT NULL COMMENT '用户ID',
    `checkin_id`       BIGINT   NOT NULL COMMENT '关联当日打卡表ID',
    `item_id`          BIGINT   NOT NULL COMMENT '运动字典ID',
    `duration_minutes` INT      NOT NULL COMMENT '实际运动时长(分钟)',
    `calories_burned`  INT      NOT NULL COMMENT '实际消耗(kcal)',
    `create_time`      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_checkin_id` (`checkin_id`) USING BTREE,
    INDEX `idx_user_create_time` (`user_id`, `create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户运动执行明细流水';

-- ============================================
-- 饮食字典库
-- ============================================
DROP TABLE IF EXISTS `food_item`;
CREATE TABLE `food_item` (
    `id`               BIGINT        NOT NULL AUTO_INCREMENT,
    `name`             VARCHAR(100)  NOT NULL COMMENT '食物名称',
    `category`         VARCHAR(50)   NOT NULL COMMENT '分类：主食/蛋白质/蔬菜/水果/油脂',
    `calorie_per_100g` INT           NOT NULL COMMENT '每100克热量(kcal)',
    `protein_per_100g` DECIMAL(4,1)  DEFAULT 0.0 COMMENT '蛋白质(g)',
    `carbs_per_100g`   DECIMAL(4,1)  DEFAULT 0.0 COMMENT '碳水化合物(g)',
    `fat_per_100g`     DECIMAL(4,1)  DEFAULT 0.0 COMMENT '脂肪(g)',
    `image_url`        VARCHAR(255)  DEFAULT NULL COMMENT '食物图片',
    `sort`             INT           NOT NULL DEFAULT 0 COMMENT '排序权重',
    `status`           TINYINT       NOT NULL DEFAULT 1 COMMENT '1-上架 0-下架',
    `create_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`      DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='饮食字典库';

-- ============================================
-- 饮食执行明细流水
-- ============================================
DROP TABLE IF EXISTS `diet_record`;
CREATE TABLE `diet_record` (
    `id`                BIGINT       NOT NULL AUTO_INCREMENT,
    `user_id`           BIGINT       NOT NULL COMMENT '用户ID',
    `checkin_id`        BIGINT       NOT NULL COMMENT '关联当日打卡表ID',
    `meal_type`         VARCHAR(20)  NOT NULL COMMENT '餐次：早餐/午餐/晚餐/加餐',
    `item_id`           BIGINT       NOT NULL COMMENT '关联饮食字典ID',
    `weight_grams`      INT          NOT NULL COMMENT '实际食用重量(克)',
    `calories_consumed` INT          NOT NULL COMMENT '实际摄入热量(kcal)',
    `remark`            VARCHAR(500) DEFAULT NULL COMMENT '饮食备注',
    `create_time`       DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_checkin_id` (`checkin_id`) USING BTREE,
    INDEX `idx_user_create_time` (`user_id`, `create_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户饮食执行明细流水';

-- ============================================
-- 种子数据
-- ============================================

-- 默认管理员账号 (密码: admin123)
INSERT INTO `sys_user` (`username`, `password`, `phone`, `nickname`, `role`, `status`)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800000000', '系统管理员', 'admin', 1);

-- 运动字典种子数据
INSERT INTO `exercise_item` (`name`, `type`, `calorie_coefficient`, `status`) VALUES
('跑步', '有氧', 7.50, 1),
('快走', '有氧', 4.50, 1),
('游泳', '有氧', 8.00, 1),
('跳绳', '有氧', 10.00, 1),
('骑行', '有氧', 6.00, 1),
('瑜伽', '拉伸', 2.50, 1),
('深蹲', '无氧', 5.00, 1),
('俯卧撑', '无氧', 5.50, 1),
('仰卧起坐', '无氧', 4.00, 1),
('引体向上', '无氧', 6.00, 1);

-- 饮食字典种子数据
INSERT INTO `food_item` (`name`, `category`, `calorie_per_100g`, `protein_per_100g`, `carbs_per_100g`, `fat_per_100g`, `sort`, `status`) VALUES
('米饭', '主食', 116, 2.6, 25.9, 0.3, 1, 1),
('馒头', '主食', 223, 7.0, 44.2, 1.1, 2, 1),
('面条', '主食', 110, 3.4, 21.4, 0.8, 3, 1),
('鸡胸肉', '蛋白质', 133, 31.0, 0.0, 1.2, 4, 1),
('鸡蛋', '蛋白质', 144, 13.3, 1.5, 8.8, 5, 1),
('三文鱼', '蛋白质', 208, 20.4, 0.0, 13.4, 6, 1),
('西兰花', '蔬菜', 34, 2.8, 6.6, 0.4, 7, 1),
('菠菜', '蔬菜', 23, 2.9, 3.6, 0.4, 8, 1),
('番茄', '蔬菜', 18, 0.9, 3.9, 0.2, 9, 1),
('苹果', '水果', 52, 0.3, 14.0, 0.2, 10, 1),
('香蕉', '水果', 93, 1.1, 23.0, 0.3, 11, 1),
('核桃', '油脂', 654, 15.2, 13.7, 65.2, 12, 1),
('橄榄油', '油脂', 884, 0.0, 0.0, 100.0, 13, 1);

-- ============================================
-- 管理员操作审计日志
-- ============================================
DROP TABLE IF EXISTS `admin_audit_log`;
CREATE TABLE `admin_audit_log` (
    `id`            BIGINT       NOT NULL AUTO_INCREMENT,
    `operator_id`   BIGINT       NOT NULL COMMENT '操作人ID',
    `operator_name` VARCHAR(50)  DEFAULT NULL COMMENT '操作人名称',
    `action`        VARCHAR(50)  NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/SEND_NOTIFICATION/ADJUST_PLAN',
    `target_type`   VARCHAR(50)  DEFAULT NULL COMMENT '操作目标类型: exercise_item/food_item/notification/plan_feedback',
    `target_id`     BIGINT       DEFAULT NULL COMMENT '操作目标ID',
    `detail`        VARCHAR(500) DEFAULT NULL COMMENT '操作详情',
    `ip`            VARCHAR(50)  DEFAULT NULL COMMENT '操作IP',
    `create_time`   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_operator_time` (`operator_id`, `create_time`) USING BTREE,
    INDEX `idx_action` (`action`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='管理员操作审计日志';

-- ============================================
-- 迁移: 健康档案表增加目标体重字段
-- ============================================
ALTER TABLE `health_record`
    ADD COLUMN IF NOT EXISTS `target_weight` INT DEFAULT NULL COMMENT '目标体重(kg)'
    AFTER `weight`;

-- ============================================
-- 迁移: 用户表增加通知偏好字段
-- ============================================
ALTER TABLE `sys_user`
    ADD COLUMN IF NOT EXISTS `notification_enabled` TINYINT DEFAULT 1 COMMENT '通知开关 1=开启 0=关闭'
    AFTER `age`,
    ADD COLUMN IF NOT EXISTS `reminder_time` VARCHAR(10) DEFAULT '08:00' COMMENT '提醒时间 HH:mm'
    AFTER `notification_enabled`;