-- ============================================
-- AI 健康管理系统 - 数据库初始化脚本
-- 版本: 1.0.0
-- ============================================

CREATE DATABASE IF NOT EXISTS ai_health_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_unicode_ci;

USE ai_health_db;

-- ============================================
-- 系统用户表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '用户ID',
    username    VARCHAR(50)  NOT NULL COMMENT '用户名',
    password    VARCHAR(255) NOT NULL COMMENT '密码(BCrypt)',
    phone       VARCHAR(20)  NOT NULL COMMENT '手机号',
    nickname    VARCHAR(50)  DEFAULT NULL COMMENT '昵称',
    avatar      VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    gender      TINYINT      DEFAULT NULL COMMENT '性别 0-女 1-男',
    age         INT          DEFAULT NULL COMMENT '年龄',
    role        VARCHAR(20)  NOT NULL DEFAULT 'user' COMMENT '角色 user/admin',
    status      TINYINT      NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
    version     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    UNIQUE KEY uk_username (username),
    UNIQUE KEY uk_phone (phone),
    INDEX idx_status (status),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

-- ============================================
-- 健康档案表
-- ============================================
CREATE TABLE IF NOT EXISTS health_record (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '记录ID',
    user_id         BIGINT       NOT NULL COMMENT '用户ID',
    height          DECIMAL(5,1) NOT NULL COMMENT '身高(cm)',
    weight          DECIMAL(5,1) NOT NULL COMMENT '体重(kg)',
    bmi             DECIMAL(4,1) DEFAULT NULL COMMENT 'BMI指数',
    bmr             DECIMAL(7,2) DEFAULT NULL COMMENT '基础代谢率(kcal)',
    daily_calorie   DECIMAL(7,2) DEFAULT NULL COMMENT '每日推荐热量(kcal)',
    goal            VARCHAR(200) DEFAULT NULL COMMENT '健康目标',
    disease_history VARCHAR(500) DEFAULT NULL COMMENT '既往病史',
    allergy_history VARCHAR(500) DEFAULT NULL COMMENT '过敏史',
    exercise_habit  VARCHAR(500) DEFAULT NULL COMMENT '运动习惯',
    diet_habit      VARCHAR(500) DEFAULT NULL COMMENT '饮食习惯',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted      TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
    version         INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    INDEX idx_user_id (user_id),
    INDEX idx_user_created (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='健康档案表';

-- ============================================
-- AI计划表
-- ============================================
CREATE TABLE IF NOT EXISTS ai_plan (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '计划ID',
    user_id       BIGINT       NOT NULL COMMENT '用户ID',
    plan_type     VARCHAR(20)  NOT NULL COMMENT '计划类型 sport/diet',
    plan_name     VARCHAR(100) NOT NULL COMMENT '计划名称',
    duration_days INT          NOT NULL COMMENT '持续天数',
    ai_content    LONGTEXT     NOT NULL COMMENT 'AI生成的内容(JSON)',
    start_date    DATE         DEFAULT NULL COMMENT '开始日期',
    status        TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0-未开始 1-进行中 2-已完成',
    create_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted    TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
    version       INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    INDEX idx_user_id (user_id),
    INDEX idx_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI计划表';

-- ============================================
-- 每日打卡表
-- ============================================
CREATE TABLE IF NOT EXISTS daily_checkin (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '打卡ID',
    user_id          BIGINT       NOT NULL COMMENT '用户ID',
    plan_id          BIGINT       NOT NULL COMMENT '计划ID',
    check_date       DATE         NOT NULL COMMENT '打卡日期',
    exercise_status  TINYINT      NOT NULL DEFAULT 0 COMMENT '运动状态 0-未完成 1-部分完成 2-全部完成',
    diet_status      TINYINT      NOT NULL DEFAULT 0 COMMENT '饮食状态 0-未完成 1-部分完成 2-全部完成',
    current_weight   DECIMAL(5,1) DEFAULT NULL COMMENT '当前体重(kg)',
    mood             VARCHAR(20)  DEFAULT NULL COMMENT '心情',
    note             VARCHAR(200) DEFAULT NULL COMMENT '备注',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_deleted       TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
    version          INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    UNIQUE KEY uk_user_plan_date (user_id, plan_id, check_date),
    INDEX idx_user_date (user_id, check_date),
    INDEX idx_user_weight (user_id, check_date, current_weight)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='每日打卡表';

-- ============================================
-- 系统公告表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_announcement (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '公告ID',
    title       VARCHAR(200) NOT NULL COMMENT '标题',
    content     TEXT         NOT NULL COMMENT '内容',
    admin_id    BIGINT       NOT NULL COMMENT '发布管理员ID',
    status      TINYINT      NOT NULL DEFAULT 0 COMMENT '状态 0-草稿 1-已发布 2-已下线',
    publish_time DATETIME    DEFAULT NULL COMMENT '发布时间',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    is_deleted  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
    version     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    INDEX idx_status_publish (status, publish_time),
    INDEX idx_created (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告表';

-- ============================================
-- 系统通知表
-- ============================================
CREATE TABLE IF NOT EXISTS sys_notification (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '通知ID',
    user_id     BIGINT       NOT NULL COMMENT '用户ID',
    title       VARCHAR(200) NOT NULL COMMENT '标题',
    content     TEXT         NOT NULL COMMENT '内容',
    type        VARCHAR(20)  NOT NULL DEFAULT 'system' COMMENT '类型 system/plan/checkin',
    is_read     TINYINT      NOT NULL DEFAULT 0 COMMENT '已读 0-否 1-是',
    read_time   DATETIME     DEFAULT NULL COMMENT '阅读时间',
    create_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    is_deleted  TINYINT      NOT NULL DEFAULT 0 COMMENT '逻辑删除 0-否 1-是',
    version     INT          NOT NULL DEFAULT 0 COMMENT '乐观锁版本',
    INDEX idx_user_read (user_id, is_read),
    INDEX idx_user_created (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统通知表';

-- ============================================
-- 默认管理员账号 (密码: admin123)
-- 注意：上线前请修改默认密码或删除此管理员账号
INSERT INTO sys_user (username, password, phone, nickname, role, status)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '13800000000', '系统管理员', 'admin', 1)
ON DUPLICATE KEY UPDATE username = username;