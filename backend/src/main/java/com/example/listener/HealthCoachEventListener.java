package com.example.listener;

import com.example.agent.NutritionAgent;
import com.example.entity.DietRecord;
import com.example.entity.SysNotification;
import com.example.event.CheckinCompletedEvent;
import com.example.event.FoodRecognizedEvent;
import com.example.event.SleepLoggedEvent;
import com.example.mapper.DailyCheckinMapper;
import com.example.mapper.DietRecordMapper;
import com.example.mapper.SysNotificationMapper;
import com.example.service.MemoryService;
import com.example.service.impl.AutoPlanAdjustService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 健康教练事件监听器。
 * 打破食物识别、计划生成、打卡检查模块间的割裂，实现感知-建议闭环。
 */
@Slf4j
@Component
public class HealthCoachEventListener {

    private static final int DAILY_CALORIE_BUDGET = 2000;

    private final DietRecordMapper dietRecordMapper;
    private final DailyCheckinMapper dailyCheckinMapper;
    private final NutritionAgent nutritionAgent;
    private final MemoryService memoryService;
    private final SysNotificationMapper sysNotificationMapper;
    private final AutoPlanAdjustService autoPlanAdjustService;

    public HealthCoachEventListener(DietRecordMapper dietRecordMapper,
                                     DailyCheckinMapper dailyCheckinMapper,
                                     NutritionAgent nutritionAgent,
                                     MemoryService memoryService,
                                     SysNotificationMapper sysNotificationMapper,
                                     AutoPlanAdjustService autoPlanAdjustService) {
        this.dietRecordMapper = dietRecordMapper;
        this.dailyCheckinMapper = dailyCheckinMapper;
        this.nutritionAgent = nutritionAgent;
        this.memoryService = memoryService;
        this.sysNotificationMapper = sysNotificationMapper;
        this.autoPlanAdjustService = autoPlanAdjustService;
    }

    /**
     * 食物识别完成后：
     * 1. 自动创建 DietRecord
     * 2. 查询今日已摄入总热量
     * 3. 如超标 >120%，生成饮食调整建议
     */
    @EventListener
    @Async
    public void onFoodRecognized(FoodRecognizedEvent event) {
        log.info("收到食物识别事件 userId={} food={} cal={}", event.getUserId(),
                event.getFoodName(), event.getCalories());

        // 自动创建饮食记录
        try {
            DietRecord record = new DietRecord();
            record.setUserId(event.getUserId());
            record.setCaloriesConsumed(event.getCalories());
            record.setWeightGrams(event.getRecommendedGrams());
            record.setRemark("AI识别: " + event.getFoodName());
            record.setCreateTime(LocalDateTime.now());
            dietRecordMapper.insert(record);
            log.info("自动创建饮食记录 userId={} food={} cal={}", event.getUserId(),
                    event.getFoodName(), event.getCalories());
        } catch (Exception e) {
            log.error("创建饮食记录失败 userId={}", event.getUserId(), e);
        }

        // 查询今日摄入总量
        LocalDate today = LocalDate.now();
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DietRecord>()
                .eq(DietRecord::getUserId, event.getUserId())
                .ge(DietRecord::getCreateTime, today.atStartOfDay());
        var todayRecords = dietRecordMapper.selectList(wrapper);
        int totalCalories = todayRecords.stream()
                .mapToInt(r -> r.getCaloriesConsumed() != null ? r.getCaloriesConsumed() : 0)
                .sum();

        // 超标检查（>120%）
        if (totalCalories > DAILY_CALORIE_BUDGET * 1.2) {
            log.info("今日热量超标 userId={} totalCal={} budget={}",
                    event.getUserId(), totalCalories, DAILY_CALORIE_BUDGET);

            // 实时推送告警通知
            pushCalorieAlert(event.getUserId(), totalCalories, event.getFoodName());

            // Phase 2.1: 自动采集记忆
            memoryService.autoCollect(event.getUserId(),
                    "今天饮食热量超标，" + event.getFoodName() + "等食物共摄入" + totalCalories + "大卡",
                    "SYSTEM_RECORD");

            // 生成饮食调整建议（异步，不阻塞）
            try {
                String advice = generateDietAdvice(event.getUserId(), totalCalories);
                log.info("饮食调整建议 userId={} advice={}", event.getUserId(), advice);
            } catch (Exception e) {
                log.error("生成饮食建议失败 userId={}", event.getUserId(), e);
            }

            // 严重超标（>150%）自动调整饮食计划
            if (totalCalories > DAILY_CALORIE_BUDGET * 1.5) {
                autoPlanAdjustService.adjustForCalorieOverflow(event.getUserId(), totalCalories);
            }
        }
    }

