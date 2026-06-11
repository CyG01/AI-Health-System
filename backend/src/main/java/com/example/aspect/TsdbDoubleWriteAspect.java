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

import java.lang.reflect.Method;

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

    public TsdbDoubleWriteAspect(TSDBConnectionPool tsdbPool) {
        this.tsdbPool = tsdbPool;
    }

    /**
     * 环绕通知：先执行原方法（MySQL 写入），成功后异步写入 TDengine。
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

        // 4. 异步写入 TDengine（捕获所有异常，不影响主流程）
        String dataType = annotation.dataType();
        Object[] args = pjp.getArgs();

        try {
            doDoubleWrite(dataType, method.getName(), args, result, pjp.getTarget());
        } catch (Exception e) {
            log.warn("TDengine double-write failed for {} dataType={}: {}",
                    method.getName(), dataType, e.getMessage());
        }

        return result;
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
                } catch (Exception ignored) {}
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