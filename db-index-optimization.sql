-- ============================================
-- 数据库索引优化（生产环境）
-- ============================================

-- 1. 健康档案表：高频查询条件 user_id + create_time
CREATE INDEX idx_health_record_user_id ON health_record(user_id);
CREATE INDEX idx_health_record_user_created ON health_record(user_id, create_time);

-- 2. AI计划表：用户所有计划查询
CREATE INDEX idx_ai_plan_user_id ON ai_plan(user_id);
CREATE INDEX idx_ai_plan_user_status ON ai_plan(user_id, status);

-- 3. 每日打卡表：最高频查询组合 + 体重趋势
CREATE INDEX idx_daily_checkin_user_date ON daily_checkin(user_id, check_date);
CREATE INDEX idx_daily_checkin_user_weight ON daily_checkin(user_id, check_date, current_weight);

-- 4. 系统用户表：登录/注册高频字段
CREATE INDEX idx_sys_user_username ON sys_user(username);
CREATE INDEX idx_sys_user_phone ON sys_user(phone);

-- 5. 系统通知表：用户通知列表查询
CREATE INDEX idx_sys_notification_user_read ON sys_notification(user_id, is_read);
CREATE INDEX idx_sys_notification_user_created ON sys_notification(user_id, create_time);

-- 6. 系统公告表：按创建时间排序
CREATE INDEX idx_sys_announcement_created ON sys_announcement(create_time);
