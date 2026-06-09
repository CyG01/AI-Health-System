/*
 Navicat Premium Dump SQL

 Source Server         : localhost_3306
 Source Server Type    : MySQL
 Source Server Version : 80037 (8.0.37)
 Source Host           : localhost:3306
 Source Schema         : ai_health_system

 Target Server Type    : MySQL
 Target Server Version : 80037 (8.0.37)
 File Encoding         : 65001

 Date: 09/06/2026 14:23:58
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_plan
-- ----------------------------
DROP TABLE IF EXISTS `ai_plan`;
CREATE TABLE `ai_plan`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `plan_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `plan_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `duration_days` int NOT NULL,
  `ai_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `start_date` date NULL DEFAULT NULL,
  `status` tinyint NULL DEFAULT 1,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id_create_time`(`user_id` ASC, `create_time` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_plan
-- ----------------------------

-- ----------------------------
-- Table structure for ai_plan_detail
-- ----------------------------
DROP TABLE IF EXISTS `ai_plan_detail`;
CREATE TABLE `ai_plan_detail`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL COMMENT '主计划ID',
  `day_sequence` int NOT NULL COMMENT '第几天 (1~N)',
  `item_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型: exercise/diet',
  `item_id` bigint NULL DEFAULT NULL COMMENT '关联exercise_item的ID',
  `item_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '任务名称(AI直接生成的或关联库的)',
  `target_amount` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '目标量(如: 30分钟 / 200克)',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-未完成 1-已完成',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_plan_day_item`(`plan_id` ASC, `day_sequence` ASC, `item_type` ASC, `item_id` ASC) USING BTREE,
  INDEX `idx_plan_day`(`plan_id` ASC, `day_sequence` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI计划每日明细表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_plan_detail
-- ----------------------------

-- ----------------------------
-- Table structure for ai_plan_feedback
-- ----------------------------
DROP TABLE IF EXISTS `ai_plan_feedback`;
CREATE TABLE `ai_plan_feedback`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `plan_id` bigint NOT NULL COMMENT '关联AI主计划ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `feedback_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈类型：难度过高/强度不够/时间不合适/饮食不合理/其他',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '反馈详细内容',
  `satisfaction_score` tinyint NULL DEFAULT NULL COMMENT '满意度评分(1-5分)',
  `adjustment_suggestion` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT 'AI调整建议',
  `is_adjusted` tinyint NOT NULL DEFAULT 0 COMMENT '是否已生成新计划',
  `new_plan_id` bigint NULL DEFAULT NULL COMMENT '调整后生成的新计划ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_plan_id`(`plan_id` ASC) USING BTREE,
  INDEX `idx_user_create_time`(`user_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI计划用户反馈表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_plan_feedback
-- ----------------------------

-- ----------------------------
-- Table structure for daily_checkin
-- ----------------------------
DROP TABLE IF EXISTS `daily_checkin`;
CREATE TABLE `daily_checkin`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `plan_id` bigint NULL DEFAULT NULL,
  `check_date` date NOT NULL,
  `exercise_status` tinyint NULL DEFAULT 0,
  `diet_status` tinyint NULL DEFAULT 0,
  `current_weight` int NULL DEFAULT NULL,
  `mood` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `note` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id_check_date`(`user_id` ASC, `check_date` ASC) USING BTREE,
  INDEX `idx_user_id_check_date`(`user_id` ASC, `check_date` DESC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of daily_checkin
-- ----------------------------

-- ----------------------------
-- Table structure for diet_record
-- ----------------------------
DROP TABLE IF EXISTS `diet_record`;
CREATE TABLE `diet_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `checkin_id` bigint NOT NULL COMMENT '关联当日打卡表ID',
  `meal_type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '餐次：早餐/午餐/晚餐/加餐',
  `item_id` bigint NOT NULL COMMENT '关联饮食字典ID',
  `weight_grams` int NOT NULL COMMENT '实际食用重量(克)',
  `calories_consumed` int NOT NULL COMMENT '实际摄入热量(kcal)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '饮食备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_checkin_id`(`checkin_id` ASC) USING BTREE,
  INDEX `idx_user_create_time`(`user_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户饮食执行明细流水' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of diet_record
-- ----------------------------

-- ----------------------------
-- Table structure for exercise_item
-- ----------------------------
DROP TABLE IF EXISTS `exercise_item`;
CREATE TABLE `exercise_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '运动名称',
  `type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '类型: 有氧/无氧/拉伸',
  `calorie_coefficient` decimal(5, 2) NOT NULL COMMENT '卡路里系数(kcal/kg/h)',
  `video_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '教学视频/GIF',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1-上架 0-下架',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '运动字典库' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exercise_item
-- ----------------------------

-- ----------------------------
-- Table structure for exercise_record
-- ----------------------------
DROP TABLE IF EXISTS `exercise_record`;
CREATE TABLE `exercise_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `checkin_id` bigint NOT NULL COMMENT '关联当日打卡表ID',
  `item_id` bigint NOT NULL COMMENT '运动字典ID',
  `duration_minutes` int NOT NULL COMMENT '实际运动时长(分钟)',
  `calories_burned` int NOT NULL COMMENT '实际消耗(kcal)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_checkin_id`(`checkin_id` ASC) USING BTREE,
  INDEX `idx_user_create_time`(`user_id` ASC, `create_time` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户运动执行明细流水' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of exercise_record
-- ----------------------------

-- ----------------------------
-- Table structure for food_item
-- ----------------------------
DROP TABLE IF EXISTS `food_item`;
CREATE TABLE `food_item`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '食物名称',
  `category` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '分类：主食/蛋白质/蔬菜/水果/油脂',
  `calorie_per_100g` int NOT NULL COMMENT '每100克热量(kcal)',
  `protein_per_100g` decimal(4, 1) NULL DEFAULT 0.0 COMMENT '蛋白质(g)',
  `carbs_per_100g` decimal(4, 1) NULL DEFAULT 0.0 COMMENT '碳水化合物(g)',
  `fat_per_100g` decimal(4, 1) NULL DEFAULT 0.0 COMMENT '脂肪(g)',
  `image_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '食物图片',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序权重',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1-上架 0-下架',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '饮食字典库' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of food_item
-- ----------------------------

-- ----------------------------
-- Table structure for health_record
-- ----------------------------
DROP TABLE IF EXISTS `health_record`;
CREATE TABLE `health_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `height` int NOT NULL,
  `weight` int NOT NULL,
  `bmi` decimal(4, 1) NULL DEFAULT NULL,
  `bmr` int NULL DEFAULT NULL,
  `daily_calorie` int NULL DEFAULT NULL,
  `goal` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `disease_history` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `allergy_history` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `exercise_habit` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `diet_habit` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  `is_latest` tinyint NOT NULL DEFAULT 1 COMMENT '1-最新 0-历史快照',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id_create_time`(`user_id` ASC, `create_time` DESC) USING BTREE,
  INDEX `idx_user_latest`(`user_id` ASC, `is_latest` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of health_record
-- ----------------------------

-- ----------------------------
-- Table structure for sys_announcement
-- ----------------------------
DROP TABLE IF EXISTS `sys_announcement`;
CREATE TABLE `sys_announcement`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `admin_id` bigint NOT NULL,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_announcement
-- ----------------------------

-- ----------------------------
-- Table structure for sys_notification
-- ----------------------------
DROP TABLE IF EXISTS `sys_notification`;
CREATE TABLE `sys_notification`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `title` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'system',
  `is_read` tinyint NULL DEFAULT 0,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id_is_read`(`user_id` ASC, `is_read` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_notification
-- ----------------------------

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `gender` tinyint NULL DEFAULT NULL,
  `age` int NULL DEFAULT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'user',
  `status` tinyint NULL DEFAULT 1,
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  `version` int NOT NULL DEFAULT 1 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE,
  UNIQUE INDEX `phone`(`phone` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iKX6l5eF7a9Vb0c8d7e6f5a4s3d2f1', '13800000000', '系统管理员', NULL, NULL, NULL, 'admin', 1, '2026-06-09 09:58:38', '2026-06-09 09:58:38', 0, 1);

-- ----------------------------
-- Table structure for water_record
-- ----------------------------
DROP TABLE IF EXISTS `water_record`;
CREATE TABLE `water_record` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `record_date` date NOT NULL COMMENT '记录日期',
  `amount_ml` int NOT NULL DEFAULT 0 COMMENT '饮水量(ml)',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id` ASC, `record_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '饮水记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for body_measurement
-- ----------------------------
DROP TABLE IF EXISTS `body_measurement`;
CREATE TABLE `body_measurement` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `record_date` date NOT NULL COMMENT '记录日期',
  `waist` decimal(5,1) NULL DEFAULT NULL COMMENT '腰围(cm)',
  `hip` decimal(5,1) NULL DEFAULT NULL COMMENT '臀围(cm)',
  `chest` decimal(5,1) NULL DEFAULT NULL COMMENT '胸围(cm)',
  `thigh` decimal(5,1) NULL DEFAULT NULL COMMENT '大腿围(cm)',
  `arm` decimal(5,1) NULL DEFAULT NULL COMMENT '臂围(cm)',
  `body_fat_rate` decimal(4,1) NULL DEFAULT NULL COMMENT '体脂率(%)',
  `note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_date`(`user_id` ASC, `record_date` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '身体围度测量表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for goal_milestone
-- ----------------------------
DROP TABLE IF EXISTS `goal_milestone`;
CREATE TABLE `goal_milestone` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `goal_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标类型: weight_loss/weight_gain/muscle_gain/exercise_days/checkin_days/water_target/custom',
  `goal_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标名称',
  `target_value` decimal(10,1) NOT NULL COMMENT '目标值',
  `current_value` decimal(10,1) NOT NULL DEFAULT 0.0 COMMENT '当前值',
  `unit` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '单位',
  `start_date` date NULL DEFAULT NULL COMMENT '起始日期',
  `target_date` date NULL DEFAULT NULL COMMENT '目标日期',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0-进行中 1-已完成 2-已放弃',
  `completed_date` date NULL DEFAULT NULL COMMENT '完成日期',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_status`(`user_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '目标里程碑表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_post
-- ----------------------------
DROP TABLE IF EXISTS `community_post`;
CREATE TABLE `community_post` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `user_avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户头像',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '内容',
  `images` varchar(2000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图片列表(JSON)',
  `exercise_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '运动类型',
  `exercise_duration` int NULL DEFAULT NULL COMMENT '运动时长(分钟)',
  `calories_burned` int NULL DEFAULT NULL COMMENT '消耗热量',
  `like_count` int NOT NULL DEFAULT 0 COMMENT '点赞数',
  `comment_count` int NOT NULL DEFAULT 0 COMMENT '评论数',
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1-正常 0-隐藏',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_create_time`(`create_time` DESC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '社区帖子表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_comment
-- ----------------------------
DROP TABLE IF EXISTS `community_comment`;
CREATE TABLE `community_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `user_nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户昵称',
  `user_avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户头像',
  `content` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `reply_to` bigint NULL DEFAULT 0 COMMENT '回复的评论ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_post_id`(`post_id` ASC) USING BTREE,
  INDEX `idx_create_time`(`create_time` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '社区评论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for community_like
-- ----------------------------
DROP TABLE IF EXISTS `community_like`;
CREATE TABLE `community_like` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_post_user`(`post_id` ASC, `user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '社区点赞表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- sys_notification 增加字段 (targetType, targetId)
-- ----------------------------
ALTER TABLE `sys_notification` ADD COLUMN `target_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '关联目标类型: checkin/water/exercise/sleep/goal' AFTER `type`;
ALTER TABLE `sys_notification` ADD COLUMN `target_id` bigint NULL DEFAULT NULL COMMENT '关联目标ID' AFTER `target_type`;

-- ----------------------------
-- sys_user 增加字段 (提醒偏好)
-- ----------------------------
ALTER TABLE `sys_user` ADD COLUMN `reminder_time` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '提醒时间(HH:mm)' AFTER `status`;
ALTER TABLE `sys_user` ADD COLUMN `notification_enabled` tinyint NOT NULL DEFAULT 1 COMMENT '是否开启通知' AFTER `reminder_time`;

SET FOREIGN_KEY_CHECKS = 1;
