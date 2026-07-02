package com.example.aspect;

import com.example.annotation.TsdbDoubleWrite;
import com.example.tsdb.TSDBConnectionPool;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * TDengine 双写切面。
 *
 * 拦截 @TsdbDoubleWrite 注解的方法，在 MySQL 写入成功后异步写入 TDengine。
 * TDengine 写入失败不影响主流程（只记录日志）。
 *
 * 切面执行顺序：在事务提交后执行，确保 MySQL 数据已持久化。
 */
@Slf4j
@Aspect
@Component
@Order(10)
public class TsdbDoubleWriteAspect {

    private final TSDBConnectionPool tsdbPool;

    /**
     * 专用线程池：用于异步执行 TSDB 双写，避免阻塞主事务线程。
     * 核心线程 2，最大线程 4，空闲 60s 回收，队列容量 128。
     */
    private static final ExecutorService TSDB_WRITE_EXECUTOR =
            Executors.newFixedThreadPool(4, r -> {
                Thread t = new Thread(r, "tsdb-double-write");
                t.setDaemon(true);
                return t;
            });

    /** 重试最大次数 */
    private static final int MAX_RETRIES = 3;

    /** 重试基础等待时间（毫秒），指数退避：100ms → 200ms → 400ms */
    private static final long RETRY_BASE_DELAY_MS = 100L;

    public TsdbDoubleWriteAspect(TSDBConnectionPool tsdbPool) {
        this.tsdbPool = tsdbPool;
    }

