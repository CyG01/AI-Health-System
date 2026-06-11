package com.example.controller;

import com.example.billing.BillingService;
import com.example.billing.BillingSummary;
import com.example.billing.SubscriptionService;
import com.example.entity.Subscription;
import com.example.entity.UserUsage;
import com.example.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 计费与订阅 — 用户端账单/消费明细查询。
 */
@Tag(name = "计费与消费明细")
@RestController
@RequestMapping("/api/billing")
public class BillingController {

    @Autowired
    private BillingService billingService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Operation(summary = "当日用量概览（Token消耗、API调用次数、费用）")
    @GetMapping("/summary")
    public Result<BillingSummary> dailySummary(@RequestAttribute("userId") Long userId) {
        return Result.success(billingService.getDailySummary(userId));
    }

    @Operation(summary = "月度用量汇总（Token总量、累计费用、套餐限额）")
    @GetMapping("/monthly")
    public Result<Map<String, Object>> monthlySummary(@RequestAttribute("userId") Long userId) {
        return Result.success(billingService.getMonthlyUsageSummary(userId));
    }

    @Operation(summary = "消费明细历史（近N天每日用量，默认30天）")
    @GetMapping("/history")
    public Result<List<UserUsage>> history(
            @RequestAttribute("userId") Long userId,
            @Parameter(description = "查询天数，默认30") @RequestParam(defaultValue = "30") int days) {
        return Result.success(billingService.getUsageHistory(userId, days));
    }

    @Operation(summary = "当前订阅信息（等级、到期时间、自动续费）")
    @GetMapping("/subscription")
    public Result<Subscription> subscription(@RequestAttribute("userId") Long userId) {
        Subscription sub = subscriptionService.getActiveSubscription(userId);
        if (sub == null) {
            sub = new Subscription();
            sub.setUserId(userId);
            sub.setTier("free");
            sub.setStatus("active");
        }
        return Result.success(sub);
    }

    @Operation(summary = "额度预警信息（剩余额度、预警级别、建议操作）")
    @GetMapping("/quota-warning")
    public Result<Map<String, Object>> quotaWarning(@RequestAttribute("userId") Long userId) {
        return Result.success(billingService.getQuotaWarning(userId));
    }
}