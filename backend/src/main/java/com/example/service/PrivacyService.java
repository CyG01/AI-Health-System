package com.example.service;

import java.util.List;
import java.util.Map;

/**
 * 用户隐私服务接口。
 * 包含数据授权、物理焚毁、AI记忆沙盒、数据导出等隐私相关功能
 */
public interface PrivacyService {

    /**
     * 查询用户数据授权状态。
     *
     * @param userId 用户ID
     * @return 包含 userId、dataConsentForModel、dataConsentForRecommend 的映射
     */
    Map<String, Object> getConsent(Long userId);

    /**
     * 更新用户数据授权状态。
     *
     * @param userId                  用户ID
     * @param dataConsentForModel     数据用于模型训练授权 0=未授权 1=已授权，null 表示不更新
     * @param dataConsentForRecommend 数据用于个性化推荐授权 0=未授权 1=已授权，null 表示不更新
     * @return 更新后的授权状态映射
     */
    Map<String, Object> updateConsent(Long userId, Integer dataConsentForModel, Integer dataConsentForRecommend);

    // ==================== 物理焚毁 ====================

    /**
     * 提交数据物理焚毁任务。
     * 不仅删除MySQL数据，还会清除向量索引和缓存。
     *
     * @param userId     用户ID
     * @param dataTypes  要焚毁的数据类型（如：health_records, memories, chat_history, all）
     * @param reason     焚毁原因
     * @param ipAddress  操作IP
     * @return 任务信息
     */
    Map<String, Object> submitDataPurge(Long userId, List<String> dataTypes, String reason, String ipAddress);

    /**
     * 执行物理焚毁（异步调用）。
     *
     * @param userId    用户ID
     * @param dataTypes 数据类型列表
     * @return 焚毁结果统计
     */
    Map<String, Integer> executePurge(Long userId, List<String> dataTypes);

    // ==================== AI记忆沙盒 ====================

    /**
     * 获取AI记忆沙盒状态。
     *
     * @param userId 用户ID
     * @return 沙盒状态信息
     */
    Map<String, Object> getMemorySandboxStatus(Long userId);

    /**
     * 切换AI记忆沙盒开关。
     * 关闭时，对话不写入AiCallAuditLog，不更新用户记忆。
     *
     * @param userId   用户ID
     * @param enabled  是否开启记忆
     * @return 更新后的状态
     */
    Map<String, Object> toggleMemorySandbox(Long userId, boolean enabled);

    /**
     * 检查当前请求是否启用了do-not-track模式。
     *
     * @param userId 用户ID
     * @return 是否不追踪
     */
    boolean isDoNotTrackEnabled(Long userId);

    // ==================== 数据导出 ====================

    /**
     * 提交数据导出请求。
     *
     * @param userId     用户ID
     * @param exportType 导出类型：FULL/HEALTH/MEMORY/ACTIVITY
     * @param exportScope 导出范围（时间范围等）
     * @return 导出任务信息
     */
    Map<String, Object> requestDataExport(Long userId, String exportType, Map<String, Object> exportScope);

    /**
     * 获取导出任务状态。
     *
     * @param userId   用户ID
     * @param taskId   任务ID
     * @return 任务状态
     */
    Map<String, Object> getExportTaskStatus(Long userId, Long taskId);

    /**
     * 获取用户导出任务列表。
     *
     * @param userId 用户ID
     * @param limit  数量
     * @return 任务列表
     */
    List<Map<String, Object>> getExportTaskList(Long userId, int limit);

    // ==================== 隐私统计 ====================

    /**
     * 获取用户隐私数据统计。
     * 用于前端隐私合规可视化展示。
     *
     * @param userId 用户ID
     * @return 统计信息
     */
    Map<String, Object> getPrivacyStatistics(Long userId);

    /**
     * 获取隐私操作审计日志。
     *
     * @param userId 用户ID
     * @param limit  数量
     * @return 审计日志列表
     */
    List<Map<String, Object>> getPrivacyAuditLogs(Long userId, int limit);
}
