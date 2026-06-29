package com.example.llmops;

import com.example.entity.PromptTemplate;
import com.example.mapper.PromptTemplateMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Prompt 版本管理器。
 * 支持 Prompt 版本回滚、A/B 测试分组切换、渐进式灰度发布。
 *
 * 灰度发布流程：
 * 1. startCanary(key, version, 5)  → 5% 流量走新版本
 * 2. increaseCanaryPercent(key, 20) → 20% 流量，每步观察足够时间
 * 3. increaseCanaryPercent(key, 50) → 50% 流量
 * 4. completeCanary(key)            → 100% 推广，结束灰度
 * 5. 如果灰度过程中安全分下降 → autoRollbackCanary(key) 自动回滚
 */
@Slf4j
@Service
public class PromptVersionManager {

    private static final String VERSION_KEY = "prompt:active_version:";
    private static final String AB_KEY = "prompt:ab:";
    private static final String CANARY_PREFIX = "prompt:canary:";
    private static final String ROLLBACK_HISTORY_KEY = "prompt:rollback_history:";

    /** 灰度阶段：running / completed / cancelled */
    private static final String CANARY_STATE_RUNNING = "running";
    private static final String CANARY_STATE_COMPLETED = "completed";
    private static final String CANARY_STATE_CANCELLED = "cancelled";

    /** 推荐的灰度放量阶梯 */
    public static final int[] CANARY_STAGES = {1, 5, 10, 20, 50, 100};

