package com.example.service;

import com.example.llmops.PrometheusMetricsExporter;
import com.example.mapper.LlmCostLogMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 成本统计与数据清理定时任务（Phase 4）。
 *
 * 定时任务：
 * 1. 每分钟同步成本数据到 Prometheus（供 Grafana 面板实时展示）
 * 2. 每日凌晨清理 90 天前的成本日志
 * 3. 每月 1 号凌晨清理 3 个月前的 MySQL 时序表副本（数据边界）
 */
@Component
public class CostStatsScheduler {

    private static final Logger log = LoggerFactory.getLogger(CostStatsScheduler.class);

    private final LlmCostLogMapper costLogMapper;
    private final PrometheusMetricsExporter metricsExporter;

    public CostStatsScheduler(LlmCostLogMapper costLogMapper,
                               PrometheusMetricsExporter metricsExporter) {
        this.costLogMapper = costLogMapper;
        this.metricsExporter = metricsExporter;
    }

    /**
     * 每分钟同步当日成本到 Prometheus。
     */
    @Scheduled(fixedRate = 60_000)
    public void syncCostToPrometheus() {
        try {
            BigDecimal dailyCost = costLogMapper.getGlobalDailyCost();
            if (dailyCost != null) {
                // 转换为纳元（精确到小数点后9位）
                long costNanos = dailyCost.multiply(new BigDecimal("1000000000")).longValue();
                metricsExporter.setTotalCostDaily(costNanos);
            }
        } catch (Exception e) {
            log.debug("同步成本到 Prometheus 失败（可能数据库未就绪）", e);
        }
    }

    /**
     * 每日凌晨 2:00 清理 90 天前的 LLM 成本日志。
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupOldCostLogs() {
        log.info("开始清理 90 天前的 LLM 成本日志");
        try {
            int deleted = costLogMapper.deleteOldLogs(LocalDateTime.now().minusDays(90));
            log.info("LLM 成本日志清理完成 deleted={}", deleted);
        } catch (Exception e) {
            log.error("清理 LLM 成本日志失败", e);
        }
    }

    /**
     * 每月 1 号凌晨 3:00 清理 3 个月前的 MySQL 时序表副本。
     * 数据边界：MySQL 仅保留业务数据，3 个月后下线时序表的 MySQL 副本。
     */
    @Scheduled(cron = "0 0 3 1 * ?")
    public void cleanupOldTimeSeriesData() {
        log.info("开始清理 3 个月前的 MySQL 时序表副本");
        try {
            LocalDateTime before = LocalDateTime.now().minusMonths(3);

            // 注意：以下是 MySQL 时序表副本清理（仅当 TDengine 已替代时执行）
            // 实际清理 SQL 需要在生产环境确认后执行
            // DELETE FROM blood_sugar WHERE create_time < #{before}
            // DELETE FROM sleep_record WHERE create_time < #{before}
            // DELETE FROM body_measurement WHERE create_time < #{before}
            // DELETE FROM water_record WHERE create_time < #{before}
            // DELETE FROM exercise_record WHERE create_time < #{before}
            // DELETE FROM diet_record WHERE create_time < #{before}

            log.info("MySQL 时序表副本清理标记已触发 before={}", before);
        } catch (Exception e) {
            log.error("清理 MySQL 时序表副本失败", e);
        }
    }
}