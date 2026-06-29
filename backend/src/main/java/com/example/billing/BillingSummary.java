package com.example.billing;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 计费摘要 — 用户当日用量与费用汇总。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingSummary {

    /** 用户ID */
    private Long userId;

    /** 订阅等级 */
    private String tier;

    /** 当日输入 token 数 */
    @Builder.Default
    private int inputTokens = 0;

    /** 当日输出 token 数 */
    @Builder.Default
    private int outputTokens = 0;

    /** 当日计划生成次数 */
    @Builder.Default
    private int planGenCount = 0;

    /** 当日食物识别次数 */
    @Builder.Default
    private int foodRecogCount = 0;

    /** 当日聊天次数 */
    @Builder.Default
    private int chatCount = 0;

    /** 当日 API 调用次数 */
    @Builder.Default
    private int apiCallCount = 0;

    /** 当日总费用（元） */
    @Builder.Default
    private BigDecimal dailyCost = BigDecimal.ZERO;

    /** 当月累计费用 */
    @Builder.Default
    private BigDecimal monthlyCost = BigDecimal.ZERO;

    /** 额度限制 */
    private UsageLimit limit;

    /** 是否已超额 */
    @Builder.Default
    private boolean exceeded = false;

    /** 当日用量百分比（0-100），用于前端额度提醒展示 */
    @Builder.Default
    private int usagePercent = 0;

    /** 额度预警级别：normal(正常) / warning(即将耗尽) / exceeded(已超额) */
    @Builder.Default
    private String quotaLevel = "normal";

    /** 本月累计Token消耗（含免费额度内和超量部分） */
    @Builder.Default
    private long monthlyTokensUsed = 0;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UsageLimit {
        /** 每日调用次数上限（免费版3次，Pro无限=-1） */
        private int dailyCallLimit;
        /** 每日计划生成上限 */
        private int dailyPlanGenLimit;
        /** 每日食物识别上限 */
        private int dailyFoodRecogLimit;
        /** 每日聊天上限 */
        private int dailyChatLimit;
        /** 月度 Token 上限（百万） */
        private int monthlyTokenLimitM;
    }
}