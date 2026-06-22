package com.example.monitor;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class DeepSeekCostMonitor {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekCostMonitor.class);

    private static final String COST_KEY_PREFIX = "deepseek:cost:daily:";
    private static final String REDIS_USER_COST_PREFIX = "deepseek:cost:user:";

    private static final BigDecimal INPUT_PRICE_PER_M = new BigDecimal("1");
    private static final BigDecimal OUTPUT_PRICE_PER_M = new BigDecimal("2");
    private static final BigDecimal DAILY_BUDGET = new BigDecimal("10.00");

    @Value("${cost.budget.daily-per-user:1.0}")
    private BigDecimal dailyPerUserBudget;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private String currentKey;

    @PostConstruct
    public void init() {
        currentKey = buildKey();
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyCost() {
        redisTemplate.delete(buildYesterdayKey());
        // 清理用户级成本key
        String yesterdayUserPrefix = REDIS_USER_COST_PREFIX + LocalDate.now().minusDays(1) + ":";
        Set<String> userKeys = redisTemplate.keys(yesterdayUserPrefix + "*");
        if (userKeys != null && !userKeys.isEmpty()) {
            redisTemplate.delete(userKeys);
        }
        currentKey = buildKey();
        log.info("DeepSeek每日消耗统计已重置");
    }

    public void recordCall(int inputTokens, int outputTokens) {
        recordCall(inputTokens, outputTokens, null, null);
    }

    /**
     * 记录调用并同时追踪用户级成本。
     */
    public void recordCall(int inputTokens, int outputTokens, Long userId) {
        recordCall(inputTokens, outputTokens, null, userId);
    }

    /**
     * 按模型层级记录调用消耗。
     */
    public void recordCall(int inputTokens, int outputTokens, ModelTier tier) {
        recordCall(inputTokens, outputTokens, tier, null);
    }

    /**
     * 按模型层级记录调用消耗，同时追踪用户级成本。
     */
    public void recordCall(int inputTokens, int outputTokens, ModelTier tier, Long userId) {
        BigDecimal inputCost = INPUT_PRICE_PER_M.multiply(
                new BigDecimal(inputTokens).divide(new BigDecimal("1000000"), 6, RoundingMode.HALF_UP));
        BigDecimal outputCost = OUTPUT_PRICE_PER_M.multiply(
                new BigDecimal(outputTokens).divide(new BigDecimal("1000000"), 6, RoundingMode.HALF_UP));
        BigDecimal callCost = inputCost.add(outputCost);

        String inputField = "inputTokens";
        String outputField = "outputTokens";
        String costField = "totalCost";

        redisTemplate.opsForHash().increment(currentKey, inputField, inputTokens);
        redisTemplate.opsForHash().increment(currentKey, outputField, outputTokens);
        redisTemplate.opsForHash().increment(currentKey, costField, callCost.doubleValue());

        // 按 Tier 分级记录
        if (tier != null) {
            String tierKey = currentKey + ":tier:" + tier.name();
            redisTemplate.opsForHash().increment(tierKey, costField, callCost.doubleValue());
        }

        // 按用户记录成本
        if (userId != null) {
            String userKey = REDIS_USER_COST_PREFIX + LocalDate.now() + ":" + userId;
            redisTemplate.opsForHash().increment(userKey, costField, callCost.doubleValue());
            redisTemplate.opsForHash().increment(userKey, "callCount", 1);
        }

        BigDecimal totalCost = getCurrentDailyCost();
        log.info("DeepSeek API调用 cost={} tier={} userId={} totalDailyCost={}", callCost, tier, userId, totalCost);

        // 按模型层级分别检查预算告警
        checkBudgetAlert(totalCost);
    }

    public boolean isGlobalCostExceeded() {
        return getCurrentDailyCost().compareTo(DAILY_BUDGET) >= 0;
    }

    /**
     * 检查单用户日预算是否超限。
     */
    public boolean isUserCostExceeded(Long userId) {
        if (userId == null) return false;
        return getUserDailyCost(userId).compareTo(dailyPerUserBudget) >= 0;
    }

    /**
     * 获取用户当日消耗。
     */
    public BigDecimal getUserDailyCost(Long userId) {
        if (userId == null) return BigDecimal.ZERO;
        String userKey = REDIS_USER_COST_PREFIX + LocalDate.now() + ":" + userId;
        Object costObj = redisTemplate.opsForHash().get(userKey, "totalCost");
        return costObj != null ? new BigDecimal(costObj.toString()) : BigDecimal.ZERO;
    }

    /**
     * 获取某层级的消耗。
     */
    public BigDecimal getTierCost(ModelTier tier) {
        String tierKey = currentKey + ":tier:" + tier.name();
        Object costObj = redisTemplate.opsForHash().get(tierKey, "totalCost");
        if (costObj == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(costObj.toString());
    }

    /**
     * 获取各层级消耗汇总。
     */
    public Map<String, BigDecimal> getAllTierCosts() {
        Map<String, BigDecimal> result = new HashMap<>();
        for (ModelTier tier : ModelTier.values()) {
            result.put(tier.name(), getTierCost(tier));
        }
        return result;
    }

    public BigDecimal getCurrentDailyCost() {
        Object costObj = redisTemplate.opsForHash().get(currentKey, "totalCost");
        if (costObj == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(costObj.toString());
    }

    public BigDecimal getRemainingBudget() {
        BigDecimal currentCost = getCurrentDailyCost();
        BigDecimal remaining = DAILY_BUDGET.subtract(currentCost);
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }

    private void checkBudgetAlert(BigDecimal totalCost) {
        BigDecimal ratio = totalCost.divide(DAILY_BUDGET, 2, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.5")) >= 0 && ratio.compareTo(new BigDecimal("0.51")) < 0) {
            log.warn("DeepSeek成本告警: 已消耗50%日预算 totalCost={}", totalCost);
        } else if (ratio.compareTo(new BigDecimal("0.8")) >= 0 && ratio.compareTo(new BigDecimal("0.81")) < 0) {
            log.warn("DeepSeek成本告警: 已消耗80%日预算 totalCost={}", totalCost);
        } else if (ratio.compareTo(new BigDecimal("0.95")) >= 0) {
            log.error("DeepSeek成本告警: 接近日预算上限 totalCost={}", totalCost);
        }
    }

    private String buildKey() {
        return COST_KEY_PREFIX + LocalDate.now();
    }

    private String buildYesterdayKey() {
        return COST_KEY_PREFIX + LocalDate.now().minusDays(1);
    }
}
