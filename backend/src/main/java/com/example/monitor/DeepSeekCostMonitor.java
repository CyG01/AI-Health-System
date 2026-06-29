package com.example.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

/**
 * DeepSeek 成本监控器（已废弃 — 委托给 MultiModelCostMonitor）。
 *
 * <p><b>迁移说明：</b>此类已被 {@link MultiModelCostMonitor} 取代。
 * 所有成本追踪、预算告警、数据库持久化功能统一由 MultiModelCostMonitor 提供。
 * 新代码应直接注入 {@link MultiModelCostMonitor}，而非此类。</p>
 *
 * <p>保留此类仅为兼容尚未迁移的旧调用方（17 处注入）。
 * 所有方法均委托至 MultiModelCostMonitor，不再维护独立的 Redis 统计。</p>
 *
 * @see MultiModelCostMonitor
 * @deprecated 使用 {@link MultiModelCostMonitor} 代替。
 *     迁移方式：将注入类型从 {@code DeepSeekCostMonitor} 改为 {@code MultiModelCostMonitor}，
 *     所有公共方法签名兼容，可直接替换。
 */
@Deprecated
@Component
public class DeepSeekCostMonitor {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekCostMonitor.class);

    private final MultiModelCostMonitor multiModelCostMonitor;

    @Autowired
    public DeepSeekCostMonitor(MultiModelCostMonitor multiModelCostMonitor) {
        this.multiModelCostMonitor = multiModelCostMonitor;
        log.info("DeepSeekCostMonitor 已废弃，所有功能委托至 MultiModelCostMonitor");
    }

    /**
     * @deprecated 使用 {@link MultiModelCostMonitor#recordCall(int, int, ModelTier)} 代替。
     */
    public void recordCall(int inputTokens, int outputTokens) {
        multiModelCostMonitor.recordCall(inputTokens, outputTokens, (ModelTier) null);
    }

    /**
     * @deprecated 使用 {@link MultiModelCostMonitor#recordCall(Long, String, String, ModelTier, int, int, long, boolean)} 代替。
     */
    public void recordCall(int inputTokens, int outputTokens, Long userId) {
        multiModelCostMonitor.recordCall(userId, null, "deepseek-chat", null,
                inputTokens, outputTokens, 0, true);
    }

    /**
     * @deprecated 使用 {@link MultiModelCostMonitor#recordCall(Long, String, String, ModelTier, int, int, long, boolean)} 代替。
     */
    public void recordCall(int inputTokens, int outputTokens, ModelTier tier) {
        String modelName = tier != null ? tier.getModelName() : "deepseek-chat";
        multiModelCostMonitor.recordCall(null, null, modelName, tier,
                inputTokens, outputTokens, 0, true);
    }

    /**
     * @deprecated 使用 {@link MultiModelCostMonitor#recordCall(Long, String, String, ModelTier, int, int, long, boolean)} 代替。
     */
    public void recordCall(int inputTokens, int outputTokens, ModelTier tier, Long userId) {
        String modelName = tier != null ? tier.getModelName() : "deepseek-chat";
        multiModelCostMonitor.recordCall(userId, null, modelName, tier,
                inputTokens, outputTokens, 0, true);
    }

    /**
     * @deprecated 使用 {@link MultiModelCostMonitor#isGlobalCostExceeded()} 代替。
     */
    public boolean isGlobalCostExceeded() {
        return multiModelCostMonitor.isGlobalCostExceeded();
    }

    /**
     * @deprecated 使用 {@link MultiModelCostMonitor#isUserCostExceeded(Long)} 代替。
     */
    public boolean isUserCostExceeded(Long userId) {
        return multiModelCostMonitor.isUserCostExceeded(userId);
    }

    /**
     * @deprecated 使用 {@link MultiModelCostMonitor#getUserDailyCost(Long)} 代替。
     */
    public BigDecimal getUserDailyCost(Long userId) {
        return multiModelCostMonitor.getUserDailyCost(userId);
    }

    /**
     * @deprecated 使用 {@link MultiModelCostMonitor#getAllTierCosts()} 代替。
     */
    public Map<String, BigDecimal> getAllTierCosts() {
        return multiModelCostMonitor.getAllTierCosts();
    }

    /**
     * @deprecated 使用 {@link MultiModelCostMonitor#getCurrentDailyCost()} 代替。
     */
    public BigDecimal getCurrentDailyCost() {
        return multiModelCostMonitor.getCurrentDailyCost();
    }

    /**
     * @deprecated 使用 {@link MultiModelCostMonitor#getRemainingBudget()} 代替。
     */
    public BigDecimal getRemainingBudget() {
        return multiModelCostMonitor.getRemainingBudget();
    }
}
