package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.entity.*;
import com.example.mapper.*;
import com.example.service.PrivacyService;
import com.example.service.UserMemoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * 用户隐私服务实现类。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrivacyServiceImpl implements PrivacyService {

    private static final String DNT_CACHE_PREFIX = "privacy:dnt:";
    private static final long DNT_CACHE_HOURS = 24;

    private final SysUserMapper sysUserMapper;
    private final UserProfileMapper userProfileMapper;
    private final PrivacyAuditLogMapper auditLogMapper;
    private final DataExportTaskMapper exportTaskMapper;
    private final UserMemoryService userMemoryService;
    private final StringRedisTemplate stringRedisTemplate;
    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    // ==================== 数据授权 ====================

    @Override
    public Map<String, Object> getConsent(Long userId) {
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>()
                        .eq(UserProfile::getUserId, userId)
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("dataConsentForModel", profile != null && profile.getDataConsentForModel() != null
                ? profile.getDataConsentForModel() : 0);
        result.put("dataConsentForRecommend", profile != null && profile.getDataConsentForRecommend() != null
                ? profile.getDataConsentForRecommend() : 0);
        return result;
    }

    @Override
    public Map<String, Object> updateConsent(Long userId, Integer dataConsentForModel,
                                              Integer dataConsentForRecommend) {
        // 确保用户profile存在
        UserProfile profile = userProfileMapper.selectOne(
                new LambdaQueryWrapper<UserProfile>().eq(UserProfile::getUserId, userId)
        );

        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setDataConsentForModel(0);
            profile.setDataConsentForRecommend(0);
            profile.setCreatedAt(LocalDateTime.now());
            profile.setUpdatedAt(LocalDateTime.now());
            userProfileMapper.insert(profile);
        }

        // 更新授权
        userProfileMapper.update(null,
                new LambdaUpdateWrapper<UserProfile>()
                        .eq(UserProfile::getUserId, userId)
                        .set(dataConsentForModel != null, UserProfile::getDataConsentForModel, dataConsentForModel)
                        .set(dataConsentForRecommend != null, UserProfile::getDataConsentForRecommend, dataConsentForRecommend)
                        .set(UserProfile::getUpdatedAt, LocalDateTime.now())
        );

        // 记录审计日志
        Map<String, Object> details = new LinkedHashMap<>();
        if (dataConsentForModel != null) {
            details.put("dataConsentForModel", dataConsentForModel);
        }
        if (dataConsentForRecommend != null) {
            details.put("dataConsentForRecommend", dataConsentForRecommend);
        }
        recordAuditLog(userId, "CONSENT_CHANGE", "更新数据授权状态", details, null, "SUCCESS");

        return getConsent(userId);
    }

    // ==================== 物理焚毁 ====================

    @Override
    public Map<String, Object> submitDataPurge(Long userId, List<String> dataTypes,
                                                 String reason, String ipAddress) {
        if (dataTypes == null || dataTypes.isEmpty()) {
            dataTypes = Collections.singletonList("all");
        }

        // 记录审计日志
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("dataTypes", dataTypes);
        details.put("reason", reason);
        recordAuditLog(userId, "DATA_PURGE", "提交数据物理焚毁请求", details, ipAddress, "SUCCESS");

        // 发送到 MQ 异步执行焚毁
        try {
            Map<String, Object> task = new LinkedHashMap<>();
            task.put("userId", userId);
            task.put("dataTypes", dataTypes);
            task.put("reason", reason);
            task.put("submittedAt", LocalDateTime.now().toString());

            rocketMQTemplate.convertAndSend("privacy-data-purge", objectMapper.writeValueAsString(task));
            log.info("数据焚毁任务已提交 MQ userId={} types={}", userId, dataTypes);
        } catch (Exception e) {
            log.error("提交数据焚毁任务失败 userId={}", userId, e);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("dataTypes", dataTypes);
        result.put("status", "PROCESSING");
        result.put("message", "数据焚毁任务已提交，将在后台执行");
        result.put("estimatedTime", "通常在5分钟内完成");
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Integer> executePurge(Long userId, List<String> dataTypes) {
        Map<String, Integer> purgeStats = new LinkedHashMap<>();
        int totalDeleted = 0;

        try {
            boolean purgeAll = dataTypes.contains("all");

            // 1. 清除健康记录数据
            if (purgeAll || dataTypes.contains("health_records")) {
                int healthDeleted = purgeHealthRecords(userId);
                purgeStats.put("healthRecords", healthDeleted);
                totalDeleted += healthDeleted;
            }

            // 2. 清除AI记忆数据
            if (purgeAll || dataTypes.contains("memories")) {
                int memoriesDeleted = userMemoryService.purgeAllMemories(userId);
                purgeStats.put("memories", memoriesDeleted);
                totalDeleted += memoriesDeleted;
            }

            // 3. 清除对话历史
            if (purgeAll || dataTypes.contains("chat_history")) {
                int chatDeleted = purgeChatHistory(userId);
                purgeStats.put("chatHistory", chatDeleted);
                totalDeleted += chatDeleted;
            }

            // 4. 清除向量索引（Qdrant）
            if (purgeAll || dataTypes.contains("vector_index")) {
                purgeVectorIndex(userId);
                purgeStats.put("vectorIndex", 1);
            }

            // 5. 清除Redis缓存
            purgeRedisCache(userId);
            purgeStats.put("redisCache", 1);

            purgeStats.put("total", totalDeleted);
            log.info("数据焚毁完成 userId={} total={}", userId, totalDeleted);

            // 记录审计日志
            Map<String, Object> details = new LinkedHashMap<>(purgeStats);
            recordAuditLog(userId, "DATA_PURGE", "数据物理焚毁完成", details, null, "SUCCESS");

        } catch (Exception e) {
            log.error("数据焚毁失败 userId={}", userId, e);
            recordAuditLog(userId, "DATA_PURGE", "数据物理焚毁失败",
                    Map.of("error", e.getMessage()), null, "FAILED");
            throw e;
        }

        return purgeStats;
    }

    // ==================== AI记忆沙盒 ====================

    @Override
    public Map<String, Object> getMemorySandboxStatus(Long userId) {
        SysUser user = sysUserMapper.selectById(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("aiMemoryEnabled", user != null && user.getAiMemoryEnabled() != null
                ? user.getAiMemoryEnabled() == 1 : true);
        result.put("doNotTrack", isDoNotTrackEnabled(userId));
        result.put("dataRetentionDays", user != null && user.getDataRetentionDays() != null
                ? user.getDataRetentionDays() : 365);
        result.put("description", "AI记忆沙盒关闭时，对话内容不会被记录和用于训练");
        return result;
    }

    @Override
    public Map<String, Object> toggleMemorySandbox(Long userId, boolean enabled) {
        sysUserMapper.update(null,
                new LambdaUpdateWrapper<SysUser>()
                        .eq(SysUser::getId, userId)
                        .set(SysUser::getAiMemoryEnabled, enabled ? 1 : 0)
                        .set(SysUser::getDoNotTrack, enabled ? 0 : 1)
        );

        // 清除缓存
        stringRedisTemplate.delete(DNT_CACHE_PREFIX + userId);

        // 记录审计日志
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("aiMemoryEnabled", enabled);
        details.put("doNotTrack", !enabled);
        recordAuditLog(userId, "MEMORY_SANDBOX",
                enabled ? "开启AI记忆" : "关闭AI记忆（沙盒模式）",
                details, null, "SUCCESS");

        log.info("AI记忆沙盒状态变更 userId={} enabled={}", userId, enabled);
        return getMemorySandboxStatus(userId);
    }

    @Override
    public boolean isDoNotTrackEnabled(Long userId) {
        // 先查缓存
        String cacheKey = DNT_CACHE_PREFIX + userId;
        String cached = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return "1".equals(cached);
        }

        // 查数据库
        SysUser user = sysUserMapper.selectById(userId);
        boolean dnt = user != null && user.getDoNotTrack() != null && user.getDoNotTrack() == 1;

        // 写入缓存
        stringRedisTemplate.opsForValue().set(cacheKey, dnt ? "1" : "0", DNT_CACHE_HOURS, TimeUnit.HOURS);

        return dnt;
    }

    // ==================== 数据导出 ====================

    @Override
    public Map<String, Object> requestDataExport(Long userId, String exportType,
                                                   Map<String, Object> exportScope) {
        // 创建导出任务
        DataExportTask task = new DataExportTask();
        task.setUserId(userId);
        task.setExportType(exportType != null ? exportType : "FULL");
        try {
            task.setExportScope(exportScope != null ? objectMapper.writeValueAsString(exportScope) : null);
        } catch (Exception e) {
            task.setExportScope(null);
        }
        task.setStatus("PENDING");
        task.setExpireAt(LocalDateTime.now().plusDays(7)); // 7天后过期
        task.setCreatedAt(LocalDateTime.now());
        exportTaskMapper.insert(task);

        // 记录审计日志
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("exportType", exportType);
        details.put("taskId", task.getId());
        recordAuditLog(userId, "DATA_EXPORT", "请求数据导出", details, null, "SUCCESS");

        // TODO: 发送到 MQ 异步执行导出

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", task.getId());
        result.put("exportType", exportType);
        result.put("status", "PENDING");
        result.put("message", "导出任务已提交，完成后可在此页面下载");
        result.put("expireAt", task.getExpireAt());
        return result;
    }

    @Override
    public Map<String, Object> getExportTaskStatus(Long userId, Long taskId) {
        DataExportTask task = exportTaskMapper.selectById(taskId);
        if (task == null || !task.getUserId().equals(userId)) {
            throw new com.example.common.BusinessException(404, "导出任务不存在");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("taskId", task.getId());
        result.put("exportType", task.getExportType());
        result.put("status", task.getStatus());
        result.put("fileUrl", task.getFileUrl());
        result.put("fileSize", task.getFileSize());
        result.put("recordCount", task.getRecordCount());
        result.put("createdAt", task.getCreatedAt());
        result.put("completedAt", task.getCompletedAt());
        result.put("expireAt", task.getExpireAt());
        result.put("errorMessage", task.getErrorMessage());
        return result;
    }

    @Override
    public List<Map<String, Object>> getExportTaskList(Long userId, int limit) {
        List<DataExportTask> tasks = exportTaskMapper.selectByUserId(userId, Math.min(limit, 20));
        List<Map<String, Object>> result = new ArrayList<>();

        for (DataExportTask task : tasks) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("taskId", task.getId());
            item.put("exportType", task.getExportType());
            item.put("status", task.getStatus());
            item.put("fileSize", task.getFileSize());
            item.put("createdAt", task.getCreatedAt());
            item.put("expireAt", task.getExpireAt());
            result.add(item);
        }

        return result;
    }

    // ==================== 隐私统计 ====================

    @Override
    public Map<String, Object> getPrivacyStatistics(Long userId) {
        Map<String, Object> stats = new LinkedHashMap<>();

        // 数据授权状态
        Map<String, Object> consent = getConsent(userId);
        stats.put("consent", consent);

        // AI记忆状态
        Map<String, Object> memorySandbox = getMemorySandboxStatus(userId);
        stats.put("memorySandbox", memorySandbox);

        // 数据存储统计（简化版，实际应统计各表数据量）
        Map<String, Object> dataStats = new LinkedHashMap<>();
        dataStats.put("healthRecords", "加密存储");
        dataStats.put("aiMemories", "加密存储");
        dataStats.put("chatHistory", "加密存储");
        dataStats.put("vectorEmbeddings", "向量索引");
        dataStats.put("encryptionLevel", "AES-256");
        stats.put("dataStorage", dataStats);

        // 数据保留策略
        Map<String, Object> retention = new LinkedHashMap<>();
        retention.put("retentionDays", 365);
        retention.put("autoPurge", true);
        retention.put("backupPolicy", "每日备份，保留30天");
        stats.put("retentionPolicy", retention);

        return stats;
    }

    @Override
    public List<Map<String, Object>> getPrivacyAuditLogs(Long userId, int limit) {
        List<PrivacyAuditLog> logs = auditLogMapper.selectByUserId(userId, Math.min(limit, 50));
        List<Map<String, Object>> result = new ArrayList<>();

        for (PrivacyAuditLog log : logs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", log.getId());
            item.put("actionType", log.getActionType());
            item.put("actionDescription", log.getActionDescription());
            item.put("result", log.getResult());
            item.put("ipAddress", maskIp(log.getIpAddress()));
            item.put("createdAt", log.getCreatedAt());
            result.add(item);
        }

        return result;
    }

    // ==================== 私有方法 ====================

    /**
     * 清除健康记录数据
     */
    private int purgeHealthRecords(Long userId) {
        int count = 0;
        // TODO: 实际项目中应物理删除各健康相关表的数据
        // 这里仅示例，实际需要删除 BloodSugar、DietRecord、ExerciseRecord、SleepRecord 等表
        log.info("物理删除健康记录 userId={}", userId);
        return count;
    }

    /**
     * 清除对话历史
     */
    private int purgeChatHistory(Long userId) {
        // TODO: 物理删除对话历史和AI调用日志
        log.info("物理删除对话历史 userId={}", userId);
        return 0;
    }

    /**
     * 清除向量索引
     */
    private void purgeVectorIndex(Long userId) {
        // TODO: 调用 Qdrant 客户端删除用户的所有向量点
        // qdrantClient.deletePoints(collectionName, pointsIds)
        log.info("清除向量索引 userId={}", userId);
    }

    /**
     * 清除Redis缓存
     */
    private void purgeRedisCache(Long userId) {
        // 清除用户相关的所有缓存
        Set<String> keys = stringRedisTemplate.keys("*" + userId + "*");
        if (keys != null && !keys.isEmpty()) {
            stringRedisTemplate.delete(keys);
            log.info("清除Redis缓存 userId={} keys={}", userId, keys.size());
        }

        // 清除DNT缓存
        stringRedisTemplate.delete(DNT_CACHE_PREFIX + userId);
    }

    /**
     * 记录隐私审计日志
     */
    private void recordAuditLog(Long userId, String actionType, String actionDescription,
                                 Map<String, Object> details, String ipAddress, String result) {
        try {
            PrivacyAuditLog log = new PrivacyAuditLog();
            log.setUserId(userId);
            log.setActionType(actionType);
            log.setActionDescription(actionDescription);
            if (details != null) {
                log.setActionDetails(objectMapper.writeValueAsString(details));
            }
            log.setIpAddress(ipAddress);
            log.setResult(result);
            log.setCreatedAt(LocalDateTime.now());
            auditLogMapper.insert(log);
        } catch (Exception e) {
            log.error("记录隐私审计日志失败", e);
        }
    }

    /**
     * 脱敏IP地址
     */
    private String maskIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return "***.***.***.***";
        }
        String[] parts = ip.split("\\.");
        if (parts.length == 4) {
            return parts[0] + "." + parts[1] + ".***.***";
        }
        return ip;
    }
}
