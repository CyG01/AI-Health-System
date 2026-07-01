package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.entity.HealthEventTimeline;
import com.example.entity.UserMemory;
import com.example.mapper.HealthEventTimelineMapper;
import com.example.mapper.UserMemoryMapper;
import com.example.service.UserMemoryService;
import com.example.vector.TimeDecayRrfFusion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 用户长程记忆服务实现类
 * 三层记忆架构：瞬时记忆(Session)、核心画像(Profile)、时间线线索(Timeline)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMemoryServiceImpl implements UserMemoryService {

    private final UserMemoryMapper userMemoryMapper;
    private final HealthEventTimelineMapper eventTimelineMapper;

    // 衰减系数：7天半衰期（近期记忆权重高）
    private static final double DEFAULT_DECAY_LAMBDA = TimeDecayRrfFusion.getDecayLambdaByHalfLife(7);
    // 时间衰减权重：30%
    private static final double DEFAULT_TIME_WEIGHT = 0.3;

    @Override
    public UserMemory addMemory(Long userId, String memoryType, String memoryLayer,
                                 String content, Integer importance, String source, String tags) {
        UserMemory memory = new UserMemory();
        memory.setUserId(userId);
        memory.setMemoryType(memoryType);
        memory.setMemoryLayer(memoryLayer);
        memory.setContent(content);
        memory.setImportance(importance != null ? importance : 5);
        memory.setSource(source);
        memory.setTags(tags);
        memory.setAccessCount(0);
        memory.setLastAccessedAt(LocalDateTime.now());
        memory.setCreatedAt(LocalDateTime.now());

        // 核心画像和高重要性记忆设置较慢的衰减
        if ("PROFILE".equals(memoryLayer) || (importance != null && importance >= 7)) {
            memory.setDecayRate(0.001); // 几乎不衰减
        } else if ("SESSION".equals(memoryLayer)) {
            memory.setDecayRate(0.1); // 快速衰减，24小时左右
        } else {
            memory.setDecayRate(DEFAULT_DECAY_LAMBDA);
        }

        userMemoryMapper.insert(memory);
        log.debug("添加用户记忆 userId={} type={} layer={}", userId, memoryType, memoryLayer);
        return memory;
    }

    @Override
    public List<UserMemory> getProfileMemories(Long userId) {
        return userMemoryMapper.selectList(
                new LambdaQueryWrapper<UserMemory>()
                        .eq(UserMemory::getUserId, userId)
                        .eq(UserMemory::getMemoryLayer, "PROFILE")
                        .orderByDesc(UserMemory::getImportance)
                        .last("LIMIT 50")
        );
    }

    @Override
    public List<UserMemory> getTimelineMemoriesWithDecay(Long userId, int limit) {
        // 获取时间线记忆
        List<UserMemory> memories = userMemoryMapper.selectList(
                new LambdaQueryWrapper<UserMemory>()
                        .eq(UserMemory::getUserId, userId)
                        .eq(UserMemory::getMemoryLayer, "TIMELINE")
                        .orderByDesc(UserMemory::getCreatedAt)
                        .last("LIMIT " + Math.min(limit * 3, 200))
        );

        if (memories.isEmpty()) {
            return memories;
        }

        // 按时间衰减得分排序
        LocalDateTime now = LocalDateTime.now();
        memories.sort((m1, m2) -> {
            double score1 = calculateMemoryDecayScore(m1, now);
            double score2 = calculateMemoryDecayScore(m2, now);
            return Double.compare(score2, score1);
        });

        return memories.stream().limit(limit).collect(Collectors.toList());
    }

    @Override
    public List<Map<String, Object>> hybridSearchMemories(Long userId, String queryText,
                                                           float[] queryEmbedding, int topK) {
        // 1. 获取用户所有记忆
        List<UserMemory> allMemories = userMemoryMapper.selectList(
                new LambdaQueryWrapper<UserMemory>()
                        .eq(UserMemory::getUserId, userId)
                        .orderByDesc(UserMemory::getCreatedAt)
                        .last("LIMIT 500")
        );

        if (allMemories.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 构建时间映射
        Map<String, LocalDateTime> eventTimeMap = new HashMap<>();
        Map<String, UserMemory> memoryMap = new HashMap<>();
        for (UserMemory mem : allMemories) {
            String id = String.valueOf(mem.getId());
            memoryMap.put(id, mem);
            eventTimeMap.put(id, mem.getEventTime() != null ? mem.getEventTime() : mem.getCreatedAt());
        }

        // 3. 简化版：按创建时间排序作为一路排名（实际项目中应使用向量相似度）
        List<String> timeRanking = allMemories.stream()
                .sorted(Comparator.comparing(UserMemory::getCreatedAt).reversed())
                .map(m -> String.valueOf(m.getId()))
                .collect(Collectors.toList());

        // 4. 按重要性排序作为另一路排名
        List<String> importanceRanking = allMemories.stream()
                .sorted(Comparator.comparing(UserMemory::getImportance).reversed())
                .map(m -> String.valueOf(m.getId()))
                .collect(Collectors.toList());

        // 5. 按访问频率排序
        List<String> accessRanking = allMemories.stream()
                .sorted(Comparator.comparing(UserMemory::getAccessCount).reversed())
                .map(m -> String.valueOf(m.getId()))
                .collect(Collectors.toList());

        // 6. 多路RRF融合 + 时间衰减
        Map<String, List<String>> rankedResults = new HashMap<>();
        rankedResults.put("time", timeRanking);
        rankedResults.put("importance", importanceRanking);
        rankedResults.put("access", accessRanking);

        List<TimeDecayRrfFusion.TimeDecayFusionScore> fusedScores =
                TimeDecayRrfFusion.fuseWithTimeDecay(
                        rankedResults, eventTimeMap, 60, DEFAULT_DECAY_LAMBDA, DEFAULT_TIME_WEIGHT);

        // 7. 返回TopK结果
        return fusedScores.stream()
                .limit(topK)
                .map(score -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    UserMemory mem = memoryMap.get(score.getDocId());
                    result.put("memory", mem);
                    result.put("finalScore", score.getFinalScore());
                    result.put("rrfScore", score.getRrfScore());
                    result.put("timeDecayScore", score.getTimeDecayScore());
                    return result;
                })
                .collect(Collectors.toList());
    }

    @Override
    public String buildMemoryContext(Long userId) {
        StringBuilder context = new StringBuilder();

        // 1. 核心画像记忆
        List<UserMemory> profileMemories = getProfileMemories(userId);
        if (!profileMemories.isEmpty()) {
            context.append("【用户核心画像】\n");
            for (UserMemory mem : profileMemories) {
                context.append("- ").append(mem.getContent()).append("\n");
            }
            context.append("\n");
        }

        // 2. 近期时间线记忆（Top 10）
        List<UserMemory> timelineMemories = getTimelineMemoriesWithDecay(userId, 10);
        if (!timelineMemories.isEmpty()) {
            context.append("【近期健康记忆】\n");
            for (UserMemory mem : timelineMemories) {
                context.append("- [").append(mem.getMemoryType()).append("] ");
                context.append(mem.getContent());
                if (mem.getCreatedAt() != null) {
                    context.append(" (").append(mem.getCreatedAt().toLocalDate()).append(")");
                }
                context.append("\n");
            }
            context.append("\n");
        }

        // 3. 最近健康事件
        List<HealthEventTimeline> events = getHealthEventTimeline(userId, 30);
        if (!events.isEmpty()) {
            context.append("【健康事件时间线（近30天）】\n");
            for (HealthEventTimeline event : events) {
                context.append("- ").append(event.getEventDate()).append(" ");
                context.append("[").append(event.getEventType()).append("] ");
                context.append(event.getEventTitle()).append("\n");
            }
        }

        return context.toString();
    }

    @Override
    public void recordMemoryAccess(Long memoryId) {
        userMemoryMapper.update(null,
                new LambdaUpdateWrapper<UserMemory>()
                        .eq(UserMemory::getId, memoryId)
                        .setSql("access_count = access_count + 1")
                        .set(UserMemory::getLastAccessedAt, LocalDateTime.now())
        );
    }

    @Override
    public int cleanupSessionMemories(Long userId) {
        // 清理24小时前的瞬时记忆
        LocalDateTime expireTime = LocalDateTime.now().minusHours(24);
        return userMemoryMapper.delete(
                new LambdaQueryWrapper<UserMemory>()
                        .eq(UserMemory::getUserId, userId)
                        .eq(UserMemory::getMemoryLayer, "SESSION")
                        .lt(UserMemory::getCreatedAt, expireTime)
                        .lt(UserMemory::getImportance, 7) // 重要的不删除
        );
    }

    @Override
    public HealthEventTimeline addHealthEvent(Long userId, String eventType, String title,
                                               String description, LocalDate eventDate,
                                               String severity) {
        HealthEventTimeline event = new HealthEventTimeline();
        event.setUserId(userId);
        event.setEventType(eventType);
        event.setEventTitle(title);
        event.setEventDescription(description);
        event.setEventDate(eventDate != null ? eventDate : LocalDate.now());
        event.setSeverity(severity != null ? severity : "MODERATE");
        event.setSource("AI_EXTRACTED");
        event.setIsVerified(0);
        event.setCreatedAt(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());

        eventTimelineMapper.insert(event);
        log.info("添加健康事件 userId={} type={} title={}", userId, eventType, title);

        // 同时添加一条时间线记忆
        addMemory(userId, "HEALTH_EVENT", "TIMELINE",
                title + ": " + description, 6, "AI_EXTRACTED", eventType);

        return event;
    }

    @Override
    public List<HealthEventTimeline> getHealthEventTimeline(Long userId, int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        return eventTimelineMapper.selectByUserIdAndDateRange(userId, startDate, LocalDate.now());
    }

    @Override
    public int purgeAllMemories(Long userId) {
        int count = 0;

        // 删除用户记忆
        count += userMemoryMapper.delete(
                new LambdaQueryWrapper<UserMemory>()
                        .eq(UserMemory::getUserId, userId)
        );

        // 删除健康事件
        count += eventTimelineMapper.delete(
                new LambdaQueryWrapper<HealthEventTimeline>()
                        .eq(HealthEventTimeline::getUserId, userId)
        );

        log.info("物理删除用户记忆数据 userId={} count={}", userId, count);
        return count;
    }

    // ==================== 私有方法 ====================

    /**
     * 计算记忆的时间衰减得分
     */
    private double calculateMemoryDecayScore(UserMemory memory, LocalDateTime now) {
        LocalDateTime eventTime = memory.getEventTime() != null ? memory.getEventTime() : memory.getCreatedAt();
        if (eventTime == null) {
            return 0.5;
        }

        double decayRate = memory.getDecayRate() != null ? memory.getDecayRate() : DEFAULT_DECAY_LAMBDA;
        long days = java.time.temporal.ChronoUnit.DAYS.between(eventTime, now);

        if (days <= 0) {
            return 1.0;
        }

        // 重要性加成
        double importanceBoost = (memory.getImportance() != null ? memory.getImportance() : 5) / 10.0;

        return Math.exp(-decayRate * days) * (0.7 + 0.3 * importanceBoost);
    }
}
