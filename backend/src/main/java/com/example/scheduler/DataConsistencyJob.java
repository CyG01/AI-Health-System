package com.example.scheduler;

import com.example.llmops.AlertManager;
import com.example.llmops.WebhookNotifier;
import com.example.mapper.BloodSugarMapper;
import com.example.mapper.SysUserMapper;
import com.example.tsdb.TSDBConnectionPool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 数据一致性校验定时任务。
 *
 * 每日凌晨 2:00 执行，抽样对比前一天的 MySQL 与 TDengine 数据是否一致。
 * 若发现不一致，立即通过 AlertManager 发送告警，并将该用户查询切换回 MySQL。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataConsistencyJob {

    private final BloodSugarMapper bloodSugarMapper;
    private final SysUserMapper userMapper;
    private final TSDBConnectionPool tsdbPool;
    private final AlertManager alertManager;
    private final WebhookNotifier webhookNotifier;

    @Value("${tdengine.consistency.sample-size:100}")
    private int sampleSize;

    @Value("${tdengine.consistency.tolerance:0.1}")
    private double tolerance;

    /** 不一致用户黑名单（查询走 MySQL，直到次日校验通过才恢复） */
    private final List<Long> inconsistentUsers = new ArrayList<>();

    /**
     * 每日凌晨 2:00 执行数据一致性校验。
     */
    @Scheduled(cron = "${tdengine.consistency.cron:0 0 2 * * ?}")
    public void checkConsistency() {
        if (!tsdbPool.isAvailable()) {
            log.info("TDengine not available, skipping data consistency check");
            return;
        }

        LocalDate yesterday = LocalDate.now().minusDays(1);
        log.info("Starting data consistency check for {} (sample size: {})", yesterday, sampleSize);

        // 抽样用户
        List<Long> userIds = sampleUserIds(sampleSize);
        if (userIds.isEmpty()) {
            log.info("No active users to check");
            return;
        }

        AtomicInteger totalChecked = new AtomicInteger(0);
        AtomicInteger inconsistentCount = new AtomicInteger(0);
        List<String> inconsistencyDetails = new ArrayList<>();

        for (Long userId : userIds) {
            try {
                totalChecked.incrementAndGet();

                // 1. 血糖日均值对比
                checkBloodSugarConsistency(userId, yesterday, inconsistencyDetails, inconsistentCount);

                // 2. 体重日均值对比
                checkWeightConsistency(userId, yesterday, inconsistencyDetails, inconsistentCount);

                // 3. 运动卡路里对比
                checkExerciseCaloriesConsistency(userId, yesterday, inconsistencyDetails, inconsistentCount);

                // 4. 饮食卡路里对比
                checkDietCaloriesConsistency(userId, yesterday, inconsistencyDetails, inconsistentCount);

            } catch (Exception e) {
                log.warn("Consistency check failed for userId={}: {}", userId, e.getMessage());
            }
        }

        // 汇总日志
        log.info("Data consistency check completed. Total={}, Inconsistent={}",
                totalChecked.get(), inconsistentCount.get());

        // 发送告警
        if (inconsistentCount.get() > 0) {
            String alertMsg = String.format(
                    "数据一致性校验发现 %d 处不一致（共检查 %d 个用户）\n%s",
                    inconsistentCount.get(), totalChecked.get(),
                    String.join("\n", inconsistencyDetails));
            log.error("[数据一致性告警] {}", alertMsg);
            try {
                alertManager.sendWarningAlert("数据不一致", alertMsg);
            } catch (Exception e) {
                log.warn("发送一致性告警失败: {}", e.getMessage());
            }
            try {
                webhookNotifier.sendAlert("WARNING", "data_consistency", alertMsg);
            } catch (Exception e) {
                log.warn("Webhook通知发送失败: {}", e.getMessage());
            }
        } else {
            // 校验全部通过，清理黑名单
            inconsistentUsers.clear();
            log.info("All consistency checks passed for {}", yesterday);
        }
    }

    /**
     * 检查血糖数据一致性。
     */
    private void checkBloodSugarConsistency(Long userId, LocalDate date,
                                              List<String> details, AtomicInteger inconsistentCount) {
        BigDecimal mysqlAvg = bloodSugarMapper.getDailyAvg(userId, date);
        BigDecimal tdengineAvg = tsdbPool.getDailyAvgBloodSugar(userId, date);

        if (mysqlAvg == null && tdengineAvg == null) {
            return; // 都无数据，一致
        }
        if (mysqlAvg == null || tdengineAvg == null) {
            inconsistentCount.incrementAndGet();
            String detail = String.format("用户%d 血糖数据缺失：MySQL=%s, TDengine=%s",
                    userId,
                    mysqlAvg != null ? mysqlAvg.setScale(2, RoundingMode.HALF_UP).toString() : "NULL",
                    tdengineAvg != null ? tdengineAvg.setScale(2, RoundingMode.HALF_UP).toString() : "NULL");
            details.add("- " + detail);
            addToInconsistentList(userId);
            return;
        }

        double diff = Math.abs(mysqlAvg.subtract(tdengineAvg).doubleValue());
        if (diff > tolerance) {
            inconsistentCount.incrementAndGet();
            String detail = String.format("用户%d 血糖数据不一致：MySQL=%.2f, TDengine=%.2f, 差异=%.3f",
                    userId,
                    mysqlAvg.setScale(2, RoundingMode.HALF_UP),
                    tdengineAvg.setScale(2, RoundingMode.HALF_UP),
                    diff);
            details.add("- " + detail);
            addToInconsistentList(userId);
        }
    }

    /**
     * 检查体重数据一致性。
     */
    private void checkWeightConsistency(Long userId, LocalDate date,
                                         List<String> details, AtomicInteger inconsistentCount) {
        BigDecimal mysqlAvg = bloodSugarMapper.getDailyAvgWeight(userId, date);
        BigDecimal tdengineAvg = tsdbPool.getDailyAvgWeight(userId, date);

        if (mysqlAvg == null && tdengineAvg == null) return;
        if (mysqlAvg == null || tdengineAvg == null) {
            inconsistentCount.incrementAndGet();
            details.add("- 用户" + userId + " 体重数据缺失");
            addToInconsistentList(userId);
            return;
        }

        double diff = Math.abs(mysqlAvg.subtract(tdengineAvg).doubleValue());
        if (diff > 0.5) { // 体重容忍度 0.5kg
            inconsistentCount.incrementAndGet();
            details.add("- 用户" + userId + " 体重数据不一致：MySQL=" +
                    mysqlAvg.setScale(2, RoundingMode.HALF_UP) +
                    ", TDengine=" + tdengineAvg.setScale(2, RoundingMode.HALF_UP));
            addToInconsistentList(userId);
        }
    }

    /**
     * 检查运动卡路里一致性。
     */
    private void checkExerciseCaloriesConsistency(Long userId, LocalDate date,
                                                    List<String> details, AtomicInteger inconsistentCount) {
        BigDecimal mysqlCal = bloodSugarMapper.getDailyExerciseCalories(userId, date);
        BigDecimal tdengineCal = tsdbPool.getDailyExerciseCalories(userId, date);

        if (mysqlCal == null && tdengineCal == null) return;
        if (mysqlCal == null || tdengineCal == null) {
            inconsistentCount.incrementAndGet();
            details.add("- 用户" + userId + " 运动数据缺失");
            addToInconsistentList(userId);
            return;
        }

        double diff = Math.abs(mysqlCal.subtract(tdengineCal).doubleValue());
        if (diff > 10) { // 卡路里容忍度 10kcal
            inconsistentCount.incrementAndGet();
            details.add("- 用户" + userId + " 运动卡路里不一致：MySQL=" +
                    mysqlCal.setScale(0, RoundingMode.HALF_UP) +
                    ", TDengine=" + tdengineCal.setScale(0, RoundingMode.HALF_UP));
            addToInconsistentList(userId);
        }
    }

    /**
     * 检查饮食卡路里一致性。
     */
    private void checkDietCaloriesConsistency(Long userId, LocalDate date,
                                               List<String> details, AtomicInteger inconsistentCount) {
        BigDecimal mysqlCal = bloodSugarMapper.getDailyDietCalories(userId, date);
        BigDecimal tdengineCal = tsdbPool.getDailyDietCalories(userId, date);

        if (mysqlCal == null && tdengineCal == null) return;
        if (mysqlCal == null || tdengineCal == null) {
            inconsistentCount.incrementAndGet();
            details.add("- 用户" + userId + " 饮食数据缺失");
            addToInconsistentList(userId);
            return;
        }

        double diff = Math.abs(mysqlCal.subtract(tdengineCal).doubleValue());
        if (diff > 10) { // 卡路里容忍度 10kcal
            inconsistentCount.incrementAndGet();
            details.add("- 用户" + userId + " 饮食卡路里不一致：MySQL=" +
                    mysqlCal.setScale(0, RoundingMode.HALF_UP) +
                    ", TDengine=" + tdengineCal.setScale(0, RoundingMode.HALF_UP));
            addToInconsistentList(userId);
        }
    }

    /**
     * 抽样获取活跃用户 ID。
     */
    private List<Long> sampleUserIds(int count) {
        try {
            return userMapper.sampleActiveUserIds(count);
        } catch (Exception e) {
            log.warn("Failed to sample user IDs: {}", e.getMessage());
            // 降级：使用 BloodSugarMapper 抽样
            try {
                return bloodSugarMapper.sampleUserIds(count);
            } catch (Exception ex) {
                log.error("Failed to sample user IDs from blood_sugar: {}", ex.getMessage());
                return List.of();
            }
        }
    }

    /**
     * 将用户加入不一致黑名单。
     */
    private void addToInconsistentList(Long userId) {
        if (!inconsistentUsers.contains(userId)) {
            inconsistentUsers.add(userId);
        }
    }

    /**
     * 判断用户是否在不一致黑名单中（查询应走 MySQL）。
     */
    public boolean isUserInconsistent(Long userId) {
        return inconsistentUsers.contains(userId);
    }

    /**
     * 手动清理黑名单（外部 API 调用）。
     */
    public void clearInconsistentList() {
        inconsistentUsers.clear();
        log.info("Inconsistent user blacklist cleared");
    }
}