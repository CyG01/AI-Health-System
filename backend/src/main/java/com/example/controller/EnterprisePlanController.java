package com.example.controller;

import com.example.annotation.NoRepeatSubmit;
import com.example.annotation.RateLimit;
import com.example.billing.EnterprisePlanService;
import com.example.common.Result;
import com.example.entity.Subscription;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 企业版定制化配置 — 团队人数、Token额度、价格定制。
 */
@Tag(name = "企业版定制化")
@RestController
@RequestMapping("/api/enterprise")
@RequiredArgsConstructor
public class EnterprisePlanController {

    private final EnterprisePlanService enterprisePlanService;

    @RateLimit(time = 60, count = 1)
    @NoRepeatSubmit
    @Operation(summary = "激活企业版定制化订阅（团队人数+自定义Token额度+价格）")
    @PostMapping("/activate")
    public Result<Subscription> activate(
            @RequestAttribute("userId") Long userId,
            @Parameter(description = "团队人数") @RequestParam int teamSize,
            @Parameter(description = "自定义月度Token额度（百万）") @RequestParam int customTokenQuotaM,
            @Parameter(description = "自定义价格（元/月）") @RequestParam BigDecimal customPrice,
            @Parameter(description = "订阅月数") @RequestParam(defaultValue = "12") int months,
            @Parameter(description = "订单号") @RequestParam String orderNo,
            @Parameter(description = "支付渠道") @RequestParam(defaultValue = "alipay") String channel) {
        Subscription sub = enterprisePlanService.activateEnterprisePlan(
                userId, teamSize, customTokenQuotaM, customPrice, months, orderNo, channel);
        return Result.success(sub);
    }

    @RateLimit(time = 60, count = 3)
    @NoRepeatSubmit
    @Operation(summary = "更新企业版配置（团队人数/Token额度/价格）")
    @PutMapping("/config")
    public Result<Subscription> updateConfig(
            @RequestAttribute("userId") Long userId,
            @Parameter(description = "团队人数") @RequestParam(required = false) Integer teamSize,
            @Parameter(description = "自定义月度Token额度（百万）") @RequestParam(required = false) Integer customTokenQuotaM,
            @Parameter(description = "自定义价格（元/月）") @RequestParam(required = false) BigDecimal customPrice) {
        Subscription sub = enterprisePlanService.updateEnterpriseConfig(
                userId, teamSize, customTokenQuotaM, customPrice);
        return Result.success(sub);
    }

    @Operation(summary = "查看企业版当前配置")
    @GetMapping("/config")
    public Result<Subscription> getConfig(@RequestAttribute("userId") Long userId) {
        Subscription sub = enterprisePlanService.getEnterpriseConfig(userId);
        return Result.success(sub != null ? sub : Map.of("message", "非企业版用户"));
    }
}