-- Phase 4: 计划可执行性 & 场景适配字段迁移
-- 1. ai_plan_detail 增加子阶段字段
-- 2. user_profile 增加场景字段
-- 3. ai_plan 增加场景标签字段

-- ============================================================
-- ai_plan_detail: 增加子阶段支持
-- ============================================================
ALTER TABLE ai_plan_detail
    ADD COLUMN IF NOT EXISTS sub_phase VARCHAR(50) COMMENT '子阶段名称（如 热身、核心训练、放松拉伸）',
    ADD COLUMN IF NOT EXISTS sub_phase_type VARCHAR(20) COMMENT '子阶段类型：warmup/core/cooldown',
    ADD COLUMN IF NOT EXISTS phase_order INT DEFAULT 1 COMMENT '子阶段排序',
    ADD COLUMN IF NOT EXISTS phase_duration_minutes INT DEFAULT NULL COMMENT '子阶段时长（分钟）',
    ADD COLUMN IF NOT EXISTS scenario_tag VARCHAR(20) COMMENT '场景标签：workday/weekend/travel';

-- ============================================================
-- user_profile: 增加当前场景字段
-- ============================================================
ALTER TABLE user_profile
    ADD COLUMN IF NOT EXISTS current_scenario VARCHAR(20) DEFAULT 'workday' COMMENT '当前场景：workday/weekend/travel',
    ADD COLUMN IF NOT EXISTS scenario_updated_at DATETIME COMMENT '场景更新时间';

-- ============================================================
-- ai_plan: 增加场景标签字段
-- ============================================================
ALTER TABLE ai_plan
    ADD COLUMN IF NOT EXISTS scenario_tag VARCHAR(20) COMMENT '场景标签：workday/weekend/travel';

-- ============================================================
-- ai_plan_detail: 为 phase_order 添加索引
-- ============================================================
ALTER TABLE ai_plan_detail
    ADD INDEX IF NOT EXISTS idx_phase_order (phase_order);