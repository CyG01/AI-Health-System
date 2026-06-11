package com.example.billing;

import com.example.entity.Subscription;
import com.example.entity.UserUsage;
import com.example.mapper.SubscriptionMapper;
import com.example.mapper.UserUsageMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.concurrent.TimeUnit;

/**
 * 计费引擎核心服务。
 *
 * 定价策略（元/百万token）：
 * - 免费版：每日 3 次调用，超出拦截
 * - Pro 版（¥19/月）：input ¥1/M, output ¥2/M，含 500 万 token/月
 * - 企业版（¥99/月）：input ¥0.8/M, output ¥1.6/M，含 2000 万 token/月
 */
@Slf4j
@Service
public class BillingService {

    private static final BigDecimal INPUT_PRICE_PRO = new BigDecimal("1.00");
    private static final BigDecimal OUTPUT_PRICE_PRO = new BigDecimal("2.00");
    private static final BigDecimal INPUT_PRICE_ENT = new BigDecimal("0.80");
    private static final BigDecimal OUTPUT_PRICE_ENT = new BigDecimal("1.60");
    private static final BigDecimal MILLION = new BigDecimal("1000000");

    private static final String USAGE_CACHE_PREFIX = "billing:usage:";
    private static final int CACHE_TTL_HOURS = 25;

    private final SubscriptionMapper subscriptionMapper;
    private final UserUsageMapper userUsageMapper;
    private final RedisTemplate<String, Object> redisTemplate;

