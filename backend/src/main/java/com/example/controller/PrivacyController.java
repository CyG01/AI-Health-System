package com.example.controller;

import com.example.common.Result;
import com.example.service.PrivacyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 用户隐私控制器。
 *
 * 提供数据授权、物理焚毁、AI记忆沙盒、数据导出等隐私相关功能：
 * - GET  /api/v1/privacy/consent        查询授权状态
 * - PUT  /api/v1/privacy/consent        更新授权状态
 * - POST /api/v1/privacy/purge          提交数据物理焚毁
 * - GET  /api/v1/privacy/memory-sandbox 获取AI记忆沙盒状态
 * - PUT  /api/v1/privacy/memory-sandbox 切换AI记忆沙盒开关
 * - POST /api/v1/privacy/export         请求数据导出
 * - GET  /api/v1/privacy/statistics     获取隐私数据统计
 * - GET  /api/v1/privacy/audit-logs     获取隐私操作审计日志
 */
@Slf4j
@Tag(name = "隐私管理", description = "数据授权、物理焚毁、AI记忆沙盒、数据导出等隐私功能")
@RestController
@RequestMapping("/api/v1/privacy")
@RequiredArgsConstructor
public class PrivacyController {

    private final PrivacyService privacyService;

    // ==================== 数据授权 ====================

    @Operation(summary = "查询用户数据授权状态")
    @GetMapping("/consent")
    public Result<Map<String, Object>> getConsent(@RequestAttribute("userId") Long userId) {
        return Result.success(privacyService.getConsent(userId));
    }

    @Operation(summary = "更新用户数据授权状态")
    @PutMapping("/consent")
    public Result<Map<String, Object>> updateConsent(
            @RequestAttribute("userId") Long userId,
            @RequestBody ConsentUpdateRequest request) {
        return Result.success(privacyService.updateConsent(
                userId, request.getDataConsentForModel(), request.getDataConsentForRecommend()));
    }

    // ==================== 物理焚毁 ====================

    @Operation(summary = "提交数据物理焚毁请求")
    @PostMapping("/purge")
    public Result<Map<String, Object>> submitDataPurge(
            @RequestAttribute("userId") Long userId,
            @RequestBody PurgeRequest request,
            HttpServletRequest httpRequest) {
        String ipAddress = getClientIp(httpRequest);
        return Result.success(privacyService.submitDataPurge(
                userId, request.getDataTypes(), request.getReason(), ipAddress));
    }

    // ==================== AI记忆沙盒 ====================

    @Operation(summary = "获取AI记忆沙盒状态")
    @GetMapping("/memory-sandbox")
    public Result<Map<String, Object>> getMemorySandboxStatus(@RequestAttribute("userId") Long userId) {
        return Result.success(privacyService.getMemorySandboxStatus(userId));
    }

    @Operation(summary = "切换AI记忆沙盒开关")
    @PutMapping("/memory-sandbox")
    public Result<Map<String, Object>> toggleMemorySandbox(
            @RequestAttribute("userId") Long userId,
            @RequestParam boolean enabled) {
        return Result.success(privacyService.toggleMemorySandbox(userId, enabled));
    }

    // ==================== 数据导出 ====================

    @Operation(summary = "请求数据导出")
    @PostMapping("/export")
    public Result<Map<String, Object>> requestDataExport(
            @RequestAttribute("userId") Long userId,
            @RequestBody ExportRequest request) {
        return Result.success(privacyService.requestDataExport(
                userId, request.getExportType(), request.getExportScope()));
    }

    @Operation(summary = "获取导出任务状态")
    @GetMapping("/export/{taskId}")
    public Result<Map<String, Object>> getExportTaskStatus(
            @RequestAttribute("userId") Long userId,
            @PathVariable Long taskId) {
        return Result.success(privacyService.getExportTaskStatus(userId, taskId));
    }

    @Operation(summary = "获取导出任务列表")
    @GetMapping("/export/list")
    public Result<List<Map<String, Object>>> getExportTaskList(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "10") int limit) {
        return Result.success(privacyService.getExportTaskList(userId, limit));
    }

    // ==================== 隐私统计与审计 ====================

    @Operation(summary = "获取隐私数据统计（用于前端可视化）")
    @GetMapping("/statistics")
    public Result<Map<String, Object>> getPrivacyStatistics(@RequestAttribute("userId") Long userId) {
        return Result.success(privacyService.getPrivacyStatistics(userId));
    }

    @Operation(summary = "获取隐私操作审计日志")
    @GetMapping("/audit-logs")
    public Result<List<Map<String, Object>>> getPrivacyAuditLogs(
            @RequestAttribute("userId") Long userId,
            @RequestParam(defaultValue = "20") int limit) {
        return Result.success(privacyService.getPrivacyAuditLogs(userId, limit));
    }

    // ==================== 内部方法 ====================

    /**
     * 获取客户端IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个IP时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    // ==================== 请求 DTO ====================

    /**
     * 授权更新请求 DTO。
     */
    public static class ConsentUpdateRequest {
        /** 数据用于模型训练授权 0=未授权 1=已授权 */
        private Integer dataConsentForModel;
        /** 数据用于个性化推荐授权 0=未授权 1=已授权 */
        private Integer dataConsentForRecommend;

        public Integer getDataConsentForModel() { return dataConsentForModel; }
        public void setDataConsentForModel(Integer dataConsentForModel) { this.dataConsentForModel = dataConsentForModel; }
        public Integer getDataConsentForRecommend() { return dataConsentForRecommend; }
        public void setDataConsentForRecommend(Integer dataConsentForRecommend) { this.dataConsentForRecommend = dataConsentForRecommend; }
    }

    /**
     * 数据焚毁请求 DTO。
     */
    public static class PurgeRequest {
        /** 要焚毁的数据类型列表 */
        private List<String> dataTypes;
        /** 焚毁原因 */
        private String reason;

        public List<String> getDataTypes() { return dataTypes; }
        public void setDataTypes(List<String> dataTypes) { this.dataTypes = dataTypes; }
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
    }

    /**
     * 数据导出请求 DTO。
     */
    public static class ExportRequest {
        /** 导出类型：FULL/HEALTH/MEMORY/ACTIVITY */
        private String exportType;
        /** 导出范围 */
        private Map<String, Object> exportScope;

        public String getExportType() { return exportType; }
        public void setExportType(String exportType) { this.exportType = exportType; }
        public Map<String, Object> getExportScope() { return exportScope; }
        public void setExportScope(Map<String, Object> exportScope) { this.exportScope = exportScope; }
    }
}
