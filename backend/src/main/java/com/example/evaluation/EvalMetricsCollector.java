package com.example.evaluation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 评测度量收集器。
 * 收集 AI 输出质量、用户满意度、成本指标到 Redis，供 Grafana 面板消费。
 */
@Slf4j
@Component
public class EvalMetricsCollector {

    private static final String METRICS_PREFIX = "eval:metrics:";
    private static final String DAILY_PREFIX = "eval:daily:";
    private static final int TTL_DAYS = 30;

    private final RedisTemplate<String, Object> redisTemplate;

    public EvalMetricsCollector(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 记录单次评测结果。
     */
    public void recordEvalResult(String category, String safetyLevel, EvalResult result) {
        String today = LocalDate.now().toString();

        // 按类别统计
        incrMetric("category:" + category + ":total");
        incrMetric("category:" + category + ":" + result.getVerdict());

        // 按安全等级统计
        incrMetric("safetyLevel:" + safetyLevel + ":total");
        incrMetric("safetyLevel:" + safetyLevel + ":" + result.getVerdict());

        // 每日统计
        incrDailyMetric(today, "total");
        incrDailyMetric(today, result.getVerdict());

        // 评分累计（用于计算平均值）
        incrDailyMetricDouble(today, "safety_sum", result.getSafety());
        incrDailyMetricDouble(today, "effectiveness_sum", result.getEffectiveness());
        incrDailyMetricDouble(today, "compliance_sum", result.getCompliance());
        incrDailyMetricDouble(today, "quality_sum", result.getQuality());

        // 评测延迟
        incrMetricLong("latency:total", result.getEvalLatencyMs());
        incrMetric("latency:count");
    }

    /**
     * 记录用户满意度反馈。
     */
    public void recordUserSatisfaction(Long userId, String aiResponseId, String rating) {
        String key = METRICS_PREFIX + "satisfaction";
        redisTemplate.opsForHash().increment(key, "total", 1);
        redisTemplate.opsForHash().increment(key, rating, 1);

        String userKey = METRICS_PREFIX + "user:" + userId + ":satisfaction";
        redisTemplate.opsForHash().increment(userKey, rating, 1);

        log.info("用户满意度反馈 userId={} responseId={} rating={}", userId, aiResponseId, rating);
    }

    /**
     * 记录关键失败（用于告警）。
     */
    public void recordCriticalFailure(int failedCount) {
        String key = METRICS_PREFIX + "critical_failures";
        redisTemplate.opsForValue().increment(key);
        redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
        log.error("CRITICAL评测累计失败次数: {}", getCriticalFailureCount());
    }

    /**
     * 记录熔断风险。
     */
    public void recordMeltdownRisk(double avgSafety) {
        String key = METRICS_PREFIX + "meltdown_risk";
        redisTemplate.opsForValue().set(key, String.format("%.2f", avgSafety), 1, TimeUnit.HOURS);
    }

    /**
     * 成本指标：记录每日总成本。
     */
    public void recordDailyCost(double cost) {
        String today = LocalDate.now().toString();
        incrDailyMetricDouble(today, "cost", cost);
    }

    /**
     * 获取当前度量汇总（供 API 和 Grafana 消费）。
     */
    public Map<String, Object> getMetricsSummary() {
        Map<String, Object> summary = new HashMap<>();

        for (String category : new String[]{"basic", "risk", "edge", "online_sample"}) {
            String key = METRICS_PREFIX + "category:" + category;
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (!entries.isEmpty()) {
                summary.put("category_" + category, entries);
            }
        }

        for (String level : new String[]{"safe", "risky", "critical"}) {
            String key = METRICS_PREFIX + "safetyLevel:" + level;
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            if (!entries.isEmpty()) {
                summary.put("safetyLevel_" + level, entries);
            }
        }

        summary.put("criticalFailures", getCriticalFailureCount());
        summary.put("avgSafety", getAverageSafetyScores());

        return summary;
    }

    public long getCriticalFailureCount() {
        String key = METRICS_PREFIX + "critical_failures";
        Object val = redisTemplate.opsForValue().get(key);
        return val != null ? Long.parseLong(val.toString()) : 0;
    }

    public double getAverageSafetyScores() {
        String today = LocalDate.now().toString();
        String key = DAILY_PREFIX + today;
        Object totalObj = redisTemplate.opsForHash().get(key, "total");
        Object sumObj = redisTemplate.opsForHash().get(key, "safety_sum");

        if (totalObj == null || sumObj == null) return 10.0;
        long total = Long.parseLong(totalObj.toString());
        double sum = Double.parseDouble(sumObj.toString());
        return total > 0 ? Math.round(sum / total * 10.0) / 10.0 : 10.0;
    }

    /**
     * 获取 RAG 幻觉率（由 RagasMonitor 写入 Redis）。
     */
    public double getHallucinationRate() {
        String key = METRICS_PREFIX + "ragas:hallucination_rate";
        Object val = redisTemplate.opsForValue().get(key);
        return val != null ? Double.parseDouble(val.toString()) : 0.0;
    }

    /**
     * 更新 RAG 幻觉率（由 RagasMonitor 调用）。
     */
    public void updateHallucinationRate(double rate) {
        String key = METRICS_PREFIX + "ragas:hallucination_rate";
        redisTemplate.opsForValue().set(key, String.format("%.4f", rate), 1, TimeUnit.HOURS);
    }

    /**
     * 获取 RAG 上下文召回率。
     */
    public double getContextRecall() {
        String key = METRICS_PREFIX + "ragas:context_recall";
        Object val = redisTemplate.opsForValue().get(key);
        return val != null ? Double.parseDouble(val.toString()) : 1.0;
    }

    /**
     * 更新 RAG 上下文召回率。
     */
    public void updateContextRecall(double recall) {
        String key = METRICS_PREFIX + "ragas:context_recall";
        redisTemplate.opsForValue().set(key, String.format("%.4f", recall), 1, TimeUnit.HOURS);
    }

    private void incrMetric(String field) {
        String key = METRICS_PREFIX + field.split(":")[0];
        if (field.contains(":")) {
            key = METRICS_PREFIX + field.substring(0, field.lastIndexOf(":"));
            field = field.substring(field.lastIndexOf(":") + 1);
        }
        redisTemplate.opsForHash().increment(key, field, 1);
        redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
    }

    private void incrDailyMetric(String date, String field) {
        String key = DAILY_PREFIX + date;
        redisTemplate.opsForHash().increment(key, field, 1);
        redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
    }

    private void incrDailyMetricDouble(String date, String field, double value) {
        String key = DAILY_PREFIX + date;
        redisTemplate.opsForHash().increment(key, field, value);
        redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
    }

    private void incrMetricLong(String field, long value) {
        String key = METRICS_PREFIX + field.split(":")[0];
        redisTemplate.opsForHash().increment(key, field, value);
        redisTemplate.expire(key, TTL_DAYS, TimeUnit.DAYS);
    }
}