    public BillingService(SubscriptionMapper subscriptionMapper,
                           UserUsageMapper userUsageMapper,
                           RedisTemplate<String, Object> redisTemplate) {
        this.subscriptionMapper = subscriptionMapper;
        this.userUsageMapper = userUsageMapper;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取用户订阅等级。
     */
    public String getUserTier(Long userId) {
        Subscription sub = subscriptionMapper.findActiveByUserId(userId);
        return sub != null && sub.isActive() ? sub.getTier() : "free";
    }

    /**
 * 检查用户是否有调用额度（日限制 + 月度Token限制）。
 */
public boolean canCall(Long userId, String callType) {
    String tier = getUserTier(userId);
    if ("enterprise".equals(tier)) return true;

    // 检查月度Token限额
    if (isMonthlyLimitExceeded(userId)) {
        log.warn("用户月度Token额度已用尽 userId={} tier={}", userId, tier);
        return false;
    }

    BillingSummary summary = getDailySummary(userId);
    if (summary.isExceeded()) return false;

    return switch (callType) {
        case "plan_generate" -> {
            if ("pro".equals(tier)) yield true;
            yield summary.getLimit().getDailyPlanGenLimit() < 0
                    || summary.getApiCallCount() < summary.getLimit().getDailyPlanGenLimit();
        }
        case "food_recognize" -> {
            if ("pro".equals(tier)) yield true;
            yield summary.getLimit().getDailyFoodRecogLimit() < 0
                    || summary.getFoodRecogCount() < summary.getLimit().getDailyFoodRecogLimit();
        }
        case "chat" -> {
            if ("pro".equals(tier)) yield true;
            yield summary.getLimit().getDailyChatLimit() < 0
                    || summary.getChatCount() < summary.getLimit().getDailyChatLimit();
        }
        default -> summary.getApiCallCount() < summary.getLimit().getDailyCallLimit()
                    || summary.getLimit().getDailyCallLimit() < 0;
    };
}

/**
 * 检查用户月度Token消耗是否超过套餐限额。
 * Pro版含500万token/月，企业版含2000万token/月，免费版100万token/月。
 */
public boolean isMonthlyLimitExceeded(Long userId) {
    String tier = getUserTier(userId);
    BillingSummary.UsageLimit limit = getUsageLimit(userId, tier);

    int monthlyLimitM = limit.getMonthlyTokenLimitM();
    if (monthlyLimitM <= 0) return false; // 无月度限制

    long monthlyTokens = getMonthlyTokenSum(userId);
    long monthlyLimit = (long) monthlyLimitM * 1_000_000;

    boolean exceeded = monthlyTokens >= monthlyLimit;
    if (exceeded) {
        log.warn("月度Token额度超限 userId={} tier={} tokens={}/{}M",
                userId, tier, monthlyTokens, monthlyLimitM);
    }
    return exceeded;
}

/**
 * 获取用户当月累计Token消耗量（从DB聚合）。
 */
public long getMonthlyTokenSum(Long userId) {
    try {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        long total = userUsageMapper.sumTokensByUserIdAndDateRange(userId, monthStart, now);
        return total;
    } catch (Exception e) {
        log.warn("查询月度Token汇总失败 userId={}", userId, e);
        return 0;
    }
}

/**
 * 获取用户当月累计费用。
 */
public BigDecimal getMonthlyCost(Long userId) {
    try {
        LocalDate now = LocalDate.now();
        LocalDate monthStart = now.withDayOfMonth(1);
        BigDecimal cost = userUsageMapper.sumCostByUserIdAndDateRange(userId, monthStart, now);
        return cost != null ? cost : BigDecimal.ZERO;
    } catch (Exception e) {
        log.warn("查询月度费用汇总失败 userId={}", userId, e);
        return BigDecimal.ZERO;
    }
}

    /**
     * 记录一次 API 调用并计费。
     */
    public void recordCall(Long userId, String callType, int inputTokens, int outputTokens) {
        String tier = getUserTier(userId);
        BigDecimal cost = calculateCost(tier, inputTokens, outputTokens);

        // 更新 Redis 缓存
        String cacheKey = usageCacheKey(userId);
        redisTemplate.opsForHash().increment(cacheKey, "inputTokens", inputTokens);
        redisTemplate.opsForHash().increment(cacheKey, "outputTokens", outputTokens);
        redisTemplate.opsForHash().increment(cacheKey, "apiCallCount", 1);
        redisTemplate.opsForHash().increment(cacheKey, "dailyCost", cost.doubleValue());
        redisTemplate.opsForHash().increment(cacheKey, callType + "Count", 1);
        redisTemplate.expire(cacheKey, CACHE_TTL_HOURS, TimeUnit.HOURS);

        // 持久化到 DB
        persistUsage(userId, callType, inputTokens, outputTokens, cost);

        log.debug("计费记录 userId={} tier={} callType={} cost={} tokens={}/{}",
                userId, tier, callType, cost, inputTokens, outputTokens);
    }

    /**
     * 获取当日用量汇总（含额度百分比和预警级别，用于前端额度提醒展示）。
     */
    public BillingSummary getDailySummary(Long userId) {
        String tier = getUserTier(userId);
        String cacheKey = usageCacheKey(userId);

        var entries = redisTemplate.opsForHash().entries(cacheKey);
        int inputTokens = getInt(entries, "inputTokens");
        int outputTokens = getInt(entries, "outputTokens");
        int apiCallCount = getInt(entries, "apiCallCount");
        int planGenCount = getInt(entries, "plan_generateCount");
        int foodRecogCount = getInt(entries, "food_recognizeCount");
        int chatCount = getInt(entries, "chatCount");
        BigDecimal dailyCost = getDecimal(entries, "dailyCost");

        BillingSummary.UsageLimit limit = getUsageLimit(userId, tier);

        boolean exceeded = (limit.getDailyCallLimit() > 0 && apiCallCount >= limit.getDailyCallLimit())
                || (limit.getDailyPlanGenLimit() > 0 && planGenCount >= limit.getDailyPlanGenLimit())
                || (limit.getDailyFoodRecogLimit() > 0 && foodRecogCount >= limit.getDailyFoodRecogLimit());

        // 计算当日用量百分比和预警级别
        int usagePercent = calcDailyUsagePercent(apiCallCount, planGenCount, foodRecogCount, limit);
        String quotaLevel = calcQuotaLevel(usagePercent, exceeded);

        // 本月累计Token
        long monthlyTokensUsed = getMonthlyTokenSum(userId);

        return BillingSummary.builder()
                .userId(userId)
                .tier(tier)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .planGenCount(planGenCount)
                .foodRecogCount(foodRecogCount)
                .chatCount(chatCount)
                .apiCallCount(apiCallCount)
                .dailyCost(dailyCost)
                .monthlyCost(getMonthlyCost(userId))
                .limit(limit)
                .exceeded(exceeded)
                .usagePercent(usagePercent)
                .quotaLevel(quotaLevel)
                .monthlyTokensUsed(monthlyTokensUsed)
                .build();
    }

    /**
     * 计算当日用量百分比（取各维度最高值）。
     */
    private int calcDailyUsagePercent(int callCount, int planGenCount, int foodRecogCount,
                                       BillingSummary.UsageLimit limit) {
        int maxPercent = 0;
        if (limit.getDailyCallLimit() > 0) {
            maxPercent = Math.max(maxPercent, callCount * 100 / limit.getDailyCallLimit());
        }
        if (limit.getDailyPlanGenLimit() > 0) {
            maxPercent = Math.max(maxPercent, planGenCount * 100 / limit.getDailyPlanGenLimit());
        }
        if (limit.getDailyFoodRecogLimit() > 0) {
            maxPercent = Math.max(maxPercent, foodRecogCount * 100 / limit.getDailyFoodRecogLimit());
        }
        return Math.min(maxPercent, 100);
    }

    /**
     * 根据用量百分比和超额状态计算预警级别。
     */
    private String calcQuotaLevel(int usagePercent, boolean exceeded) {
        if (exceeded || usagePercent >= 100) return "exceeded";
        if (usagePercent >= 80) return "warning";
        return "normal";
    }

    /**
     * 获取额度预警信息（供前端主动拉取，用于展示剩余额度提醒）。
     * 返回包含剩余额度、预警级别、建议操作的信息。
     */
    public java.util.Map<String, Object> getQuotaWarning(Long userId) {
        BillingSummary summary = getDailySummary(userId);
        java.util.Map<String, Object> warning = new java.util.LinkedHashMap<>();
        warning.put("quotaLevel", summary.getQuotaLevel());
        warning.put("usagePercent", summary.getUsagePercent());

        if (summary.getLimit().getDailyCallLimit() > 0) {
            int remaining = summary.getLimit().getDailyCallLimit() - summary.getApiCallCount();
            warning.put("remainingCalls", Math.max(0, remaining));
            warning.put("dailyLimit", summary.getLimit().getDailyCallLimit());
        } else {
            warning.put("remainingCalls", -1); // 无限
            warning.put("dailyLimit", -1);
        }

        if (summary.getLimit().getMonthlyTokenLimitM() > 0) {
            long monthlyLimit = (long) summary.getLimit().getMonthlyTokenLimitM() * 1_000_000;
            long remainingTokens = Math.max(0, monthlyLimit - summary.getMonthlyTokensUsed());
            warning.put("remainingTokens", remainingTokens);
            warning.put("monthlyTokenLimit", monthlyLimit);
            warning.put("monthlyTokensUsed", summary.getMonthlyTokensUsed());
        }

        String message;
        switch (summary.getQuotaLevel()) {
            case "exceeded":
                message = "今日AI调用额度已用尽，请在明天0点后重试。升级Pro/企业版可享无限调用。";
                break;
            case "warning":
                message = "今日额度即将用尽（已使用" + summary.getUsagePercent() + "%），建议升级以获取更多调用次数。";
                break;
            default:
                message = "额度充足，今日已使用" + summary.getUsagePercent() + "%。";
        }
        warning.put("message", message);
        warning.put("tier", summary.getTier());

        return warning;
    }

    /**
     * 计算调用费用。
     */
    private BigDecimal calculateCost(String tier, int inputTokens, int outputTokens) {
        BigDecimal inputPrice = "enterprise".equals(tier) ? INPUT_PRICE_ENT : INPUT_PRICE_PRO;
        BigDecimal outputPrice = "enterprise".equals(tier) ? OUTPUT_PRICE_ENT : OUTPUT_PRICE_PRO;

        BigDecimal inputMil = new BigDecimal(inputTokens).divide(MILLION, 6, RoundingMode.HALF_UP);
        BigDecimal outputMil = new BigDecimal(outputTokens).divide(MILLION, 6, RoundingMode.HALF_UP);

        return inputMil.multiply(inputPrice)
                .add(outputMil.multiply(outputPrice))
                .setScale(6, RoundingMode.HALF_UP);
    }

    /**
     * 获取各等级的用量限制（支持企业版自定义额度）。
     */
    public BillingSummary.UsageLimit getUsageLimit(Long userId, String tier) {
        return switch (tier) {
            case "free" -> BillingSummary.UsageLimit.builder()
                    .dailyCallLimit(3)
                    .dailyPlanGenLimit(3)
                    .dailyFoodRecogLimit(5)
                    .dailyChatLimit(10)
                    .monthlyTokenLimitM(1)
                    .build();
            case "pro" -> BillingSummary.UsageLimit.builder()
                    .dailyCallLimit(-1)     // 无限
                    .dailyPlanGenLimit(-1)
                    .dailyFoodRecogLimit(-1)
                    .dailyChatLimit(-1)
                    .monthlyTokenLimitM(5)
                    .build();
            case "enterprise" -> {
                // 企业版支持自定义额度：从Subscription读取定制配置
                int customTokenQuotaM = 20; // 默认2000万Token
                if (userId != null) {
                    var sub = subscriptionMapper.findActiveByUserId(userId);
                    if (sub != null && sub.getCustomTokenQuotaM() != null && sub.getCustomTokenQuotaM() > 0) {
                        customTokenQuotaM = sub.getCustomTokenQuotaM();
                    }
                }
                yield BillingSummary.UsageLimit.builder()
                        .dailyCallLimit(-1)
                        .dailyPlanGenLimit(-1)
                        .dailyFoodRecogLimit(-1)
                        .dailyChatLimit(-1)
                        .monthlyTokenLimitM(customTokenQuotaM)
                        .build();
            }
            default -> BillingSummary.UsageLimit.builder()
                    .dailyCallLimit(3).dailyPlanGenLimit(3)
                    .dailyFoodRecogLimit(5).dailyChatLimit(10)
                    .monthlyTokenLimitM(1).build();
        };
    }

    /**
     * 获取各等级的用量限制（不区分用户，用于月度汇总等场景）。
     */
    public BillingSummary.UsageLimit getUsageLimit(String tier) {
        return getUsageLimit(null, tier);
    }

    private void persistUsage(Long userId, String callType, int inputTokens, int outputTokens, BigDecimal cost) {
        try {
            LocalDate today = LocalDate.now();
            UserUsage usage = userUsageMapper.findByUserIdAndDate(userId, today);
            if (usage == null) {
                usage = new UserUsage();
                usage.setUserId(userId);
                usage.setUsageDate(today);
                usage.setInputTokens(0);
                usage.setOutputTokens(0);
                usage.setApiCallCount(0);
                usage.setPlanGenCount(0);
                usage.setFoodRecogCount(0);
                usage.setChatCount(0);
                usage.setDailyCost(BigDecimal.ZERO);
                usage.setCreatedAt(java.time.LocalDateTime.now());
            }

            usage.setInputTokens((usage.getInputTokens() != null ? usage.getInputTokens() : 0) + inputTokens);
            usage.setOutputTokens((usage.getOutputTokens() != null ? usage.getOutputTokens() : 0) + outputTokens);
            usage.setApiCallCount((usage.getApiCallCount() != null ? usage.getApiCallCount() : 0) + 1);
            usage.setDailyCost((usage.getDailyCost() != null ? usage.getDailyCost() : BigDecimal.ZERO).add(cost));

            switch (callType) {
                case "plan_generate" -> usage.setPlanGenCount(
                        (usage.getPlanGenCount() != null ? usage.getPlanGenCount() : 0) + 1);
                case "food_recognize" -> usage.setFoodRecogCount(
                        (usage.getFoodRecogCount() != null ? usage.getFoodRecogCount() : 0) + 1);
                case "chat" -> usage.setChatCount(
                        (usage.getChatCount() != null ? usage.getChatCount() : 0) + 1);
            }

            userUsageMapper.insertOrUpdate(usage);
        } catch (Exception e) {
            log.error("持久化用量记录失败 userId={}", userId, e);
        }
    }

    private String usageCacheKey(Long userId) {
        return USAGE_CACHE_PREFIX + userId + ":" + LocalDate.now();
    }

    private int getInt(java.util.Map<Object, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return 0;
        return val instanceof Number ? ((Number) val).intValue() : Integer.parseInt(val.toString());
    }

    private BigDecimal getDecimal(java.util.Map<Object, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return BigDecimal.ZERO;
        return val instanceof BigDecimal ? (BigDecimal) val : new BigDecimal(val.toString());
    }

    // 用于统计模块的业务方法
    public int getFoodRecogCount(Long userId) {
        BillingSummary summary = getDailySummary(userId);
        return summary != null ? summary.getFoodRecogCount() : 0;
    }

    public int getChatCount(Long userId) {
        BillingSummary summary = getDailySummary(userId);
        return summary != null ? summary.getChatCount() : 0;
    }

    /**
     * 获取月度用量汇总（含Token消耗、费用、订阅等级）。
     */
    public java.util.Map<String, Object> getMonthlyUsageSummary(Long userId) {
        String tier = getUserTier(userId);
        long monthlyTokens = getMonthlyTokenSum(userId);
        BigDecimal monthlyCost = getMonthlyCost(userId);
        BillingSummary.UsageLimit limit = getUsageLimit(userId, tier);

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("userId", userId);
        result.put("tier", tier);
        result.put("monthlyTokens", monthlyTokens);
        result.put("monthlyCost", monthlyCost);
        result.put("monthlyTokenLimitM", limit.getMonthlyTokenLimitM());
        result.put("monthlyTokenLimitBytes", (long) limit.getMonthlyTokenLimitM() * 1_000_000);
        result.put("monthlyLimitExceeded", isMonthlyLimitExceeded(userId));
        result.put("month", java.time.LocalDate.now().getMonthValue());
        result.put("year", java.time.LocalDate.now().getYear());

        // 续费提醒：接近到期时加入提醒信息
        var sub = subscriptionMapper.findActiveByUserId(userId);
        if (sub != null && sub.getEndTime() != null) {
            long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDateTime.now(), sub.getEndTime());
            result.put("daysUntilExpiry", daysUntilExpiry);
            if (daysUntilExpiry <= 7 && daysUntilExpiry > 0) {
                result.put("renewalWarning", "您的订阅将在" + daysUntilExpiry + "天后到期，请及时续费以免服务中断。");
                result.put("renewalLevel", daysUntilExpiry <= 3 ? "urgent" : "normal");
            }
        }
        return result;
    }

    /**
     * 获取用户近 N 天的每日用量明细列表。
     */
    public java.util.List<UserUsage> getUsageHistory(Long userId, int days) {
        java.time.LocalDate end = java.time.LocalDate.now();
        java.time.LocalDate start = end.minusDays(days - 1);
        return userUsageMapper.listByUserIdAndDateRange(userId, start, end);
    }
}