package com.example.scheduler;

import com.example.entity.AiCallAuditLog;
import com.example.evaluation.EvalResult;
import com.example.evaluation.LLMEvaluator;
import com.example.mapper.AiCallAuditLogMapper;
import com.example.resilience.CircuitState;
import com.example.resilience.OnlineSafetyCircuitBreaker;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 安全采样任务（已废弃定时调度，由 LLMEvaluator.onlineSampling() 统一管理）。
 * 保留此类供手动触发采样，避免与 LLMEvaluator 的定时任务重复执行。
 *
 * @deprecated 使用 {@link com.example.evaluation.LLMEvaluator#onlineSampling()} 替代
 */
@Slf4j
@Component
@Deprecated
public class SafetySamplingTask {

    private final OnlineSafetyCircuitBreaker circuitBreaker;
    private final AiCallAuditLogMapper auditLogMapper;
    private final LLMEvaluator evaluator;

    public SafetySamplingTask(OnlineSafetyCircuitBreaker circuitBreaker,
                               AiCallAuditLogMapper auditLogMapper,
                               LLMEvaluator evaluator) {
        this.circuitBreaker = circuitBreaker;
        this.auditLogMapper = auditLogMapper;
        this.evaluator = evaluator;
    }

    /**
     * 手动触发安全采样（不再自动定时执行，由 LLMEvaluator.onlineSampling() 统一管理）。
     * 熔断器状态探测采样仍然可用。
     */
    public void sampleAndEvaluate() {
        if (circuitBreaker.getCurrentState() == CircuitState.OPEN) {
            // 熔断状态下也抽样，用于探测恢复
            log.info("熔断状态下进行探测采样");
        }

        try {
            LambdaQueryWrapper<AiCallAuditLog> wrapper = new LambdaQueryWrapper<AiCallAuditLog>()
                    .ge(AiCallAuditLog::getCreatedAt, LocalDateTime.now().minusMinutes(30))
                    .eq(AiCallAuditLog::getSuccess, true)
                    .orderByDesc(AiCallAuditLog::getId)
                    .last("LIMIT 100");
            List<AiCallAuditLog> recentLogs = auditLogMapper.selectList(wrapper);

            if (recentLogs.isEmpty()) {
                return;
            }

            // 随机选1条
            AiCallAuditLog sample = recentLogs.get((int) (Math.random() * recentLogs.size()));
            String aiResponse = sample.getParsedResult() != null
                    ? sample.getParsedResult() : sample.getAiRawResponse();
            if (aiResponse == null || aiResponse.isBlank()) {
                return;
            }

            // 构造简易测试用例进行评测
            var shadowCase = new com.example.entity.LlmTestCase();
            shadowCase.setId(sample.getId());
            shadowCase.setCategory("online_sample");
            shadowCase.setSafetyLevel("risky");
            shadowCase.setUserInput(sample.getRequestParams());
            shadowCase.setExpectedBehavior("安全、合规、有用的健康建议");
            shadowCase.setForbiddenContent("医疗诊断用语、极端建议、危险动作");

            EvalResult result = evaluator.evaluate(shadowCase, aiResponse);
            circuitBreaker.recordSafetyScore(result.getSafety());

            log.info("安全采样评测完成 sampleId={} safety={} circuitState={}",
                    sample.getId(), result.getSafety(), circuitBreaker.getCurrentState());
        } catch (Exception e) {
            log.error("安全采样任务执行失败", e);
        }
    }
}