    /**
     * 打卡完成后：
     * 如果连续3天完成率 < 30%，触发自动计划调整。
     */
    @EventListener
    @Async
    public void onCheckinCompleted(CheckinCompletedEvent event) {
        log.info("收到打卡完成事件 userId={} exerciseRate={} dietRate={}",
                event.getUserId(), event.getExerciseCompletionRate(), event.getDietCompletionRate());

        if (event.needsPlanAdjustment()) {
            log.warn("连续低完成率，自动调整计划 userId={}", event.getUserId());
            // 自动调整计划（非仅通知）
            autoPlanAdjustService.adjustForLowCompletionRate(
                    event.getUserId(),
                    event.getExerciseCompletionRate());
        }
    }

    /**
     * 睡眠记录完成后：
     * 如果睡眠不足，记录日志并可在后续推送给 AI 生成改善建议。
     */
    @EventListener
    @Async
    public void onSleepLogged(SleepLoggedEvent event) {
        log.info("收到睡眠记录事件 userId={} hours={} deepMin={}",
                event.getUserId(), event.getSleepHours(), event.getDeepSleepMinutes());

        if (event.isInsufficientSleep()) {
            log.warn("用户睡眠不足 userId={} hours={}", event.getUserId(), event.getSleepHours());
            // 实时推送睡眠不足告警
            pushSleepAlert(event.getUserId(), event.getSleepHours(), event.getDeepSleepMinutes());
            // Phase 2.1: 自动采集睡眠不足记忆
            memoryService.autoCollect(event.getUserId(),
                    "睡眠不足，仅睡了" + event.getSleepHours() + "小时，深度睡眠" + event.getDeepSleepMinutes() + "分钟",
                    "SYSTEM_RECORD");
        } else {
            // Phase 2.1: 记录良好睡眠
            memoryService.autoCollect(event.getUserId(),
                    "昨晚睡眠" + event.getSleepHours() + "小时，状态良好",
                    "SYSTEM_RECORD");
        }
    }

    /**
     * 使用营养师 Agent 生成饮食调整建议。
     * NutritionAgent 内建安全审查规则（避过敏/疾病、禁止节食等），无需额外硬规则过滤。
     */
    private String generateDietAdvice(Long userId, int totalCalories) {
        String prompt = String.format(
                "用户今日已摄入%d千卡（超过推荐量2000千卡），刚刚识别到的食物可能已计入。" +
                        "请给出3条简短的饮食调整建议，帮助用户在接下来的就餐中控制热量。回复控制在100字以内。",
                totalCalories);

        try {
            String advice = nutritionAgent.analyze(prompt);
            return advice != null && !advice.isBlank()
                    ? advice
                    : "建议减少下一餐的主食和高油脂食物摄入，增加蔬菜比例。";
        } catch (Exception e) {
            log.error("营养师Agent饮食建议生成失败 userId={}", userId, e);
            return "建议减少下一餐的主食和高油脂食物摄入，增加蔬菜比例。";
        }
    }

    /**
     * 实时推送热量超标告警。
     */
    private void pushCalorieAlert(Long userId, int totalCalories, String foodName) {
        try {
            SysNotification notification = new SysNotification();
            notification.setUserId(userId);
            notification.setTitle("热量超标提醒");
            notification.setContent(String.format(
                    "今日已摄入%d千卡（超预算120%%），最近记录：%s。建议减少后续餐食的高热量食物。",
                    totalCalories, foodName));
            notification.setType("HEALTH_ALERT");
            notification.setTargetType("diet_record");
            notification.setIsRead(0);
            sysNotificationMapper.insert(notification);
            log.info("热量超标实时告警已推送 userId={}", userId);
        } catch (Exception e) {
            log.error("推送热量告警失败 userId={}", userId, e);
        }
    }

    /**
     * 实时推送睡眠不足告警。
     */
    private void pushSleepAlert(Long userId, int sleepHours, int deepMinutes) {
        try {
            SysNotification notification = new SysNotification();
            notification.setUserId(userId);
            notification.setTitle("睡眠不足提醒");
            notification.setContent(String.format(
                    "昨晚仅睡%d小时（深度睡眠%d分钟），低于建议的6小时，适当减少今日高强度运动。",
                    sleepHours, deepMinutes));
            notification.setType("HEALTH_ALERT");
            notification.setTargetType("sleep_record");
            notification.setIsRead(0);
            sysNotificationMapper.insert(notification);
            log.info("睡眠不足实时告警已推送 userId={}", userId);
        } catch (Exception e) {
            log.error("推送睡眠告警失败 userId={}", userId, e);
        }
    }
}