    private final PromptTemplateMapper promptTemplateMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public PromptVersionManager(PromptTemplateMapper promptTemplateMapper,
                                 RedisTemplate<String, Object> redisTemplate) {
        this.promptTemplateMapper = promptTemplateMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 激活指定版本的 Prompt（全量切换，不使用灰度）。
     */
    public void activateVersion(String templateKey, int version) {
        // 验证版本存在
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PromptTemplate>()
                .eq(PromptTemplate::getTemplateKey, templateKey)
                .eq(PromptTemplate::getVersion, version);
        PromptTemplate template = promptTemplateMapper.selectOne(wrapper);

        if (template == null) {
            throw new com.example.common.BusinessException(
                    "Prompt版本不存在: " + templateKey + " v" + version);
        }

        // 取消进行中的灰度（如果有）
        cancelRunningCanary(templateKey);

        // 记录当前版本（回滚用）
        Integer currentVersion = getActiveVersion(templateKey);
        if (currentVersion != null && currentVersion != version) {
            recordRollbackSnapshot(templateKey, currentVersion);
        }

        // 激活新版本
        template.setIsActive(true);
        template.setUpdatedAt(LocalDateTime.now());
        promptTemplateMapper.updateById(template);

        // 更新 Redis
        redisTemplate.opsForValue().set(
                VERSION_KEY + templateKey,
                String.valueOf(version),
                24, TimeUnit.HOURS);

        // 更新缓存
        String cacheKey = "prompt:template:" + templateKey + ":" + version;
        redisTemplate.opsForValue().set(cacheKey, template.getTemplateContent(), 24, TimeUnit.HOURS);

        log.info("Prompt版本已激活 key={} version={} oldVersion={}", templateKey, version, currentVersion);
    }

    // ==================== 灰度发布 ====================

    /**
     * 启动渐进式灰度发布。
     * 将指定版本的新 Prompt 以 percentage% 的流量开始灰度。
     *
     * @param templateKey 模板标识
     * @param version     灰度版本号
     * @param percentage  灰度流量百分比（1-99）
     */
    public void startCanary(String templateKey, int version, int percentage) {
        if (percentage < 1 || percentage > 99) {
            throw new com.example.common.BusinessException("灰度百分比必须在1-99之间");
        }

        // 验证版本存在
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PromptTemplate>()
                .eq(PromptTemplate::getTemplateKey, templateKey)
                .eq(PromptTemplate::getVersion, version);
        PromptTemplate template = promptTemplateMapper.selectOne(wrapper);

        if (template == null) {
            throw new com.example.common.BusinessException(
                    "Prompt版本不存在: " + templateKey + " v" + version);
        }

        // 获取当前稳定版本
        int stableVersion = getActiveVersion(templateKey);

        // 缓存灰度版本的模板内容
        String cacheKey = "prompt:template:" + templateKey + ":" + version;
        redisTemplate.opsForValue().set(cacheKey, template.getTemplateContent(), 24, TimeUnit.HOURS);

        // 存储灰度配置到 Redis
        String canaryKey = CANARY_PREFIX + templateKey;
        Map<String, String> canaryConfig = new LinkedHashMap<>();
        canaryConfig.put("canaryVersion", String.valueOf(version));
        canaryConfig.put("stableVersion", String.valueOf(stableVersion));
        canaryConfig.put("percentage", String.valueOf(percentage));
        canaryConfig.put("state", CANARY_STATE_RUNNING);
        canaryConfig.put("startTime", LocalDateTime.now().toString());
        redisTemplate.opsForHash().putAll(canaryKey, canaryConfig);
        redisTemplate.expire(canaryKey, 7, TimeUnit.DAYS);

        log.info("灰度发布已启动 key={} canaryV{} @{}% stableV{}",
                templateKey, version, percentage, stableVersion);
    }

    /**
     * 渐进式扩大灰度比例。
     *
     * @param templateKey   模板标识
     * @param newPercentage 新的灰度百分比
     */
    public void increaseCanaryPercent(String templateKey, int newPercentage) {
        if (newPercentage < 1 || newPercentage > 100) {
            throw new com.example.common.BusinessException("灰度百分比必须在1-100之间");
        }

        String canaryKey = CANARY_PREFIX + templateKey;
        Object stateObj = redisTemplate.opsForHash().get(canaryKey, "state");

        if (stateObj == null || !CANARY_STATE_RUNNING.equals(stateObj.toString())) {
            throw new com.example.common.BusinessException("无进行中的灰度发布");
        }

        Object oldPercentObj = redisTemplate.opsForHash().get(canaryKey, "percentage");
        int oldPercent = oldPercentObj != null ? Integer.parseInt(oldPercentObj.toString()) : 0;

        if (newPercentage <= oldPercent) {
            throw new com.example.common.BusinessException(
                    "新比例必须大于当前比例: " + oldPercent + "%");
        }

        redisTemplate.opsForHash().put(canaryKey, "percentage", String.valueOf(newPercentage));
        log.info("灰度比例扩大 key={} {}% → {}%", templateKey, oldPercent, newPercentage);
    }

    /**
     * 完成灰度发布：新版本推广至 100%。
     */
    public void completeCanary(String templateKey) {
        String canaryKey = CANARY_PREFIX + templateKey;
        Object stateObj = redisTemplate.opsForHash().get(canaryKey, "state");

        if (stateObj == null || !CANARY_STATE_RUNNING.equals(stateObj.toString())) {
            throw new com.example.common.BusinessException("无进行中的灰度发布");
        }

        Object versionObj = redisTemplate.opsForHash().get(canaryKey, "canaryVersion");
        int canaryVersion = Integer.parseInt(versionObj.toString());

        // 将灰度版本设为全量激活
        redisTemplate.opsForHash().put(canaryKey, "state", CANARY_STATE_COMPLETED);
        redisTemplate.opsForHash().put(canaryKey, "percentage", "100");
        redisTemplate.opsForHash().put(canaryKey, "completeTime", LocalDateTime.now().toString());

        // 更新激活版本
        redisTemplate.opsForValue().set(
                VERSION_KEY + templateKey,
                String.valueOf(canaryVersion),
                24, TimeUnit.HOURS);

        // 更新 DB 中的 isActive
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PromptTemplate>()
                .eq(PromptTemplate::getTemplateKey, templateKey)
                .eq(PromptTemplate::getVersion, canaryVersion);
        PromptTemplate template = promptTemplateMapper.selectOne(wrapper);
        if (template != null) {
            template.setIsActive(true);
            template.setUpdatedAt(LocalDateTime.now());
            promptTemplateMapper.updateById(template);
        }

        log.info("灰度发布完成 key={} V{} → 100% 已推广", templateKey, canaryVersion);
    }

    /**
     * 手动取消灰度发布（回滚到稳定版本）。
     */
    public void cancelCanary(String templateKey) {
        String canaryKey = CANARY_PREFIX + templateKey;
        Object stateObj = redisTemplate.opsForHash().get(canaryKey, "state");

        if (stateObj == null || !CANARY_STATE_RUNNING.equals(stateObj.toString())) {
            throw new com.example.common.BusinessException("无进行中的灰度发布");
        }

        Object stableVersionObj = redisTemplate.opsForHash().get(canaryKey, "stableVersion");
        int stableVersion = stableVersionObj != null
                ? Integer.parseInt(stableVersionObj.toString()) : getActiveVersion(templateKey);

        redisTemplate.opsForHash().put(canaryKey, "state", CANARY_STATE_CANCELLED);
        redisTemplate.opsForHash().put(canaryKey, "cancelTime", LocalDateTime.now().toString());

        // 恢复稳定版本
        redisTemplate.opsForValue().set(
                VERSION_KEY + templateKey,
                String.valueOf(stableVersion),
                24, TimeUnit.HOURS);

        log.warn("灰度发布已取消 key={} → 已回滚至 V{}", templateKey, stableVersion);
    }

    /**
     * 自动回滚灰度：当灰度版本的安全评分异常下降时触发。
     * 由 LLMEvaluator 在线采样检测到安全分下降后调用。
     */
    public void autoRollbackCanary(String templateKey, String reason) {
        String canaryKey = CANARY_PREFIX + templateKey;
        Object stateObj = redisTemplate.opsForHash().get(canaryKey, "state");

        if (stateObj == null || !CANARY_STATE_RUNNING.equals(stateObj.toString())) {
            log.debug("无进行中的灰度发布，跳过自动回滚 key={}", templateKey);
            return;
        }

        Object stableVersionObj = redisTemplate.opsForHash().get(canaryKey, "stableVersion");
        Object canaryVersionObj = redisTemplate.opsForHash().get(canaryKey, "canaryVersion");
        int stableVersion = stableVersionObj != null
                ? Integer.parseInt(stableVersionObj.toString()) : getActiveVersion(templateKey);

        redisTemplate.opsForHash().put(canaryKey, "state", CANARY_STATE_CANCELLED);
        redisTemplate.opsForHash().put(canaryKey, "cancelTime", LocalDateTime.now().toString());
        redisTemplate.opsForHash().put(canaryKey, "rollbackReason", reason);

        // 恢复稳定版本
        redisTemplate.opsForValue().set(
                VERSION_KEY + templateKey,
                String.valueOf(stableVersion),
                24, TimeUnit.HOURS);

        log.warn("灰度自动回滚 key={} canaryV{}→stableV{} reason={}",
                templateKey, canaryVersionObj, stableVersion, reason);
    }

    /**
     * 获取灰度发布状态。
     */
    public Map<Object, Object> getCanaryStatus(String templateKey) {
        String canaryKey = CANARY_PREFIX + templateKey;
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(canaryKey);
        if (entries.isEmpty()) {
            Map<Object, Object> empty = new LinkedHashMap<>();
            empty.put("state", "none");
            return empty;
        }
        return entries;
    }

    /**
     * 获取当前所有进行中的灰度发布。
     */
    public java.util.Set<String> getRunningCanaries() {
        java.util.Set<String> keys = redisTemplate.keys(CANARY_PREFIX + "*");
        if (keys == null) return java.util.Set.of();

        java.util.Set<String> running = new java.util.HashSet<>();
        for (String key : keys) {
            Object state = redisTemplate.opsForHash().get(key, "state");
            if (CANARY_STATE_RUNNING.equals(String.valueOf(state))) {
                running.add(key.substring(CANARY_PREFIX.length()));
            }
        }
        return running;
    }

    // ==================== 原有方法 ====================

    /**
     * 回滚到上一个版本。
     */
    public void rollback(String templateKey) {
        String rollbackKey = ROLLBACK_HISTORY_KEY + templateKey;
        Object prevVersionObj = redisTemplate.opsForList().leftPop(rollbackKey);

        if (prevVersionObj == null) {
            throw new com.example.common.BusinessException("无可用回滚版本");
        }

        int prevVersion = Integer.parseInt(prevVersionObj.toString());
        activateVersion(templateKey, prevVersion);
        log.warn("Prompt已回滚 key={} version={}", templateKey, prevVersion);
    }

    /**
     * 设置 A/B 测试分组。
     * 例如：50% 流量走 v2，50% 走 v3
     */
    public void setAbTest(String templateKey, int versionA, int versionB, int ratioA) {
        String abKey = AB_KEY + templateKey;
        String config = String.format("%d:%d:%d", versionA, versionB, ratioA);
        redisTemplate.opsForValue().set(abKey, config, 24, TimeUnit.HOURS);
        log.info("A/B测试已设置 key={} vA={} vB={} ratio={}/100", templateKey, versionA, versionB, ratioA);
    }

    /**
     * 根据用户ID哈希决定走哪个版本（优先灰度 > A/B测试 > 激活版本）。
     */
    public String getTemplateForUser(String templateKey, Long userId) {
        // 1. 优先检查灰度发布
        String canaryTemplate = getCanaryTemplate(templateKey, userId);
        if (canaryTemplate != null) {
            return canaryTemplate;
        }

        // 2. 检查 A/B 测试
        String abKey = AB_KEY + templateKey;
        Object abConfig = redisTemplate.opsForValue().get(abKey);

        if (abConfig != null) {
            String[] parts = abConfig.toString().split(":");
            int versionA = Integer.parseInt(parts[0]);
            int versionB = Integer.parseInt(parts[1]);
            int ratioA = Integer.parseInt(parts[2]);

            // 用户ID哈希决定分组
            int selectedVersion = (Math.abs(userId.hashCode()) % 100) < ratioA ? versionA : versionB;
            return getTemplateContent(templateKey, selectedVersion);
        }

        // 3. 返回当前激活版本
        return getActiveTemplate(templateKey);
    }

    /**
     * 获取当前激活版本的模板内容。
     */
    public String getActiveTemplate(String templateKey) {
        int version = getActiveVersion(templateKey);
        return getTemplateContent(templateKey, version);
    }

    /**
     * 获取当前激活版本号。
     */
    public int getActiveVersion(String templateKey) {
        Object cached = redisTemplate.opsForValue().get(VERSION_KEY + templateKey);
        if (cached != null) {
            return Integer.parseInt(cached.toString());
        }

        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PromptTemplate>()
                .eq(PromptTemplate::getTemplateKey, templateKey)
                .eq(PromptTemplate::getIsActive, true)
                .orderByDesc(PromptTemplate::getVersion)
                .last("LIMIT 1");
        PromptTemplate template = promptTemplateMapper.selectOne(wrapper);
        return template != null ? template.getVersion() : 1;
    }

    /**
     * 获取模板版本历史。
     */
    public List<PromptTemplate> getVersionHistory(String templateKey) {
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PromptTemplate>()
                .eq(PromptTemplate::getTemplateKey, templateKey)
                .orderByDesc(PromptTemplate::getVersion);
        return promptTemplateMapper.selectList(wrapper);
    }

    // ==================== 私有方法 ====================

    /**
     * 根据灰度配置获取模板内容。
     * 灰度比例通过用户ID哈希决定是否分配到灰度版本。
     */
    private String getCanaryTemplate(String templateKey, Long userId) {
        String canaryKey = CANARY_PREFIX + templateKey;
        Object stateObj = redisTemplate.opsForHash().get(canaryKey, "state");

        if (stateObj == null || !CANARY_STATE_RUNNING.equals(stateObj.toString())) {
            return null; // 无进行中的灰度
        }

        Object percentObj = redisTemplate.opsForHash().get(canaryKey, "percentage");
        Object canaryVersionObj = redisTemplate.opsForHash().get(canaryKey, "canaryVersion");

        if (percentObj == null || canaryVersionObj == null) {
            return null;
        }

        int percentage = Integer.parseInt(percentObj.toString());
        int canaryVersion = Integer.parseInt(canaryVersionObj.toString());

        // 用户ID哈希决定是否进入灰度分组
        boolean inCanaryGroup = (Math.abs(userId.hashCode()) % 100) < percentage;

        if (inCanaryGroup) {
            return getTemplateContent(templateKey, canaryVersion);
        }

        return null; // 不在灰度分组，走默认激活版本
    }

    /**
     * 取消正在运行的灰度（在 activateVersion 全量切换时使用）。
     */
    private void cancelRunningCanary(String templateKey) {
        String canaryKey = CANARY_PREFIX + templateKey;
        Object stateObj = redisTemplate.opsForHash().get(canaryKey, "state");
        if (stateObj != null && CANARY_STATE_RUNNING.equals(stateObj.toString())) {
            redisTemplate.opsForHash().put(canaryKey, "state", CANARY_STATE_CANCELLED);
            log.info("已取消进行中的灰度发布 key={}（全量版本激活覆盖）", templateKey);
        }
    }

    private String getTemplateContent(String templateKey, int version) {
        String cacheKey = "prompt:template:" + templateKey + ":" + version;
        Object cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached.toString();
        }

        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<PromptTemplate>()
                .eq(PromptTemplate::getTemplateKey, templateKey)
                .eq(PromptTemplate::getVersion, version);
        PromptTemplate template = promptTemplateMapper.selectOne(wrapper);
        return template != null ? template.getTemplateContent() : null;
    }

    private void recordRollbackSnapshot(String templateKey, int version) {
        redisTemplate.opsForList().leftPush(
                ROLLBACK_HISTORY_KEY + templateKey,
                String.valueOf(version));
        redisTemplate.expire(ROLLBACK_HISTORY_KEY + templateKey, 7, TimeUnit.DAYS);
        // 只保留最近5个快照
        redisTemplate.opsForList().trim(ROLLBACK_HISTORY_KEY + templateKey, 0, 4);
    }
}