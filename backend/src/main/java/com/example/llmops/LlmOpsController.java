package com.example.llmops;

import com.example.common.Result;
import com.example.resilience.ModelRouter;
import com.example.resilience.OnlineSafetyCircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * LLMOps 运维管理 API。
 * 提供 Prompt 版本管理、模型状态、告警历史、运维面板等功能。
 */
@Tag(name = "LLMOps 运维", description = "Prompt管理、模型状态、监控告警、运维面板")
@RestController
@RequestMapping("/api/admin/llmops")
public class LlmOpsController {

    private final PromptVersionManager promptVersionManager;
    private final AlertManager alertManager;
    private final MetricsExporter metricsExporter;
    private final ModelRouter modelRouter;
    private final OnlineSafetyCircuitBreaker circuitBreaker;

    public LlmOpsController(PromptVersionManager promptVersionManager,
                             AlertManager alertManager,
                             MetricsExporter metricsExporter,
                             ModelRouter modelRouter,
                             OnlineSafetyCircuitBreaker circuitBreaker) {
        this.promptVersionManager = promptVersionManager;
        this.alertManager = alertManager;
        this.metricsExporter = metricsExporter;
        this.modelRouter = modelRouter;
        this.circuitBreaker = circuitBreaker;
    }

    // ==================== Prompt 管理 ====================

    @Operation(summary = "激活指定版本 Prompt")
    @PostMapping("/prompt/{templateKey}/activate/{version}")
    public Result<Void> activatePromptVersion(
            @PathVariable String templateKey,
            @PathVariable int version) {
        promptVersionManager.activateVersion(templateKey, version);
        return Result.success();
    }

    @Operation(summary = "回滚 Prompt 到上一个版本")
    @PostMapping("/prompt/{templateKey}/rollback")
    public Result<Void> rollbackPrompt(@PathVariable String templateKey) {
        promptVersionManager.rollback(templateKey);
        return Result.success();
    }

    @Operation(summary = "设置 A/B 测试")
    @PostMapping("/prompt/{templateKey}/ab-test")
    public Result<Void> setAbTest(
            @PathVariable String templateKey,
            @RequestParam int versionA,
            @RequestParam int versionB,
            @RequestParam int ratioA) {
        promptVersionManager.setAbTest(templateKey, versionA, versionB, ratioA);
        return Result.success();
    }

    @Operation(summary = "获取 Prompt 版本历史")
    @GetMapping("/prompt/{templateKey}/history")
    public Result<?> getVersionHistory(@PathVariable String templateKey) {
        return Result.success(promptVersionManager.getVersionHistory(templateKey));
    }

    @Operation(summary = "获取当前激活的 Prompt 版本")
    @GetMapping("/prompt/{templateKey}/active")
    public Result<Map<String, Object>> getActiveVersion(@PathVariable String templateKey) {
        return Result.success(Map.of(
                "templateKey", templateKey,
                "activeVersion", promptVersionManager.getActiveVersion(templateKey),
                "content", promptVersionManager.getActiveTemplate(templateKey)
        ));
    }

    // ==================== Prompt 灰度发布 ====================

    @Operation(summary = "启动渐进式灰度发布")
    @PostMapping("/prompt/{templateKey}/canary/start")
    public Result<Void> startCanary(
            @PathVariable String templateKey,
            @RequestParam int version,
            @RequestParam int percentage) {
        promptVersionManager.startCanary(templateKey, version, percentage);
        return Result.success();
    }

    @Operation(summary = "扩大灰度比例")
    @PostMapping("/prompt/{templateKey}/canary/increase")
    public Result<Void> increaseCanaryPercent(
            @PathVariable String templateKey,
            @RequestParam int percentage) {
        promptVersionManager.increaseCanaryPercent(templateKey, percentage);
        return Result.success();
    }

    @Operation(summary = "完成灰度发布（推广至100%）")
    @PostMapping("/prompt/{templateKey}/canary/complete")
    public Result<Void> completeCanary(@PathVariable String templateKey) {
        promptVersionManager.completeCanary(templateKey);
        return Result.success();
    }

    @Operation(summary = "取消灰度发布（回滚至稳定版本）")
    @PostMapping("/prompt/{templateKey}/canary/cancel")
    public Result<Void> cancelCanary(@PathVariable String templateKey) {
        promptVersionManager.cancelCanary(templateKey);
        return Result.success();
    }

    @Operation(summary = "获取灰度发布状态")
    @GetMapping("/prompt/{templateKey}/canary/status")
    public Result<?> getCanaryStatus(@PathVariable String templateKey) {
        return Result.success(promptVersionManager.getCanaryStatus(templateKey));
    }

    @Operation(summary = "获取所有进行中的灰度发布")
    @GetMapping("/prompt/canaries/running")
    public Result<?> getRunningCanaries() {
        return Result.success(promptVersionManager.getRunningCanaries());
    }

    // ==================== 模型管理 ====================

    @Operation(summary = "获取多模型状态")
    @GetMapping("/models/status")
    public Result<Map<String, Object>> getModelStatus() {
        return Result.success(modelRouter.getModelStatus());
    }

    // ==================== 告警 ====================

    @Operation(summary = "获取最近告警")
    @GetMapping("/alerts")
    public Result<?> getAlerts(@RequestParam(defaultValue = "20") int limit) {
        return Result.success(alertManager.getRecentAlerts(limit));
    }

    // ==================== 运维面板 ====================

    @Operation(summary = "获取安全熔断器状态")
    @GetMapping("/circuit/status")
    public Result<Map<String, Object>> getCircuitStatus() {
        return Result.success(Map.of(
                "state", circuitBreaker.getCurrentState().name(),
                "avgSafetyScore", circuitBreaker.getAverageSafetyScore(),
                "summary", circuitBreaker.getStatusSummary()
        ));
    }

    @Operation(summary = "获取运维面板摘要")
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> getDashboard() {
        return Result.success(metricsExporter.getOpsDashboard());
    }
}