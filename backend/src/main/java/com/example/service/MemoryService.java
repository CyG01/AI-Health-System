package com.example.service;

import com.example.entity.UserMemory;

import java.util.List;

public interface MemoryService {

    /**
     * 存储一条用户记忆（自动生成向量）
     */
    UserMemory store(Long userId, String content, String memoryType, int importance, String source);

    /**
     * 检索与查询文本最相关的 Top-K 记忆（语义相似度）
     */
    List<UserMemory> retrieveRelevant(Long userId, String queryText, int topK);

    /**
     * 获取用户所有高重要性记忆（importance ≥ 7）
     */
    List<UserMemory> getHighImportance(Long userId);

    /**
     * 将相关记忆拼接为自然语言上下文，注入到 AI Prompt 中
     */
    String buildMemoryContext(Long userId, String queryText, int topK);

    /**
     * 清理低重要性旧记忆（90天未访问且importance < 3）
     * 每日凌晨3点自动执行
     */
    int cleanupStaleMemories();

    /**
     * 从用户自然语言内容自动提取并存储记忆
     */
    void autoCollect(Long userId, String content, String source);
}