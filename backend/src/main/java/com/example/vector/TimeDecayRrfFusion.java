package com.example.vector;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 时间衰减增强版 RRF 融合算法。
 * 在标准 RRF 基础上，引入时间衰减因子，让近期的记忆和记录获得更高权重。
 *
 * 时间衰减公式：decay = exp(-λ * days)
 * 其中 λ 为衰减系数，days 为距离现在的天数
 *
 * 最终得分：score(doc) = Σ [1/(k + rank_i)] * time_decay
 */
public class TimeDecayRrfFusion {

    /** 带时间信息的融合得分记录 */
    public static class TimeDecayFusionScore {
        private final String docId;
        private final Map<String, Integer> ranks;
        private LocalDateTime eventTime;
        private double rrfScore;
        private double timeDecayScore;
        private double finalScore;

        public TimeDecayFusionScore(String docId) {
            this.docId = docId;
            this.ranks = new HashMap<>();
        }

        public String getDocId() { return docId; }

        public void addRank(String source, int rank) {
            ranks.put(source, rank);
        }

        public LocalDateTime getEventTime() { return eventTime; }
        public void setEventTime(LocalDateTime eventTime) { this.eventTime = eventTime; }

        public double getRrfScore() { return rrfScore; }
        public void setRrfScore(double rrfScore) { this.rrfScore = rrfScore; }

        public double getTimeDecayScore() { return timeDecayScore; }
        public void setTimeDecayScore(double timeDecayScore) { this.timeDecayScore = timeDecayScore; }

        public double getFinalScore() { return finalScore; }
        public void setFinalScore(double finalScore) { this.finalScore = finalScore; }

        public Map<String, Integer> getRanks() { return ranks; }
    }

    /**
     * 执行带时间衰减的 RRF 融合。
     *
     * @param rankedResults 各路检索结果，key 为来源名称，value 为按相关性排序的文档 ID 列表
     * @param eventTimeMap  文档ID到事件时间的映射（用于计算时间衰减）
     * @param k             RRF 参数 k，默认 60
     * @param decayLambda   时间衰减系数 λ（每天衰减比例），默认 0.05（约半衰期14天）
     * @param timeWeight    时间衰减权重（0-1），0表示完全不用时间衰减，1表示时间衰减占主导
     * @return 融合后的文档得分列表，按最终得分降序排列
     */
    public static List<TimeDecayFusionScore> fuseWithTimeDecay(
            Map<String, List<String>> rankedResults,
            Map<String, LocalDateTime> eventTimeMap,
            int k,
            double decayLambda,
            double timeWeight) {

        Map<String, TimeDecayFusionScore> scoreMap = new LinkedHashMap<>();
        LocalDateTime now = LocalDateTime.now();

        // 1. 收集所有文档及其排名
        for (Map.Entry<String, List<String>> entry : rankedResults.entrySet()) {
            String source = entry.getKey();
            List<String> docIds = entry.getValue();

            for (int i = 0; i < docIds.size(); i++) {
                String docId = docIds.get(i);
                int rank = i + 1;

                TimeDecayFusionScore fs = scoreMap.computeIfAbsent(docId, TimeDecayFusionScore::new);
                fs.addRank(source, rank);

                // 设置事件时间（如果有）
                if (eventTimeMap != null && eventTimeMap.containsKey(docId)) {
                    fs.setEventTime(eventTimeMap.get(docId));
                }
            }
        }

        // 2. 计算 RRF 基础得分
        for (TimeDecayFusionScore fs : scoreMap.values()) {
            double rrfScore = 0.0;
            for (int rank : fs.getRanks().values()) {
                rrfScore += 1.0 / (k + rank);
            }
            fs.setRrfScore(rrfScore);
        }

        // 3. 计算时间衰减得分
        double maxRrfScore = scoreMap.values().stream()
                .mapToDouble(TimeDecayFusionScore::getRrfScore)
                .max().orElse(1.0);

        for (TimeDecayFusionScore fs : scoreMap.values()) {
            double decay = 1.0; // 默认无衰减

            if (fs.getEventTime() != null && eventTimeMap != null) {
                long days = ChronoUnit.DAYS.between(fs.getEventTime(), now);
                if (days > 0) {
                    // 指数衰减：decay = exp(-λ * days)
                    decay = Math.exp(-decayLambda * days);
                }
            }

            fs.setTimeDecayScore(decay);

            // 4. 计算最终得分：RRF得分 * (1 - timeWeight) + 时间衰减 * timeWeight * maxRrfScore
            double normalizedRrf = fs.getRrfScore() / maxRrfScore;
            double finalScore = normalizedRrf * (1 - timeWeight) + decay * timeWeight;
            fs.setFinalScore(finalScore);
        }

        // 5. 按最终得分降序排序
        return scoreMap.values().stream()
                .sorted(Comparator.comparingDouble(TimeDecayFusionScore::getFinalScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 简化版：使用默认参数执行时间衰减 RRF 融合。
     *
     * @param rankedResults 各路检索结果
     * @param eventTimeMap  文档ID到事件时间的映射
     * @param topN          返回前N个
     * @return 融合后的文档 ID 列表
     */
    public static List<String> fuseTopNWithTimeDecay(
            Map<String, List<String>> rankedResults,
            Map<String, LocalDateTime> eventTimeMap,
            int topN) {
        // 默认参数：k=60, decayLambda=0.05(约14天半衰期), timeWeight=0.3
        List<TimeDecayFusionScore> fused = fuseWithTimeDecay(
                rankedResults, eventTimeMap, 60, 0.05, 0.3);
        return fused.stream()
                .limit(topN)
                .map(TimeDecayFusionScore::getDocId)
                .collect(Collectors.toList());
    }

    /**
     * 计算时间衰减因子。
     *
     * @param eventTime    事件时间
     * @param decayLambda  衰减系数
     * @return 衰减因子（0-1），1表示最新，0表示完全衰减
     */
    public static double calculateDecayFactor(LocalDateTime eventTime, double decayLambda) {
        if (eventTime == null) {
            return 0.5; // 未知时间给中等权重
        }
        long days = ChronoUnit.DAYS.between(eventTime, LocalDateTime.now());
        if (days <= 0) {
            return 1.0; // 今天的事件权重最高
        }
        return Math.exp(-decayLambda * days);
    }

    /**
     * 获取推荐的衰减系数。
     *
     * @param halfLifeDays 半衰期（天），即权重衰减到50%需要的天数
     * @return 衰减系数 λ
     */
    public static double getDecayLambdaByHalfLife(int halfLifeDays) {
        if (halfLifeDays <= 0) {
            return 0.05; // 默认14天半衰期
        }
        return Math.log(2) / halfLifeDays;
    }
}
