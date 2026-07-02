package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 异步任务线程池配置。
 * 为不同业务场景提供独立线程池，避免互相影响。
 */
@Configuration
public class AsyncConfig {

    /**
     * 自动计划调整专用线程池。
     * 用于 AutoPlanAdjustService 中的各类自动调整任务（睡眠不足、体重变化、难度反馈等）。
     * 独立线程池避免与系统其他 @Async 任务争抢资源。
     */
    @Bean("planAdjustExecutor")
    public Executor planAdjustExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("plan-adjust-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.initialize();
        return executor;
    }
}
