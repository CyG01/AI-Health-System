package com.example.monitor;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Component
public class DeepSeekCostMonitor {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekCostMonitor.class);

    private static final String COST_KEY_PREFIX = "deepseek:cost:daily:";

    private static final BigDecimal INPUT_PRICE_PER_M = new BigDecimal("1");
    private static final BigDecimal OUTPUT_PRICE_PER_M = new BigDecimal("2");
    private static final BigDecimal DAILY_BUDGET = new BigDecimal("10.00");

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
        currentKey = buildKey();
        log.info("DeepSeek每日消耗统计已重置");
    }

    public void recordCall(int inputTokens, int outputTokens) {
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

        BigDecimal totalCost = getCurrentDailyCost();
        log.info("DeepSeek API调用 cost={} totalDailyCost={}", callCost, totalCost);
    }

    public boolean isGlobalCostExceeded() {
        return getCurrentDailyCost().compareTo(DAILY_BUDGET) >= 0;
    }

    private BigDecimal getCurrentDailyCost() {
        Object costObj = redisTemplate.opsForHash().get(currentKey, "totalCost");
        if (costObj == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(costObj.toString());
    }

    private String buildKey() {
        return COST_KEY_PREFIX + LocalDate.now();
    }

    private String buildYesterdayKey() {
        return COST_KEY_PREFIX + LocalDate.now().minusDays(1);
    }
}
