package com.example.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 订阅等级校验注解。
 * 用于限制高阶功能仅付费用户可用。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresSubscription {

    /** 最低订阅等级：free(免费) / pro / enterprise */
    String value() default "free";

    /** 功能名称（用于错误提示） */
    String feature() default "";
}