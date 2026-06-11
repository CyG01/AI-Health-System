package com.example.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * LLM 调用成本追踪日志（Phase 4：成本精细化）。
 *
 * 按用户 × 意图 × 模型维度记录每次 LLM 调用的 Token 消耗和费用。
 * 支撑 Grafana 成本面板、单用户日预算告警（>1 元自动暂停）。
 */
@TableName("llm_cost_log")
public class LlmCostLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 意图分类：chitchat/food_recognize/plan_generate/medical_analysis/safety_check 等 */
    private String intent;

    /** 模型名称：deepseek-chat/qwen-turbo/ollama-llama3 等 */
    private String modelName;

    /** 模型层级：LOW/MEDIUM/HIGH/CRITICAL */
    private String modelTier;

    /** 输入 Token 数 */
    private Integer inputTokens;

    /** 输出 Token 数 */
    private Integer outputTokens;

    /** 输入成本（元） */
    private BigDecimal inputCost;

    /** 输出成本（元） */
    private BigDecimal outputCost;

    /** 总成本（元） */
    private BigDecimal totalCost;

    /** 调用延迟（毫秒） */
    private Integer latencyMs;

    /** 是否成功：1=成功 0=失败 */
    private Integer success;

    /** 失败原因 */
    private String errorMsg;

    /** 创建时间 */
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getIntent() { return intent; }
    public void setIntent(String intent) { this.intent = intent; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getModelTier() { return modelTier; }
    public void setModelTier(String modelTier) { this.modelTier = modelTier; }
    public Integer getInputTokens() { return inputTokens; }
    public void setInputTokens(Integer inputTokens) { this.inputTokens = inputTokens; }
    public Integer getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Integer outputTokens) { this.outputTokens = outputTokens; }
    public BigDecimal getInputCost() { return inputCost; }
    public void setInputCost(BigDecimal inputCost) { this.inputCost = inputCost; }
    public BigDecimal getOutputCost() { return outputCost; }
    public void setOutputCost(BigDecimal outputCost) { this.outputCost = outputCost; }
    public BigDecimal getTotalCost() { return totalCost; }
    public void setTotalCost(BigDecimal totalCost) { this.totalCost = totalCost; }
    public Integer getLatencyMs() { return latencyMs; }
    public void setLatencyMs(Integer latencyMs) { this.latencyMs = latencyMs; }
    public Integer getSuccess() { return success; }
    public void setSuccess(Integer success) { this.success = success; }
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}