package com.example.resilience;

import com.example.common.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * AI调用异步队列 — 高峰流量削峰。
 *
 * 在高并发场景（早晚高峰）下，AI调用超过并发阈值时，
 * 将请求放入有界队列排队处理，避免瞬时峰值击穿下游 AI 服务。
 *
 * 设计要点：
 * - 核心线程池 + 有界队列 = 削峰填谷
 * - 队列满时快速失败，返回明确提示
 * - 提供队列深度/水位监控供运维观测
 */
@Slf4j
@Service
public class AiCallQueueService {

    /** 核心处理线程数 */
    private static final int CORE_THREADS = 8;

    /** 最大线程数 */
    private static final int MAX_THREADS = 16;

    /** 排队队列容量（削峰缓冲能力） */
    private static final int QUEUE_CAPACITY = 100;

    /** 单次排队最大等待时间（秒） */
    private static final int MAX_WAIT_SECONDS = 30;

    /** 当前排队请求数（监控指标） */
    private final AtomicInteger queuedCount = new AtomicInteger(0);

    /** 当前正在处理的请求数 */
    private final AtomicInteger processingCount = new AtomicInteger(0);

    private final ThreadPoolExecutor executor;

    public AiCallQueueService() {
        this.executor = new ThreadPoolExecutor(
                CORE_THREADS,
                MAX_THREADS,
                60, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                r -> {
                    Thread t = new Thread(r, "ai-call-worker");
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy()  // 队列满时由调用线程执行，天然限流
        ) {
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                queuedCount.decrementAndGet();
                processingCount.incrementAndGet();
            }

            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                processingCount.decrementAndGet();
                if (t != null) {
                    log.error("AI调用队列任务异常", t);
                }
            }
        };
    }

    /**
     * 提交 AI 调用任务，带削峰保护。
     *
     * 流程：
     * 1. 当前并发 < 核心线程  → 立即执行
     * 2. 核心线程满，进入队列 → 排队等待
     * 3. 队列满              → 快速失败
     *
     * @param taskSupplier AI调用任务
     * @param taskName     任务名称（用于日志）
     * @param <T>          返回值类型
     * @return 任务执行结果
     */
    public <T> T submitWithPeakProtection(Supplier<T> taskSupplier, String taskName) {
        int currentQueueSize = executor.getQueue().size();

        // 队列接近满载告警
        if (currentQueueSize > QUEUE_CAPACITY * 0.8) {
            log.warn("AI调用队列水位告警 queueSize={}/{}, processing={}",
                    currentQueueSize, QUEUE_CAPACITY, processingCount.get());
        }

        // 队列满时快速失败
        if (currentQueueSize >= QUEUE_CAPACITY) {
            log.error("AI调用队列已满，拒绝请求 taskName={}", taskName);
            throw new BusinessException(503,
                    "当前AI服务请求量过大，请稍后再试。系统正在排队处理中。");
        }

        queuedCount.incrementAndGet();
        log.debug("AI调用任务入队 taskName={} queueSize={}", taskName, currentQueueSize + 1);

        try {
            Future<T> future = executor.submit(() -> {
                log.debug("AI调用任务开始执行 taskName={}", taskName);
                return taskSupplier.get();
            });

            return future.get(MAX_WAIT_SECONDS, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            log.error("AI调用排队超时 taskName={}, maxWait={}s", taskName, MAX_WAIT_SECONDS);
            throw new BusinessException("AI服务繁忙，排队超时，请稍后再试");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("AI调用被中断，请重试");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof BusinessException be) {
                throw be;
            }
            log.error("AI调用队列任务执行失败 taskName={}", taskName, cause);
            throw new BusinessException("AI服务处理失败: " + (cause != null ? cause.getMessage() : "未知错误"));
        }
    }

    /**
     * 获取队列状态（供运维监控使用）。
     */
    public QueueStatus getQueueStatus() {
        return new QueueStatus(
                executor.getActiveCount(),
                executor.getQueue().size(),
                QUEUE_CAPACITY,
                processingCount.get(),
                queuedCount.get(),
                executor.getCompletedTaskCount(),
                executor.getTaskCount()
        );
    }

    public record QueueStatus(
            int activeCount,
            int queueSize,
            int queueCapacity,
            int processingCount,
            int queuedCount,
            long completedTasks,
            long totalTasks
    ) {
        public boolean isUnderPressure() {
            return queueSize > queueCapacity * 0.7;
        }

        public double utilizationRate() {
            return queueCapacity > 0 ? (double) queueSize / queueCapacity : 0;
        }
    }
}