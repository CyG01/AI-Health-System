package com.example.monitor;

import com.example.entity.LlmCostLog;
import com.example.mapper.LlmCostLogMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多模型成本监控器（Phase 4 升级版）。
 *
 * 从 DeepSeekCostMonitor 升级而来，支持：
 * 1. 多模型成本追踪（DeepSeek/Qwen/GLM/Moonshot/Ollama）
 * 2. 按用户 × 意图 × 模型三维度记录
 * 3. 数据库持久化（llm_cost_log 表）
 * 4. 单用户日预算告警（>1 元自动暂停）
 * 5. 全局预算监控
 */
@Component
public class MultiModelCostMonitor {

    private static final Logger log = LoggerFactory.getLogger(MultiModelCostMonitor.class);

    // 模型定价（元/百万Token）
    private static final Map<String, BigDecimal[]> MODEL_PRICES = new HashMap<>();
    static {
        // [input价格, output价格]
        MODEL_PRICES.put("deepseek-chat", new BigDecimal[]{new BigDecimal("1.0"), new BigDecimal("2.0")});
        MODEL_PRICES.put("deepseek-reasoner", new BigDecimal[]{new BigDecimal("4.0"), new BigDecimal("16.0")});
        MODEL_PRICES.put("qwen-turbo", new BigDecimal[]{new BigDecimal("0.3"), new BigDecimal("0.6")});
        MODEL_PRICES.put("qwen-max", new BigDecimal[]{new BigDecimal("2.0"), new BigDecimal("6.0")});
        MODEL_PRICES.put("glm-4", new BigDecimal[]{new BigDecimal("1.0"), new BigDecimal("1.0")});
        MODEL_PRICES.put("moonshot-v1-8k", new BigDecimal[]{new BigDecimal("1.2"), new BigDecimal("1.2")});
        MODEL_PRICES.put("local-ollama", new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
    }

    private static final BigDecimal MILLION = new BigDecimal("1000000");

    private static final String REDIS_KEY_PREFIX = "llm:cost:daily:";
    private static final String REDIS_USER_PREFIX = "llm:cost:user:";
    private static final String REDIS_PAUSED_KEY = "llm:cost:paused:";
    private static final String REDIS_ALERT_SENT_KEY = "llm:cost:alert:sent:";

    private final LlmCostLogMapper costLogMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${cost.budget.daily-per-user:1.0}")
    private BigDecimal dailyPerUserBudget;

    @Value("${cost.budget.auto-pause-threshold:1.0}")
    private BigDecimal autoPauseThreshold;

    /** 被自动暂停的用户集合（userId → 暂停时间） */
    private final ConcurrentHashMap<Long, LocalDateTime> pausedUsers = new ConcurrentHashMap<>();

    public MultiModelCostMonitor(LlmCostLogMapper costLogMapper,
                                  RedisTemplate<String, Object> redisTemplate) {
        this.costLogMapper = costLogMapper;
        this.redisTemplate = redisTemplate;
    }

    @PostConstruct
    public void init() {
        log.info("MultiModelCostMonitor 初始化 dailyPerUserBudget={} autoPauseThreshold={}",
                dailyPerUserBudget, autoPauseThreshold);
    }

    /**
     * 记录一次 LLM 调用成本（完整参数）。
     *
     * @param userId       用户ID
     * @param intent       意图分类
     * @param modelName    模型名称
     * @param modelTier    模型层级
     * @param inputTokens  输入Token数
     * @param outputTokens 输出Token数
     * @param latencyMs    调用延迟
     * @param success      是否成功
     */
    public void recordCall(Long userId, String intent, String modelName, ModelTier modelTier,
                            int inputTokens, int outputTokens, long latencyMs, boolean success) {
        // 计算成本
        BigDecimal[] prices = MODEL_PRICES.getOrDefault(modelName,
                new BigDecimal[]{new BigDecimal("1.0"), new BigDecimal("2.0")});
        BigDecimal inputCost = prices[0].multiply(
                new BigDecimal(inputTokens)).divide(MILLION, 8, RoundingMode.HALF_UP);
        BigDecimal outputCost = prices[1].multiply(
                new BigDecimal(outputTokens)).divide(MILLION, 8, RoundingMode.HALF_UP);
        BigDecimal totalCost = inputCost.add(outputCost);

        // 持久化到数据库
        try {
            LlmCostLog costLog = new LlmCostLog();
            costLog.setUserId(userId);
            costLog.setIntent(intent);
            costLog.setModelName(modelName);
            costLog.setModelTier(modelTier != null ? modelTier.name() : null);
            costLog.setInputTokens(inputTokens);
            costLog.setOutputTokens(outputTokens);
            costLog.setInputCost(inputCost);
            costLog.setOutputCost(outputCost);
            costLog.setTotalCost(totalCost);
            costLog.setLatencyMs((int) latencyMs);
            costLog.setSuccess(success ? 1 : 0);
            costLog.setCreateTime(LocalDateTime.now());
            costLogMapper.insert(costLog);
        } catch (Exception e) {
            log.error("持久化成本日志失败 userId={} intent={}", userId, intent, e);
        }

        // 更新 Redis 实时统计
        updateRedisStats(userId, intent, modelTier, inputTokens, outputTokens, totalCost);

        // 检查用户日预算
        checkUserBudget(userId);

        log.debug("LLM成本记录 userId={} intent={} model={} tier={} cost={} latencyMs={}",
                userId, intent, modelName, modelTier, totalCost, latencyMs);
    }

    /**
     * 简化版记录（兼容旧调用）。
     */
    public void recordCall(int inputTokens, int outputTokens, ModelTier tier) {
        // 全局统计（无用户上下文时的兜底）
        BigDecimal[] prices = MODEL_PRICES.getOrDefault("deepseek-chat",
                new BigDecimal[]{new BigDecimal("1.0"), new BigDecimal("2.0")});
        BigDecimal totalCost = prices[0].multiply(new BigDecimal(inputTokens))
                .add(prices[1].multiply(new BigDecimal(outputTokens)))
                .divide(MILLION, 8, RoundingMode.HALF_UP);

        String key = REDIS_KEY_PREFIX + LocalDate.now();
        redisTemplate.opsForHash().increment(key, "inputTokens", inputTokens);
        redisTemplate.opsForHash().increment(key, "outputTokens", outputTokens);
        redisTemplate.opsForHash().increment(key, "totalCost", totalCost.doubleValue());
    }

    /**
     * 更新 Redis 实时统计。
     */
    private void updateRedisStats(Long userId, String intent, ModelTier modelTier,
                                   int inputTokens, int outputTokens, BigDecimal totalCost) {
        String dateKey = REDIS_KEY_PREFIX + LocalDate.now();

        // 全局统计
        redisTemplate.opsForHash().increment(dateKey, "inputTokens", inputTokens);
        redisTemplate.opsForHash().increment(dateKey, "outputTokens", outputTokens);
        redisTemplate.opsForHash().increment(dateKey, "totalCost", totalCost.doubleValue());
        redisTemplate.opsForHash().increment(dateKey, "callCount", 1);

        // 按用户统计
        String userKey = REDIS_USER_PREFIX + LocalDate.now() + ":" + userId;
        redisTemplate.opsForHash().increment(userKey, "totalCost", totalCost.doubleValue());
        redisTemplate.opsForHash().increment(userKey, "callCount", 1);

        // 按层级统计
        if (modelTier != null) {
            redisTemplate.opsForHash().increment(dateKey + ":tier:" + modelTier.name(),
                    "totalCost", totalCost.doubleValue());
        }
    }

    /**
     * 获取用户当日总成本。
     */
    public BigDecimal getUserDailyCost(Long userId) {
        // 优先从数据库查询（精确）
        BigDecimal dbCost = costLogMapper.getUserDailyCost(userId);
        if (dbCost != null) {
            return dbCost;
        }

        // 降级从 Redis 查询
        String userKey = REDIS_USER_PREFIX + LocalDate.now() + ":" + userId;
        Object costObj = redisTemplate.opsForHash().get(userKey, "totalCost");
        return costObj != null ? new BigDecimal(costObj.toString()) : BigDecimal.ZERO;
    }

    /**
     * 获取用户当日分意图成本。
     */
    public List<Map<String, Object>> getUserDailyCostByIntent(Long userId) {
        return costLogMapper.getUserDailyCostByIntent(userId);
    }

    /**
     * 获取用户当日分模型成本。
     */
    public List<Map<String, Object>> getUserDailyCostByModel(Long userId) {
        return costLogMapper.getUserDailyCostByModel(userId);
    }

    /**
     * 获取全局当日总成本。
     */
    public BigDecimal getGlobalDailyCost() {
        BigDecimal dbCost = costLogMapper.getGlobalDailyCost();
        if (dbCost != null) {
            return dbCost;
        }
        String key = REDIS_KEY_PREFIX + LocalDate.now();
        Object costObj = redisTemplate.opsForHash().get(key, "totalCost");
        return costObj != null ? new BigDecimal(costObj.toString()) : BigDecimal.ZERO;
    }

    /**
     * 获取全局当日分层级成本。
     */
    public List<Map<String, Object>> getGlobalDailyCostByTier() {
        return costLogMapper.getGlobalDailyCostByTier();
    }

    /**
     * 获取所有 Tier 的 Redis 实时成本。
     */
    public Map<String, BigDecimal> getAllTierCosts() {
        Map<String, BigDecimal> result = new HashMap<>();
        for (ModelTier tier : ModelTier.values()) {
            String tierKey = REDIS_KEY_PREFIX + LocalDate.now() + ":tier:" + tier.name();
            Object costObj = redisTemplate.opsForHash().get(tierKey, "totalCost");
            result.put(tier.name(), costObj != null ? new BigDecimal(costObj.toString()) : BigDecimal.ZERO);
        }
        return result;
    }

    /**
     * 获取全局日预算剩余。
     */
    public BigDecimal getRemainingBudget() {
        return dailyPerUserBudget.subtract(getGlobalDailyCost());
    }

    /**
     * 检查用户日预算，超过阈值自动暂停。
     */
    private void checkUserBudget(Long userId) {
        BigDecimal userCost = getUserDailyCost(userId);

        // 超过自动暂停阈值
        if (userCost.compareTo(autoPauseThreshold) >= 0) {
            String alertKey = REDIS_ALERT_SENT_KEY + LocalDate.now() + ":" + userId;

            // 避免重复发送告警
            if (Boolean.FALSE.equals(redisTemplate.hasKey(alertKey))) {
                log.error("用户日预算超限 userId={} dailyCost={} threshold={}", userId, userCost, autoPauseThreshold);

                // 自动暂停
                pauseUser(userId);
                redisTemplate.opsForValue().set(alertKey, "1");
            }
        }

        // 预算消耗 80% 告警
        BigDecimal warnThreshold = autoPauseThreshold.multiply(new BigDecimal("0.8"));
        if (userCost.compareTo(warnThreshold) >= 0 && userCost.compareTo(autoPauseThreshold) < 0) {
            String warnKey = "llm:cost:warn:" + LocalDate.now() + ":" + userId;
            if (Boolean.FALSE.equals(redisTemplate.hasKey(warnKey))) {
                log.warn("用户日预算即将超限 userId={} dailyCost={} threshold={}", userId, userCost, autoPauseThreshold);
                redisTemplate.opsForValue().set(warnKey, "1");
            }
        }
    }

    /**
     * 暂停用户 LLM 调用。
     */
    public void pauseUser(Long userId) {
        pausedUsers.put(userId, LocalDateTime.now());
        redisTemplate.opsForValue().set(REDIS_PAUSED_KEY + userId, "1");
        log.warn("用户已暂停LLM调用 userId={} reason=日预算超限", userId);
    }

    /**
     * 恢复用户 LLM 调用。
     */
    public void resumeUser(Long userId) {
        pausedUsers.remove(userId);
        redisTemplate.delete(REDIS_PAUSED_KEY + userId);
        log.info("用户已恢复LLM调用 userId={}", userId);
    }

    /**
     * 检查用户是否被暂停。
     */
    public boolean isUserPaused(Long userId) {
        return pausedUsers.containsKey(userId) ||
                Boolean.TRUE.equals(redisTemplate.hasKey(REDIS_PAUSED_KEY + userId));
    }

    /**
     * 获取所有超预算用户。
     */
    public List<Map<String, Object>> getOverBudgetUsers() {
        return costLogMapper.getOverBudgetUsers(autoPauseThreshold);
    }

    /**
     * 每日零点重置 Redis 缓存。
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyStats() {
        // 清理 Redis 日统计（数据库保留完整记录）
        Set<String> keys = redisTemplate.keys(REDIS_KEY_PREFIX + "*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        Set<String> userKeys = redisTemplate.keys(REDIS_USER_PREFIX + "*");
        if (userKeys != null && !userKeys.isEmpty()) {
            redisTemplate.delete(userKeys);
        }
        // 清理告警标记
        Set<String> alertKeys = redisTemplate.keys(REDIS_ALERT_SENT_KEY + "*");
        if (alertKeys != null && !alertKeys.isEmpty()) {
            redisTemplate.delete(alertKeys);
        }
        log.info("LLM 成本日统计已重置");
    }
}