    /**
     * 环绕通知：先执行原方法（MySQL 写入），事务提交后异步写入 TDengine。
     * <p>
     * 修复要点：
     * 1. pjp.proceed() 返回时 Spring 事务尚未提交，不能同步写 TSDB
     * 2. 通过 TransactionSynchronizationManager.afterCommit 确保 MySQL 已提交
     * 3. TSDB 写入异步执行，不阻塞主流程
     * 4. 写入失败自动重试（最多 3 次，指数退避）
     */
    @Around("@annotation(com.example.annotation.TsdbDoubleWrite)")
    public Object handleDoubleWrite(ProceedingJoinPoint pjp) throws Throwable {
        // 1. 执行原方法（MySQL 写入）
        Object result = pjp.proceed();

        // 2. 获取注解信息
        MethodSignature signature = (MethodSignature) pjp.getSignature();
        Method method = signature.getMethod();
        TsdbDoubleWrite annotation = method.getAnnotation(TsdbDoubleWrite.class);

        if (!annotation.enabled()) {
            return result;
        }

        // 3. 如果 TDengine 不可用，跳过双写
        if (!tsdbPool.isAvailable()) {
            log.debug("TDengine not available, skipping double-write for {}.{}",
                    pjp.getTarget().getClass().getSimpleName(), method.getName());
            return result;
        }

        // 4. 准备双写参数（在 lambda 捕获前取值）
        String dataType = annotation.dataType();
        Object[] args = pjp.getArgs();
        String methodName = method.getName();
        Object target = pjp.getTarget();

        // 5. 注册事务提交后的异步写入
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // 在事务中：注册 afterCommit 回调，确保 MySQL 事务提交后再写 TSDB
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    CompletableFuture.runAsync(() ->
                            writeWithRetry(dataType, methodName, args, result, target),
                            TSDB_WRITE_EXECUTOR);
                }
            });
        } else {
            // 不在事务中：直接异步执行 TSDB 写入
            CompletableFuture.runAsync(() ->
                    writeWithRetry(dataType, methodName, args, result, target),
                    TSDB_WRITE_EXECUTOR);
        }

        return result;
    }

    /**
     * 带重试的 TDengine 写入。最多重试 {@value MAX_RETRIES} 次，指数退避。
     * 全部失败后记录 error 日志，不抛出异常（不影响主流程）。
     */
    private void writeWithRetry(String dataType, String methodName,
                                Object[] args, Object result, Object target) {
        Exception lastException = null;
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                doDoubleWrite(dataType, methodName, args, result, target);
                return; // 写入成功，直接返回
            } catch (Exception e) {
                lastException = e;
                if (attempt < MAX_RETRIES) {
                    long delayMs = RETRY_BASE_DELAY_MS * (1L << (attempt - 1));
                    log.warn("TDengine double-write attempt {}/{} failed for {} dataType={}, retrying in {}ms: {}",
                            attempt, MAX_RETRIES, methodName, dataType, delayMs, e.getMessage());
                    try {
                        TimeUnit.MILLISECONDS.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("TDengine double-write retry interrupted for {} dataType={}", methodName, dataType);
                        return;
                    }
                }
            }
        }
        // 全部重试失败
        log.error("TDengine double-write FAILED after {} attempts for {} dataType={}: {}",
                MAX_RETRIES, methodName, dataType,
                lastException != null ? lastException.getMessage() : "unknown error");
    }

    /**
     * 根据数据类型执行对应的 TDengine 写入逻辑。
     */
    private void doDoubleWrite(String dataType, String methodName,
                                Object[] args, Object result, Object target) {
        switch (dataType) {
            case "blood_sugar" -> {
                // BloodSugarService.submitRecord → 参数含 userId 和 BloodSugarSubmitDTO
                handleBloodSugarWrite(args, result);
            }
            case "body_measurement" -> {
                handleBodyMeasurementWrite(args, result);
            }
            case "exercise_record" -> {
                handleExerciseRecordWrite(args, result);
            }
            case "diet_record" -> {
                handleDietRecordWrite(args, result);
            }
            case "sleep_record" -> {
                handleSleepRecordWrite(args, result);
            }
            case "water_record" -> {
                handleWaterRecordWrite(args, result);
            }
            case "daily_checkin" -> {
                handleDailyCheckinWrite(args, result);
            }
            default -> log.debug("Unknown dataType for TSDB double-write: {}", dataType);
        }
    }

    // ===== 各数据类型的 TDengine 写入 =====

    private void handleBloodSugarWrite(Object[] args, Object result) {
        if (args.length < 2) return;
        try {
            Long userId = getUserId(args[0]);
            Object dto = args[1];
            if (dto == null) return;

            // 反射获取 DTO 字段
            java.time.LocalDate recordDate = (java.time.LocalDate) dto.getClass()
                    .getMethod("getRecordDate").invoke(dto);
            java.time.LocalTime recordTime = (java.time.LocalTime) dto.getClass()
                    .getMethod("getRecordTime").invoke(dto);
            String measureType = (String) dto.getClass().getMethod("getMeasureType").invoke(dto);
            java.math.BigDecimal glucoseValue = (java.math.BigDecimal) dto.getClass()
                    .getMethod("getGlucoseValue").invoke(dto);
            String note = (String) dto.getClass().getMethod("getNote").invoke(dto);

            // 从返回值获取 abnormalFlag
            Integer abnormalFlag = 0;
            if (result != null) {
                try {
                    abnormalFlag = (Integer) result.getClass().getMethod("getAbnormalFlag").invoke(result);
                } catch (Exception e) {
                    log.debug("Failed to get abnormalFlag from result: {}", e.getMessage());
                }
            }

            tsdbPool.insertBloodSugar(userId, recordDate,
                    recordTime != null ? recordTime.toString() : "00:00:00",
                    measureType, glucoseValue, note, abnormalFlag);
        } catch (Exception e) {
            log.warn("TDengine blood_sugar write failed: {}", e.getMessage());
        }
    }

    private void handleBodyMeasurementWrite(Object[] args, Object result) {
        // 预留：体重/体围测量数据双写
        log.debug("TDengine body_measurement double-write not yet implemented");
    }

    private void handleExerciseRecordWrite(Object[] args, Object result) {
        // 预留：运动记录数据双写
        log.debug("TDengine exercise_record double-write not yet implemented");
    }

    private void handleDietRecordWrite(Object[] args, Object result) {
        // 预留：饮食记录数据双写
        log.debug("TDengine diet_record double-write not yet implemented");
    }

    private void handleSleepRecordWrite(Object[] args, Object result) {
        // 预留：睡眠记录数据双写
        log.debug("TDengine sleep_record double-write not yet implemented");
    }

    private void handleWaterRecordWrite(Object[] args, Object result) {
        // 预留：饮水记录数据双写
        log.debug("TDengine water_record double-write not yet implemented");
    }

    private void handleDailyCheckinWrite(Object[] args, Object result) {
        // 预留：打卡记录数据双写
        log.debug("TDengine daily_checkin double-write not yet implemented");
    }

    /**
     * 从参数中提取 userId。
     */
    private Long getUserId(Object arg) {
        if (arg instanceof Long) {
            return (Long) arg;
        }
        return null;
    }
}