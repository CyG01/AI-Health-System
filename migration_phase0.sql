-- =====================================================
-- Phase 0 数据库迁移：安全修复与合规加固
-- =====================================================
-- 创建数据库（如果不存在）
CREATE DATABASE IF NOT EXISTS ai_health_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
-- 使用该数据库
USE ai_health_system;
-- 0.3 Prompt 模板表
CREATE TABLE IF NOT EXISTS prompt_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    template_key VARCHAR(100) NOT NULL UNIQUE COMMENT '模板标识',
    template_name VARCHAR(200) COMMENT '模板名称',
    template_content TEXT NOT NULL COMMENT '模板内容，支持 %s/%d 等占位符',
    version INT DEFAULT 1,
    is_active TINYINT DEFAULT 1 COMMENT '是否启用',
    ab_group VARCHAR(20) COMMENT 'A/B测试分组',
    description VARCHAR(500),
    created_at DATETIME DEFAULT NOW(),
    updated_at DATETIME DEFAULT NOW() ON UPDATE NOW()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Prompt模板表';

-- 初始模板数据
INSERT INTO prompt_template (template_key, template_name, template_content) VALUES
('plan_generate', '运动计划生成',
 '用户身高: %.1f cm, 体重: %.1f kg, BMI: %.1f, 健康目标: %s, 计划持续: %d 天. 偏好: %s. 用户画像: %s. 请为该用户生成分天的个性化%s计划, 每天2-5个任务项, 格式为严格的JSON: {"days":[{"d":天数, "items":["具体任务描述1", "具体任务描述2"]}]}. 任务要具体、可执行, 包含具体数值(时长/组数/重量/食物克数等). %s 仅输出JSON.'),

('food_recognition', '食物视觉识别',
 '请识别图片中的食物，并估算每100克的热量（kcal）、蛋白质、碳水、脂肪。严格按照JSON格式输出：{"foodName":"名称","caloriePer100g":数字,"proteinPer100g":数字,"carbsPer100g":数字,"fatPer100g":数字,"category":"分类","confidence":置信度0-100}。如果无法识别，返回{"error":"无法识别图片内容"}。'),

('health_chat_system', 'AI健康顾问系统提示',
 '你是一位专业的AI健康顾问，擅长运动科学、营养学和健康管理。%s请用中文回答，回复要专业、具体、有可操作性。');

-- 0.5 运动规则表
CREATE TABLE IF NOT EXISTS exercise_rules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    goal VARCHAR(50) COMMENT '健康目标：减重/增肌/保持/康复',
    bmi_min DECIMAL(4,1),
    bmi_max DECIMAL(4,1),
    exercise_type VARCHAR(50) COMMENT '有氧/力量/柔韧/平衡',
    exercise_name VARCHAR(100),
    default_duration INT COMMENT '默认时长(分钟)',
    default_intensity VARCHAR(20) COMMENT '低/中/高',
    priority INT DEFAULT 0,
    is_active TINYINT DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='运动规则表（降级方案）';

-- 初始规则数据
INSERT INTO exercise_rules (goal, bmi_min, bmi_max, exercise_type, exercise_name, default_duration, default_intensity, priority) VALUES
('减重', 25.0, 50.0, '有氧', '快走', 40, '中', 1),
('减重', 25.0, 50.0, '有氧', '慢跑', 30, '中', 2),
('减重', 25.0, 50.0, '力量', '自重深蹲', 15, '中', 3),
('减重', 25.0, 50.0, '力量', '开合跳', 10, '中', 4),
('减重', 25.0, 50.0, '柔韧', '全身拉伸', 15, '低', 5),
('减重', 18.5, 24.9, '有氧', '慢跑', 35, '中', 1),
('减重', 18.5, 24.9, '有氧', '跳绳', 20, '中', 2),
('减重', 18.5, 24.9, '力量', '平板支撑', 10, '中', 3),
('减重', 18.5, 24.9, '柔韧', '瑜伽拉伸', 20, '低', 4),
('增肌', 18.5, 24.9, '力量', '俯卧撑', 15, '中', 1),
('增肌', 18.5, 24.9, '力量', '哑铃弯举', 15, '中', 2),
('增肌', 18.5, 24.9, '力量', '深蹲跳', 10, '高', 3),
('增肌', 25.0, 50.0, '力量', '自重深蹲', 15, '中', 1),
('增肌', 25.0, 50.0, '力量', '哑铃推举', 12, '中', 2),
('保持', 18.5, 50.0, '有氧', '快走', 30, '低', 1),
('保持', 18.5, 50.0, '柔韧', '全身拉伸', 20, '低', 2),
('保持', 18.5, 50.0, '力量', '平板支撑', 10, '低', 3),
('康复', 18.5, 50.0, '柔韧', '温和拉伸', 15, '低', 1),
('康复', 18.5, 50.0, '平衡', '单腿站立', 5, '低', 2),
('康复', 18.5, 50.0, '有氧', '慢走', 20, '低', 3);

-- 0.6 AI 调用全链路审计日志表
CREATE TABLE IF NOT EXISTS ai_call_audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    call_type VARCHAR(50) NOT NULL COMMENT '调用类型：plan_generate/food_recognize/chat/plan_adjust',
    model_name VARCHAR(100) COMMENT '使用的模型',
    prompt_version INT COMMENT 'prompt模板版本',
    request_params TEXT COMMENT '请求参数(脱敏后)',
    prompt_used TEXT COMMENT '实际使用的prompt',
    ai_raw_response MEDIUMTEXT COMMENT 'AI原始响应',
    parsed_result TEXT COMMENT '解析后结果',
    input_tokens INT DEFAULT 0,
    output_tokens INT DEFAULT 0,
    latency_ms INT COMMENT '响应耗时(毫秒)',
    success TINYINT DEFAULT 1 COMMENT '是否成功',
    error_message VARCHAR(1000),
    created_at DATETIME DEFAULT NOW(),
    INDEX idx_user_time (user_id, created_at),
    INDEX idx_call_type (call_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用全链路审计日志';