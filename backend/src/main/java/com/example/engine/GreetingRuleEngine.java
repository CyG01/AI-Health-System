package com.example.engine;

import com.example.vo.DashboardGreetingVO;
import com.example.vo.DashboardGreetingVO.CardAction;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 问候卡片规则引擎。
 * 根据当前时间、用户打卡状态和计划状态生成动态问候卡片。
 * 纯规则匹配 + 模板填充，毫秒级响应，不消耗大模型 Token。
 */
public class GreetingRuleEngine {

    /**
     * 根据用户状态生成问候卡片。
     *
     * @param isCheckedIn    今日是否已打卡
     * @param streakDays     连续打卡天数
     * @param hasActivePlan  是否有活跃计划
     * @param planName       活跃计划名称
     * @param planId         活跃计划ID
     * @param completedTasks 已完成任务数
     * @param totalTasks     总任务数
     * @param exerciseCal    今日运动消耗
     * @param dietCal        今日饮食摄入
     */
    public static DashboardGreetingVO evaluate(
            boolean isCheckedIn,
            int streakDays,
            boolean hasActivePlan,
            String planName,
            Long planId,
            int completedTasks,
            int totalTasks,
            int exerciseCal,
            int dietCal
    ) {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();

        // 计算今日进度
        Integer progress = null;
        if (totalTasks > 0) {
            progress = (int) Math.round((double) completedTasks / totalTasks * 100);
        }

        // 早晨 (5:00-10:00)
        if (hour >= 5 && hour < 10) {
            return buildMorningCard(hasActivePlan, planName, planId, progress);
        }
        // 中午 (10:00-14:00)
        else if (hour >= 10 && hour < 14) {
            return buildNoonCard(dietCal, exerciseCal);
        }
        // 下午/傍晚 (14:00-20:00)
        else if (hour >= 14 && hour < 20) {
            return buildAfternoonCard(hasActivePlan, planName, planId, progress, isCheckedIn);
        }
        // 晚上 (20:00-5:00)
        else {
            if (isCheckedIn) {
                return buildCelebrationCard(streakDays, progress);
            } else {
                return buildReminderCard(hasActivePlan, planName, planId, progress);
            }
        }
    }

    private static DashboardGreetingVO buildMorningCard(boolean hasPlan, String planName, Long planId, Integer progress) {
        List<CardAction> actions = new ArrayList<>();
        String message;
        String detail;

        if (hasPlan) {
            message = "今日安排了「" + planName + "」训练，准备好了吗？";
            detail = "保持规律运动，健康生活从每一天开始。";
            actions.add(CardAction.builder().label("查看计划").url("/plan/" + planId).primary(true).build());
        } else {
            message = "新的一天开始了！还没有运动计划？";
            detail = "让AI根据你的身体状况生成个性化运动计划。";
            actions.add(CardAction.builder().label("生成计划").url("/plan/generate").primary(true).build());
        }

        return DashboardGreetingVO.builder()
                .type("morning")
                .icon("☀️")
                .greeting("早安！")
                .message(message)
                .detail(detail)
                .actions(actions)
                .progress(progress)
                .build();
    }

    private static DashboardGreetingVO buildNoonCard(int dietCal, int exerciseCal) {
        List<CardAction> actions = new ArrayList<>();
        String detail;

        if (dietCal > 0) {
            detail = "今日已摄入 " + dietCal + " kcal，注意均衡营养。";
        } else {
            detail = "午餐时间到了，推荐低脂高蛋白的健康午餐。";
        }

        actions.add(CardAction.builder().label("记录午餐").url("/food/record").primary(true).build());
        actions.add(CardAction.builder().label("AI推荐").action("open_copilot").build());

        return DashboardGreetingVO.builder()
                .type("noon")
                .icon("🍽️")
                .greeting("午饭时间到！")
                .message("别忘了记录午餐哦")
                .detail(detail)
                .actions(actions)
                .build();
    }

    private static DashboardGreetingVO buildAfternoonCard(boolean hasPlan, String planName, Long planId, Integer progress, boolean isCheckedIn) {
        List<CardAction> actions = new ArrayList<>();
        String message;
        String detail = null;

        if (isCheckedIn) {
            message = "今日已打卡，继续保持！";
            if (progress != null && progress >= 100) {
                detail = "所有任务已完成，太棒了！";
            } else {
                detail = "还有部分任务未完成，加油！";
            }
        } else if (hasPlan) {
            message = "今日「" + planName + "」训练还未开始";
            detail = "利用傍晚时间完成训练，效果更佳。";
            actions.add(CardAction.builder().label("查看计划").url("/plan/" + planId).primary(true).build());
        } else {
            message = "下午好！今天有什么运动安排吗？";
            actions.add(CardAction.builder().label("让AI安排").action("open_copilot").primary(true).build());
        }

        return DashboardGreetingVO.builder()
                .type("afternoon")
                .icon("🏃")
                .greeting("下午好！")
                .message(message)
                .detail(detail)
                .actions(actions.isEmpty() ? null : actions)
                .progress(progress)
                .build();
    }

    private static DashboardGreetingVO buildCelebrationCard(int streakDays, Integer progress) {
        String message;
        String detail;

        if (streakDays > 0) {
            message = "今日任务全部完成！连续打卡 " + streakDays + " 天，太厉害了！";
            detail = "坚持就是胜利，你的身体会感谢今天的你。";
        } else {
            message = "今日打卡完成，辛苦了！";
            detail = "每天进步一点点，健康生活在向你招手。";
        }

        return DashboardGreetingVO.builder()
                .type("celebration")
                .icon("🏆")
                .greeting("恭喜完成！")
                .message(message)
                .detail(detail)
                .progress(progress)
                .build();
    }

    private static DashboardGreetingVO buildReminderCard(boolean hasPlan, String planName, Long planId, Integer progress) {
        List<CardAction> actions = new ArrayList<>();
        String message;
        String detail;

        if (hasPlan) {
            message = "今晚的训练还未完成！";
            detail = "如果太累了，可以让AI帮你降级为5分钟拉伸。";
            actions.add(CardAction.builder().label("查看计划").url("/plan/" + planId).primary(true).build());
            actions.add(CardAction.builder().label("AI调整").action("open_copilot").build());
        } else {
            message = "今日尚未打卡，还有时间！";
            detail = "哪怕只是5分钟的运动也比不动好。";
            actions.add(CardAction.builder().label("快速开始").action("open_copilot").primary(true).build());
        }

        return DashboardGreetingVO.builder()
                .type("reminder")
                .icon("⏰")
                .greeting("温馨提醒")
                .message(message)
                .detail(detail)
                .actions(actions)
                .progress(progress)
                .build();
    }
}
