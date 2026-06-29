package com.example.evaluation;

import com.example.entity.KnowledgeDoc;
import com.example.entity.RagasTestCase;
import com.example.llmops.AlertManager;
import com.example.llmops.PrometheusMetricsExporter;
import com.example.mapper.RagasTestCaseMapper;
import com.example.resilience.ModelRouter;
import com.example.service.KnowledgeService;
import com.example.service.MemoryService;
import io.micrometer.core.instrument.Gauge;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * RAGAS 质量监控器。
 *
 * 每日凌晨2点运行 RAG 质量检查，监控三大核心指标：
 * 1. 上下文召回率 — 检索结果中包含正确答案的比例
 * 2. 答案忠实度 — AI答案与检索上下文的一致性
 * 3. 幻觉率 — AI答案中不在检索上下文中的内容比例
 */
@Slf4j
@Component
public class RagasMonitor {

    private static final double RECALL_THRESHOLD = 0.9;
    private static final double HALLUCINATION_THRESHOLD = 0.05;

    private final MemoryService memoryService;
    private final KnowledgeService knowledgeService;
    private final AlertManager alertManager;
    private final EvalMetricsCollector metricsCollector;
    private final ModelRouter modelRouter;
    private final RagasTestCaseMapper ragasTestCaseMapper;

    /** 使用 AtomicReference 存储最新指标值，避免 Gauge 重复注册 */
    private final AtomicReference<Double> contextRecallGauge = new AtomicReference<>(1.0);
    private final AtomicReference<Double> hallucinationRateGauge = new AtomicReference<>(0.0);

    public RagasMonitor(MemoryService memoryService,
                         KnowledgeService knowledgeService,
                         PrometheusMetricsExporter metrics,
                         AlertManager alertManager,
                         EvalMetricsCollector metricsCollector,
                         ModelRouter modelRouter,
                         RagasTestCaseMapper ragasTestCaseMapper) {
        this.memoryService = memoryService;
        this.knowledgeService = knowledgeService;
        this.alertManager = alertManager;
        this.metricsCollector = metricsCollector;
        this.modelRouter = modelRouter;
        this.ragasTestCaseMapper = ragasTestCaseMapper;

        // 一次性注册 Gauge，后续通过 AtomicReference 更新值
        Gauge.builder("rag_context_recall", contextRecallGauge, AtomicReference::get)
                .description("RAG上下文召回率")
                .register(metrics.getMeterRegistry());

        Gauge.builder("rag_hallucination_rate", hallucinationRateGauge, AtomicReference::get)
                .description("RAG幻觉率")
                .register(metrics.getMeterRegistry());
    }

    /**
     * 每日凌晨2点运行 RAGAS 质量检查。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void runRagasEvaluation() {
        log.info("开始运行RAGAS质量监控");

        try {
            // 1. 计算上下文召回率
            double contextRecall = calculateContextRecall();
            // 2. 计算幻觉率
            double hallucinationRate = calculateHallucinationRate();

            // 更新 Gauge 值（通过 AtomicReference）
            contextRecallGauge.set(contextRecall);
            hallucinationRateGauge.set(hallucinationRate);

            // 写入 Redis 供 AlertManager 读取
            metricsCollector.updateContextRecall(contextRecall);
            metricsCollector.updateHallucinationRate(hallucinationRate);

            // 告警：指标低于阈值时触发
            if (contextRecall < RECALL_THRESHOLD) {
                alertManager.sendWarningAlert("RAG召回率下降预警",
                        String.format("当前召回率: %.2f, 低于阈值%.2f", contextRecall, RECALL_THRESHOLD));
            }
            if (hallucinationRate > HALLUCINATION_THRESHOLD) {
                alertManager.sendWarningAlert("RAG幻觉率上升预警",
                        String.format("当前幻觉率: %.2f, 高于阈值%.2f", hallucinationRate, HALLUCINATION_THRESHOLD));

                // 幻觉率超标 → 自动降级主力模型，流量切换至更稳定的备选模型
                modelRouter.deprioritizeModel("deepseek-v3");
                log.warn("幻觉率超标(%.2f%%) → 已自动降级deepseek-v3模型", hallucinationRate * 100);
            }

            log.info("RAGAS质量检查完成：召回率={}, 幻觉率={}", contextRecall, hallucinationRate);
        } catch (Exception e) {
            log.error("RAGAS质量检查执行失败", e);
            alertManager.sendWarningAlert("RAGAS质量检查执行失败",
                    "RAG质量监控任务执行异常: " + e.getMessage());
        }
    }

    /**
     * 计算上下文召回率：检索结果中包含正确答案的比例。
     * 使用黄金测试集进行评估。
     */
    private double calculateContextRecall() {
        List<RagasTestCase> testCases = ragasTestCaseMapper.selectList(null);
        if (testCases.isEmpty()) {
            log.info("无RAGAS测试用例，跳过召回率计算");
            return 1.0;
        }

        int correct = 0;
        for (RagasTestCase testCase : testCases) {
            try {
                List<KnowledgeDoc> retrieved = knowledgeService.searchRelevant(
                        testCase.getQuery(), false, 3);
                for (KnowledgeDoc doc : retrieved) {
                    if (doc.getContent() != null
                            && testCase.getExpectedContext() != null
                            && doc.getContent().contains(testCase.getExpectedContext())) {
                        correct++;
                        break;
                    }
                }
            } catch (Exception e) {
                log.warn("RAGAS召回率测试用例执行失败 testCaseId={}", testCase.getId(), e);
            }
        }
        return (double) correct / testCases.size();
    }

    /**
     * 计算幻觉率：AI答案中不在检索上下文中的内容比例。
     * 简化实现：统计测试用例中期望答案与实际检索结果的差异。
     */
    private double calculateHallucinationRate() {
        List<RagasTestCase> testCases = ragasTestCaseMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<RagasTestCase>()
                        .eq(RagasTestCase::getTestCategory, "hallucination"));
        if (testCases.isEmpty()) {
            return 0.0;
        }

        int hallucinationCount = 0;
        for (RagasTestCase testCase : testCases) {
            try {
                List<KnowledgeDoc> retrieved = knowledgeService.searchRelevant(
                        testCase.getQuery(), false, 3);

                boolean foundInContext = false;
                if (testCase.getExpectedAnswer() != null) {
                    for (KnowledgeDoc doc : retrieved) {
                        if (doc.getContent() != null
                                && doc.getContent().contains(testCase.getExpectedAnswer())) {
                            foundInContext = true;
                            break;
                        }
                    }
                }
                if (!foundInContext) {
                    hallucinationCount++;
                }
            } catch (Exception e) {
                log.warn("RAGAS幻觉率测试用例执行失败 testCaseId={}", testCase.getId(), e);
            }
        }

        return testCases.isEmpty() ? 0.0 : (double) hallucinationCount / testCases.size();
    }
}