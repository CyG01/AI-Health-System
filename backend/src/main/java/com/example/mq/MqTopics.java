package com.example.mq;

/**
 * RocketMQ Topic 常量定义。
 */
public final class MqTopics {

    private MqTopics() {}

    /** 健康报告生成 */
    public static final String HEALTH_REPORT_GENERATE = "health-report-generate";

    /** AI 计划生成 */
    public static final String AI_PLAN_GENERATE = "ai-plan-generate";

    /** 知识库索引构建 */
    public static final String KNOWLEDGE_INDEX_BUILD = "knowledge-index-build";

    /** LLM 对话响应 */
    public static final String LLM_CHAT_RESPONSE = "llm-chat-response";

    /** 健康提醒推送 */
    public static final String HEALTH_REMINDER = "health-reminder";

    /** 多模态处理任务 */
    public static final String MULTIMODAL_PROCESS = "multimodal-process";

    /** 隐私数据焚毁任务 */
    public static final String PRIVACY_DATA_PURGE = "privacy-data-purge";

    // --- 死信队列 Topic ---

    /** 健康报告生成 — 死信 */
    public static final String HEALTH_REPORT_GENERATE_DLQ = "health-report-generate-dlq";

    /** AI 计划生成 — 死信 */
    public static final String AI_PLAN_GENERATE_DLQ = "ai-plan-generate-dlq";

    /** 知识库索引构建 — 死信 */
    public static final String KNOWLEDGE_INDEX_BUILD_DLQ = "knowledge-index-build-dlq";

    /** LLM 对话响应 — 死信 */
    public static final String LLM_CHAT_RESPONSE_DLQ = "llm-chat-response-dlq";

    // --- 消费者组 ---

    public static final String REPORT_CONSUMER_GROUP = "report-consumers";
    public static final String PLAN_CONSUMER_GROUP = "plan-consumers";
    public static final String KNOWLEDGE_CONSUMER_GROUP = "knowledge-consumers";
    public static final String CHAT_CONSUMER_GROUP = "chat-consumers";
    public static final String IDEMPOTENT_CONSUMER = "idempotent-%s";   // 动态 taskId
}