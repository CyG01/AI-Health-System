package com.example.service;

import com.example.entity.KnowledgeDoc;

import java.util.List;

public interface KnowledgeService {

    /**
     * 检索相关知识文档（带分级过滤）
     * @param queryText 查询文本
     * @param isMedicalCore 是否为医疗核心问题（涉及疾病/药物/诊断/康复）
     * @param topK 返回数量
     */
    List<KnowledgeDoc> searchRelevant(String queryText, boolean isMedicalCore, int topK);

    /**
     * 判断查询是否为医疗核心问题
     */
    boolean isMedicalCoreQuestion(String queryText);

    /**
     * 将检索到的知识文档拼接为引用上下文
     */
    String buildKnowledgeContext(List<KnowledgeDoc> docs);

    /**
     * 导入权威文档（自动生成向量）
     */
    KnowledgeDoc importDocument(String title, String content, String category,
                                 String sourceName, String authorityLevel, String version);

    /**
     * 生成文本的向量表示（调用 DeepSeek Embedding API）
     */
    float[] generateEmbedding(String text);
}