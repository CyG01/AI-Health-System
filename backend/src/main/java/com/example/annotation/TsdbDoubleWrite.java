package com.example.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记需要同时写入 MySQL 和 TDengine 的方法。
 * 用于体征数据（血糖、血压、体重等时序数据）的双写过渡期。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface TsdbDoubleWrite {

    /**
     * 数据类型，用于路由到对应的 TDengine 超级表。
     * 例如：blood_sugar, body_measurement, sleep_record, exercise_record, water_record
     */
    String dataType() default "";

    /**
     * 是否启用（用于生产环境灰度开关控制）
     */
    boolean enabled() default true;
}