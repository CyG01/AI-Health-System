-- ============================================================
-- Phase 4: 高并发与成本精细化 — 数据库迁移
-- 1. chat_message 增加 user_id 分片键
-- 2. chat_message 分片表创建（chat_message_0 ~ chat_message_7）
-- 3. llm_cost_log 成本追踪表
-- 4. 成本汇总视图
-- ============================================================

-- ============================================================
-- 1. chat_message: 增加 user_id 分片键 & 索引
-- ============================================================
ALTER TABLE chat_message
    ADD COLUMN IF NOT EXISTS user_id BIGINT DEFAULT NULL COMMENT '用户ID（分片键）' AFTER id,
    ADD INDEX IF NOT EXISTS idx_user_id (user_id),
    ADD INDEX IF NOT EXISTS idx_session_id (session_id),
    ADD INDEX IF NOT EXISTS idx_user_create (user_id, create_time);

-- 回填已有数据的 user_id（通过 chat_session 关联）
UPDATE chat_message cm
    JOIN chat_session cs ON cm.session_id = cs.id
SET cm.user_id = cs.user_id
WHERE cm.user_id IS NULL;

-- ============================================================
-- 2. chat_message 分片表创建（chat_message_0 ~ chat_message_7）
-- 物理表结构完全一致，ShardingSphere-JDBC 按 user_id % 8 路由
-- ============================================================
-- 注意：以下 SQL 需在 ShardingSphere 启用前执行
-- 分片表结构与 chat_message 完全一致

DROP PROCEDURE IF EXISTS create_chat_message_shards;

DELIMITER //
CREATE PROCEDURE create_chat_message_shards()
BEGIN
    DECLARE i INT DEFAULT 0;
    WHILE i < 8 DO
        SET @sql = CONCAT(
            'CREATE TABLE IF NOT EXISTS chat_message_', i, ' (',
            '  id BIGINT AUTO_INCREMENT PRIMARY KEY,',
            '  user_id BIGINT COMMENT ''用户ID（分片键）'',',
            '  session_id BIGINT COMMENT ''会话ID'',',
            '  role VARCHAR(20) COMMENT ''角色：user/assistant'',',
            '  content TEXT COMMENT ''对话内容（加密存储）'',',
            '  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'',',
            '  INDEX idx_user_id (user_id),',
            '  INDEX idx_session_id (session_id),',
            '  INDEX idx_user_create (user_id, create_time)',
            ') ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT=''聊天消息分片表', i, '''');
        PREPARE stmt FROM @sql;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
        SET i = i + 1;
    END WHILE;
END //
DELIMITER ;

CALL create_chat_message_shards();
DROP PROCEDURE IF EXISTS create_chat_message_shards;

-- ============================================================
-- 3. llm_cost_log: LLM 调用成本追踪表（用户 × 意图 × 模型维度）
-- ============================================================
CREATE TABLE IF NOT EXISTS llm_cost_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    intent VARCHAR(50) COMMENT '意图分类：chitchat/food_recognize/plan_generate/medical_analysis等',
    model_name VARCHAR(100) COMMENT '模型名称：deepseek-chat/qwen-turbo/ollama-llama3等',
    model_tier VARCHAR(20) COMMENT '模型层级：LOW/MEDIUM/HIGH/CRITICAL',
    input_tokens INT DEFAULT 0 COMMENT '输入Token数',
    output_tokens INT DEFAULT 0 COMMENT '输出Token数',
    input_cost DECIMAL(12,6) DEFAULT 0.000000 COMMENT '输入成本（元）',
    output_cost DECIMAL(12,6) DEFAULT 0.000000 COMMENT '输出成本（元）',
    total_cost DECIMAL(12,6) DEFAULT 0.000000 COMMENT '总成本（元）',
    latency_ms INT COMMENT '调用延迟（毫秒）',
    success TINYINT(1) DEFAULT 1 COMMENT '是否成功：1=成功 0=失败',
    error_msg VARCHAR(500) COMMENT '失败原因',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_date (user_id, create_time),
    INDEX idx_user_intent (user_id, intent),
    INDEX idx_model_tier (model_tier),
    INDEX idx_create_time (create_time),
    INDEX idx_user_cost (user_id, total_cost)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='LLM调用成本追踪日志';

-- ============================================================
-- 4. 用户日成本汇总视图
-- ============================================================
CREATE OR REPLACE VIEW v_user_daily_cost AS
SELECT
    user_id,
    DATE(create_time) AS cost_date,
    COUNT(*) AS call_count,
    SUM(input_tokens) AS total_input_tokens,
    SUM(output_tokens) AS total_output_tokens,
    SUM(total_cost) AS daily_total_cost,
    COUNT(DISTINCT intent) AS intent_count,
    COUNT(DISTINCT model_name) AS model_count,
    SUM(CASE WHEN success = 0 THEN 1 ELSE 0 END) AS fail_count
FROM llm_cost_log
GROUP BY user_id, DATE(create_time);

-- ============================================================
-- 5. 数据边界声明：3 个月后下线时序表 MySQL 副本
-- 添加到期标记，方便运维定时清理
-- ============================================================
-- 为时序相关表添加 TTL 注释（标记，非自动删除）
ALTER TABLE blood_sugar COMMENT = '血糖记录 — 时序数据，MySQL副本保留3个月，到期后可清理';
ALTER TABLE sleep_record COMMENT = '睡眠记录 — 时序数据，MySQL副本保留3个月，到期后可清理';
ALTER TABLE body_measurement COMMENT = '体测记录 — 时序数据，MySQL副本保留3个月，到期后可清理';
ALTER TABLE water_record COMMENT = '饮水记录 — 时序数据，MySQL副本保留3个月，到期后可清理';
ALTER TABLE exercise_record COMMENT = '运动记录 — 时序数据，MySQL副本保留3个月，到期后可清理';
ALTER TABLE diet_record COMMENT = '饮食记录 — 时序数据，MySQL副本保留3个月，到期后可清理';

-- ============================================================
-- 6. 定时清理任务日志表（运维用）
-- ============================================================
CREATE TABLE IF NOT EXISTS data_cleanup_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    table_name VARCHAR(100) COMMENT '清理的表名',
    deleted_rows INT DEFAULT 0 COMMENT '删除行数',
    cleanup_date DATE COMMENT '清理日期',
    status VARCHAR(20) COMMENT '状态：SUCCESS/FAILED',
    error_msg VARCHAR(500) COMMENT '错误信息',
    execute_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',
    INDEX idx_cleanup_date (cleanup_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据清理日志';