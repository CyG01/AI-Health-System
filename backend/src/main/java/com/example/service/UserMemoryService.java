package com.example.service;

import com.example.entity.UserMemory;
import com.example.entity.HealthEventTimeline;

import java.util.List;
import java.util.Map;

/**
 * 用户长程记忆服务接口
 * 支持三层记忆架构：瞬时记忆(Session)、核心画像(Profile)、时间线线索(Timeline)
 */
public interface UserMemoryService {

    /**
     * 添加用户记忆
     *
     * @param userId      用户ID
     * @param memoryType  记忆类型
     * @param memoryLayer 记忆层级：SESSION/PROFILE/TIMELINE
     * @param content     记忆内容
     * @param importance  重要性 1-10
     * @param source      来源
     * @param tags        标签（逗号分隔）
     * @return 创建的记忆
     */
    UserMemory addMemory(Long userId, String memoryType, String memoryLayer,
                          String content, Integer importance, String source, String tags);

    /**
     * 获取用户的核心画像记忆（Profile层）
     *
     * @param userId 用户ID
     * @return 核心画像记忆列表
     */
    List<UserMemory> getProfileMemories(Long userId);

    /**
     * 获取用户的时间线记忆（Timeline层），带时间衰减排序
     *
     * @param userId 用户ID
     * @param limit  返回数量
     * @return 时间线记忆列表（按时间衰减得分排序）
     */
    List<UserMemory> getTimelineMemoriesWithDecay(Long userId, int limit);

    /**
     * 混合检索用户记忆：结合语义相似度和时间衰减
     *
     * @param userId        用户ID
     * @param queryText     查询文本
     * @param queryEmbedding 查询向量
     * @param topK          返回数量
     * @return 检索结果（带得分）
     */
    List<Map<String, Object> hybridSearchMemories(Long userId, String queryText,
                                                    float[] queryEmbedding, int topK);

    /**
     * 构建用户的完整记忆上下文（用于AI对话）
     *
     * @param userId 用户ID
     * @return 记忆上下文文本
     */
    String buildMemoryContext(Long userId);

    /**
     * 记录记忆访问（更新访问次数和最后访问时间）
     *
     * @param memoryId 记忆ID
     */
    void recordMemoryAccess(Long memoryId);

    /**
     * 清理过期的瞬时记忆（Session层）
     *
     * @param userId 用户ID
     * @return 清理的数量
     */
    int cleanupSessionMemories(Long userId);

    /**
     * 添加健康事件到时间线
     *
     * @param userId     用户ID
     * @param eventType  事件类型
     * @param title      事件标题
     * @param description 事件描述
     * @param eventDate  事件日期
     * @param severity   严重程度
     * @return 创建的事件
     */
    HealthEventTimeline addHealthEvent(Long userId, String eventType, String title,
                                        String description, java.time.LocalDate eventDate,
                                        String severity);

    /**
     * 获取用户健康事件时间线
     *
     * @param userId   用户ID
     * @param days     最近多少天
     * @return 事件列表
     */
    List<HealthEventTimeline> getHealthEventTimeline(Long userId, int days);

    /**
     * 物理删除用户所有记忆（隐私焚毁）
     *
     * @param userId 用户ID
     * @return 删除的数量
     */
    int purgeAllMemories(Long userId);
}
