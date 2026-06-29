package com.example.vector;

import java.util.*;
import java.util.stream.Collectors;

/**
 * RRF（Reciprocal Rank Fusion）融合算法。
 * 将多路检索结果（向量检索 + BM25 关键词检索）融合为统一排序。
 *
 * 公式：score(doc) = Σ 1/(k + rank_i)
 * 其中 k=60（默认），rank_i 为文档在第 i 路检索结果中的排名（从 1 开始）。
 */
public class RrfFusion {

    /** 每个文档的融合得分记录 */
    public static class FusionScore {
        private final String docId;
        private final Map<String, Integer> ranks;
        private double rrfScore;

        public FusionScore(String docId) {
            this.docId = docId;
            this.ranks = new HashMap<>();
        }

        public String getDocId() {
            return docId;
        }

        public void addRank(String source, int rank) {
            ranks.put(source, rank);
        }

        public double getRrfScore() {
            return rrfScore;
        }

        public void setRrfScore(double rrfScore) {
            this.rrfScore = rrfScore;
        }

        public Map<String, Integer> getRanks() {
            return ranks;
        }
    }

    /**
     * 执行 RRF 融合。
     *
     * @param rankedResults 各路检索结果，key 为来源名称（如 "vector", "bm25"），
     *                      value 为按相关性排序的文档 ID 列表（已排序，第 1 名 rank=1）
     * @param k             RRF 参数 k，默认 60
     * @return 融合后的文档 ID 列表，按 RRF 得分降序排列
     */
    public static List<FusionScore> fuse(Map<String, List<String>> rankedResults, int k) {
        Map<String, FusionScore> scoreMap = new LinkedHashMap<>();

        for (Map.Entry<String, List<String>> entry : rankedResults.entrySet()) {
            String source = entry.getKey();
            List<String> docIds = entry.getValue();

            for (int i = 0; i < docIds.size(); i++) {
                String docId = docIds.get(i);
                int rank = i + 1; // 排名从 1 开始

                FusionScore fs = scoreMap.computeIfAbsent(docId, FusionScore::new);
                fs.addRank(source, rank);
            }
        }

        // 计算 RRF 得分
        for (FusionScore fs : scoreMap.values()) {
            double score = 0.0;
            for (int rank : fs.getRanks().values()) {
                score += 1.0 / (k + rank);
            }
            fs.setRrfScore(score);
        }

        // 按 RRF 得分降序排序
        return scoreMap.values().stream()
                .sorted(Comparator.comparingDouble(FusionScore::getRrfScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 执行 RRF 融合并返回 Top-N 文档 ID。
     */
    public static List<String> fuseTopN(Map<String, List<String>> rankedResults, int k, int topN) {
        List<FusionScore> fused = fuse(rankedResults, k);
        return fused.stream()
                .limit(topN)
                .map(FusionScore::getDocId)
                .collect(Collectors.toList());
    }
}