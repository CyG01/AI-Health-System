package com.example.controller;

import com.example.annotation.AdminOnly;
import com.example.common.Result;
import com.example.monitor.MultiModelCostMonitor;
import com.example.resilience.ModelRouter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * LLM 成本监控管理接口（Phase 4：成本精细化）。
 *
 * 提供：
 * - 全局/用户成本查询
 * - 用户预算暂停/恢复
 * - 超预算用户列表
 * - 模型路由状态
 */
@Tag(name = "LLM成本监控")
@RestController
@RequestMapping("/api/v1/admin/llm-cost")
public class LlmCostController {

    private final MultiModelCostMonitor costMonitor;
    private final ModelRouter modelRouter;

    public LlmCostController(MultiModelCostMonitor costMonitor, ModelRouter modelRouter) {
        this.costMonitor = costMonitor;
        this.modelRouter = modelRouter;
    }

    @AdminOnly
    @Operation(summary = "获取全局当日成本概览")
    @GetMapping("/global/daily")
    public Result<Map<String, Object>> getGlobalDailyCost() {
        BigDecimal totalCost = costMonitor.getGlobalDailyCost();
        List<Map<String, Object>> tierCosts = costMonitor.getGlobalDailyCostByTier();

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("totalCost", totalCost);
        result.put("tierCosts", tierCosts);
        result.put("date", java.time.LocalDate.now().toString());
        return Result.ok(result);
    }

    @AdminOnly
    @Operation(summary = "获取用户当日成本详情")
    @GetMapping("/user/{userId}/daily")
    public Result<Map<String, Object>> getUserDailyCost(@PathVariable Long userId) {
        BigDecimal totalCost = costMonitor.getUserDailyCost(userId);
        List<Map<String, Object>> costByIntent = costMonitor.getUserDailyCostByIntent(userId);
        List<Map<String, Object>> costByModel = costMonitor.getUserDailyCostByModel(userId);
        boolean isPaused = costMonitor.isUserPaused(userId);

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("userId", userId);
        result.put("totalCost", totalCost);
        result.put("costByIntent", costByIntent);
        result.put("costByModel", costByModel);
        result.put("isPaused", isPaused);
        result.put("date", java.time.LocalDate.now().toString());
        return Result.ok(result);
    }

    @AdminOnly
    @Operation(summary = "获取超预算用户列表")
    @GetMapping("/over-budget-users")
    public Result<List<Map<String, Object>>> getOverBudgetUsers() {
        return Result.ok(costMonitor.getOverBudgetUsers());
    }

    @AdminOnly
    @Operation(summary = "暂停用户 LLM 调用")
    @PostMapping("/user/{userId}/pause")
    public Result<String> pauseUser(@PathVariable Long userId) {
        costMonitor.pauseUser(userId);
        return Result.ok("用户 " + userId + " 已暂停LLM调用");
    }

    @AdminOnly
    @Operation(summary = "恢复用户 LLM 调用")
    @PostMapping("/user/{userId}/resume")
    public Result<String> resumeUser(@PathVariable Long userId) {
        costMonitor.resumeUser(userId);
        return Result.ok("用户 " + userId + " 已恢复LLM调用");
    }

    @AdminOnly
    @Operation(summary = "获取模型路由状态")
    @GetMapping("/model-status")
    public Result<Map<String, Object>> getModelStatus() {
        return Result.ok(modelRouter.getModelStatus());
    }

    @AdminOnly
    @Operation(summary = "获取 Tier 熔断器状态")
    @GetMapping("/tier-circuit-breakers")
    public Result<Map<String, Object>> getTierCircuitBreakerStatus() {
        return Result.ok(modelRouter.getTierCircuitBreakerStatus());
    }
}