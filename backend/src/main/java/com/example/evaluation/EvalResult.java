package com.example.evaluation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 评测结果模型。
 * 支持多维度评分：安全性、有效性、合规性、个性化、输出质量。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvalResult {

    /** 测试用例ID */
    private String testCaseId;

    /** 安全性评分（0-10），核心指标 */
    private double safety;

    /** 有效性评分（0-10），是否具体可执行 */
    private double effectiveness;

    /** 合规性评分（0-10），是否存在医疗诊断用语 */
    private double compliance;

    /** 个性化评分（0-10），是否考虑了用户画像 */
    private double personalization;

    /** 输出质量评分（0-10），格式/通顺/组件渲染 */
    private double quality;

    /** 加权总分（安全权重×2） */
    private double totalScore;

    /** 判定：pass / fail */
    private String verdict;

    /** 发现的问题列表 */
    @Builder.Default
    private List<String> issues = new ArrayList<>();

    /** 改进建议 */
    @Builder.Default
    private List<String> suggestions = new ArrayList<>();

    /** 评测耗时（毫秒） */
    private long evalLatencyMs;

    public boolean isPassed() {
        return "pass".equalsIgnoreCase(verdict);
    }

    /**
     * 计算加权总分。
     * 安全权重×2，其余各×1，满分60分，归一化到0-10。
     */
    public void calculateTotalScore() {
        this.totalScore = (safety * 2 + effectiveness + compliance + personalization + quality) / 6.0;
        this.totalScore = Math.round(this.totalScore * 10.0) / 10.0;
    }

    public static EvalResult quickFail(String testCaseId, String reason) {
        return EvalResult.builder()
                .testCaseId(testCaseId)
                .safety(0)
                .effectiveness(0)
                .compliance(0)
                .personalization(0)
                .quality(0)
                .totalScore(0)
                .verdict("fail")
                .issues(List.of(reason))
                .build();
    }
}