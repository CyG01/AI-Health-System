package com.example.service.impl;

import com.example.entity.UserMemory;
import com.example.entity.UserProfile;
import com.example.mapper.UserMemoryMapper;
import com.example.mapper.UserProfileMapper;
import com.example.service.MemoryService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MemoryServiceImpl implements MemoryService {

    private static final int EMBEDDING_DIM = 1536;
    private static final double DUPLICATE_THRESHOLD = 0.02;

    private final UserMemoryMapper memoryMapper;
    private final UserProfileMapper userProfileMapper;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    public MemoryServiceImpl(UserMemoryMapper memoryMapper, UserProfileMapper userProfileMapper,
                                ObjectMapper objectMapper) {
        this.memoryMapper = memoryMapper;
        this.userProfileMapper = userProfileMapper;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    @Transactional
    public UserMemory store(Long userId, String content, String memoryType, int importance, String source) {
        float[] embedding = generateEmbedding(content);
        if (embedding == null) {
            log.warn("向量生成失败，跳过记忆存储 userId={}", userId);
            return null;
        }

        // 去重：检查是否存在语义相近的记忆（余弦距离 < 阈值）
        String embeddingStr = vectorToString(embedding);
        List<UserMemory> duplicates = memoryMapper.findSimilar(userId, embeddingStr, 1);
        if (!duplicates.isEmpty()) {
            UserMemory existing = duplicates.get(0);
            // 更新访问统计，不重复插入
            memoryMapper.incrementAccessCount(existing.getId());
            log.info("记忆去重跳过 userId={} type={} content={}", userId, memoryType, truncate(content, 80));
            return existing;
        }

        UserMemory memory = new UserMemory();
        memory.setUserId(userId);
        memory.setMemoryType(memoryType);
        memory.setContent(content);
        memory.setEmbedding(embeddingStr);
        memory.setImportance(importance);
        memory.setSource(source);
        memory.setAccessCount(1);
        memory.setLastAccessedAt(LocalDateTime.now());
        memory.setCreatedAt(LocalDateTime.now());

        memoryMapper.insert(memory);
        log.info("记忆存储成功 userId={} type={} importance={} content={}",
                userId, memoryType, importance, truncate(content, 80));
        return memory;
    }

    @Override
    public List<UserMemory> retrieveRelevant(Long userId, String queryText, int topK) {
        float[] queryEmbedding = generateEmbedding(queryText);
        if (queryEmbedding == null) {
            log.warn("向量生成失败，回退到空结果 userId={}", userId);
            return Collections.emptyList();
        }
        String embeddingStr = vectorToString(queryEmbedding);

        // 尝试使用 MySQL 原生 VEC_COSINE_DISTANCE 检索
        try {
            List<UserMemory> results = memoryMapper.findSimilar(userId, embeddingStr, topK);
            // 更新访问统计
            for (UserMemory mem : results) {
                memoryMapper.incrementAccessCount(mem.getId());
            }
            return results;
        } catch (Exception e) {
            // 降级：MySQL 不支持原生 VECTOR，使用应用层余弦相似度计算
            log.debug("VEC_COSINE_DISTANCE 不可用，使用应用层相似度计算 userId={}", userId);
            return retrieveRelevantInApp(userId, queryEmbedding, topK);
        }
    }

    /**
     * 应用层余弦相似度计算（TEXT/JSON 列存储向量的降级方案）
     */
    private List<UserMemory> retrieveRelevantInApp(Long userId, float[] queryEmbedding, int topK) {
        List<UserMemory> all = memoryMapper.findAllWithEmbedding(userId);
        if (all.isEmpty()) {
            return Collections.emptyList();
        }

        // 计算每条记忆的余弦相似度
        List<Map.Entry<UserMemory, Double>> scored = new ArrayList<>();
        for (UserMemory mem : all) {
            float[] memEmbedding = stringToVector(mem.getEmbedding());
            if (memEmbedding == null || memEmbedding.length != queryEmbedding.length) continue;
            double similarity = cosineSimilarity(queryEmbedding, memEmbedding);
            scored.add(new AbstractMap.SimpleEntry<>(mem, similarity));
        }

        scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        List<UserMemory> results = new ArrayList<>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            UserMemory mem = scored.get(i).getKey();
            memoryMapper.incrementAccessCount(mem.getId());
            results.add(mem);
        }
        return results;
    }

    /**
     * 计算两个向量的余弦相似度
     */
    private double cosineSimilarity(float[] a, float[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * 将 JSON 格式的向量字符串解析为 float[]
     */
    private float[] stringToVector(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            s = s.replace("[", "").replace("]", "").trim();
            String[] parts = s.split(",");
            float[] result = new float[parts.length];
            for (int i = 0; i < parts.length; i++) {
                result[i] = Float.parseFloat(parts[i].trim());
            }
            return result;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public List<UserMemory> getHighImportance(Long userId) {
        return memoryMapper.findHighImportance(userId);
    }

    @Override
    public String buildMemoryContext(Long userId, String queryText, int topK) {
        List<UserMemory> memories = retrieveRelevant(userId, queryText, topK);
        if (memories.isEmpty()) {
            // 冷启动降级：使用用户画像作为初始上下文
            String profileContext = buildProfileFallback(userId);
            return profileContext;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("【用户历史记忆】\n");
        for (int i = 0; i < memories.size(); i++) {
            UserMemory mem = memories.get(i);
            sb.append(String.format("%d. [%s] %s\n",
                    i + 1, translateMemoryType(mem.getMemoryType()), mem.getContent()));
        }
        return sb.toString();
    }

    /**
     * 冷启动降级：从 user_profile 构建上下文
     */
    private String buildProfileFallback(Long userId) {
        try {
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserProfile> wrapper =
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
            wrapper.eq(UserProfile::getUserId, userId);
            UserProfile profile = userProfileMapper.selectOne(wrapper);
            if (profile == null) {
                return "";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("【用户基本信息】\n");
            sb.append("1. 健康目标:").append(translateGoal(profile.getHealthGoal())).append("\n");
            sb.append("2. 运动基础:").append(profile.getFitnessLevel() != null ? profile.getFitnessLevel() : "未知").append("\n");
            if (profile.getChronicDiseases() != null && !profile.getChronicDiseases().equals("无")) {
                sb.append("3. 慢性疾病:").append(profile.getChronicDiseases()).append("\n");
            }
            if (profile.getInjuries() != null && !profile.getInjuries().equals("无")) {
                sb.append("4. 运动损伤:").append(profile.getInjuries()).append("\n");
            }
            if (profile.getDietPreferences() != null && !profile.getDietPreferences().equals("无")) {
                sb.append("5. 饮食偏好:").append(profile.getDietPreferences()).append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("冷启动画像降级失败 userId={}", userId, e);
            return "";
        }
    }

    private String translateGoal(String goal) {
        if (goal == null) return "未设置";
        return switch (goal) {
            case "LOSE_WEIGHT" -> "减重";
            case "GAIN_MUSCLE" -> "增肌";
            case "STAY_HEALTHY" -> "保持健康";
            case "REHABILITATION" -> "康复";
            case "STRESS_RELIEF" -> "减压";
            default -> goal;
        };
    }

    @Override
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public int cleanupStaleMemories() {
        String threshold = LocalDateTime.now().minusDays(90).toString();
        int deleted = memoryMapper.deleteLowImportanceStale(threshold);
        if (deleted > 0) {
            log.info("清理低重要性旧记忆 {} 条 (90天未访问 & importance<3)", deleted);
        }
        return deleted;
    }

    @Override
    public void autoCollect(Long userId, String content, String source) {
        if (content == null || content.isBlank() || content.length() < 10) {
            return;
        }
        // 分析内容，判断记忆类型和重要性
        String type = classifyMemoryType(content);
        int importance = calculateImportance(content, source);
        store(userId, content, type, importance, source);
    }

    /**
     * 调用 DeepSeek Embedding API 生成向量
     */
    private float[] generateEmbedding(String text) {
        try {
            Map<String, Object> body = Map.of(
                    "model", "deepseek-embedding",
                    "input", text
            );
            String response = webClient.post()
                    .uri("/embeddings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode node = objectMapper.readTree(response);
            JsonNode embeddingNode = node.get("data").get(0).get("embedding");
            float[] result = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                result[i] = (float) embeddingNode.get(i).asDouble();
            }
            return result;
        } catch (Exception e) {
            log.error("生成向量失败: {}", e.getMessage());
            return null;
        }
    }

    private String vectorToString(float[] vector) {
        if (vector == null) return null;
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 基于关键词自动分类记忆类型
     */
    private String classifyMemoryType(String content) {
        if (containsAny(content, "喜欢", "偏好", "爱吃", "不喜欢", "讨厌", "习惯", "常用")) {
            return "PREFERENCE";
        }
        if (containsAny(content, "疼", "痛", "伤", "膝盖", "腰", "血压", "血糖", "过敏", "病史", "病历")) {
            return "INJURY";
        }
        if (containsAny(content, "反馈", "太累", "太难", "完成", "做不到", "很好", "不行")) {
            return "FEEDBACK";
        }
        if (containsAny(content, "每天", "经常", "一直", "从不", "总是", "每周", "坚持")) {
            return "HABIT";
        }
        return "HEALTH";
    }

    /**
     * 基于来源和内容关键词计算重要性
     */
    private int calculateImportance(String content, String source) {
        int base = 5;
        if ("ONBOARDING".equals(source)) return 8;
        if (containsAny(content, "过敏", "病史", "高血压", "糖尿病", "心脏病", "手术", "怀孕")) return 9;
        if (containsAny(content, "膝盖", "腰", "伤", "疼", "痛")) return 7;
        if (containsAny(content, "喜欢", "偏好", "习惯")) return 4;
        return base;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private String translateMemoryType(String type) {
        return switch (type) {
            case "PREFERENCE" -> "偏好";
            case "INJURY" -> "伤病";
            case "FEEDBACK" -> "反馈";
            case "HABIT" -> "习惯";
            case "ONBOARDING" -> "基本信息";
            case "HEALTH" -> "健康";
            default -> type;
        };
    }

    private String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}