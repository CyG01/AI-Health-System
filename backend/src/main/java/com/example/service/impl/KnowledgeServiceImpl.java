package com.example.service.impl;

import com.example.annotation.Trace;
import com.example.entity.KnowledgeDoc;
import com.example.mapper.KnowledgeDocMapper;
import com.example.service.KnowledgeService;
import com.example.vector.EmbeddingService;
import com.example.vector.VectorStoreService;
import com.example.dto.AiTaskMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 知识库服务实现（Phase 2b 升级版）。
 *
 * 检索策略：
 * 1. 优先 Qdrant 混合检索（Dense Vector + BM25 → RRF → Rerank）
 * 2. Qdrant 不可用 → MySQL LIKE + Redis 热词缓存降级
 * 3. 灰度切换按 traffic.ratio 控制流量分配
 */
@Slf4j
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    /** 医疗核心问题关键词 */
    private static final Pattern MEDICAL_CORE_PATTERN = Pattern.compile(
            "(疾病|癌症|糖尿病|高血压|心脏病|中风|肝炎|肾病|肺病|甲状腺|痛风|关节炎|骨质|抑郁|焦虑|精神分裂"
                    + "|药物|服药|吃药|处方|诊断|确诊|治疗|治愈|根治|复发|症状|并发症|康复|术后|手术"
                    + "|血压|血糖|血脂|肝功|肾功|尿酸|胆固醇|体检|检查|化验|B超|CT|MRI|X光)");

    private static final List<String> ALL_LEVELS = Arrays.asList("A", "B", "C", "D");
    private static final List<String> HIGH_LEVELS = Arrays.asList("A", "B");

    /** Redis 热词缓存 key 前缀 */
    private static final String HOTWORD_CACHE_PREFIX = "kb:hotword:";

    /** 热词缓存 TTL（30 分钟） */
    private static final long HOTWORD_CACHE_TTL_MINUTES = 30;

    private final KnowledgeDocMapper mapper;
    private final ObjectMapper objectMapper;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;
    private final StringRedisTemplate redisTemplate;

    @Value("${qdrant.traffic.ratio:0.0}")
    private double trafficRatio;

    @Value("${qdrant.traffic.enabled:false}")
    private boolean trafficEnabled;

    public KnowledgeServiceImpl(KnowledgeDocMapper mapper,
                                 ObjectMapper objectMapper,
                                 EmbeddingService embeddingService,
                                 VectorStoreService vectorStoreService,
                                 StringRedisTemplate redisTemplate) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
        this.redisTemplate = redisTemplate;
    }

    @Override
    @Trace(spanName = "knowledge-service-search", recordArgs = false, recordResult = false, slowThresholdMs = 3000)
    public List<KnowledgeDoc> searchRelevant(String queryText, boolean isMedicalCore, int topK) {
        List<String> authorityFilter = isMedicalCore ? HIGH_LEVELS : ALL_LEVELS;

        // 灰度判断：是否走 Qdrant 混合检索
        if (shouldUseQdrant()) {
            try {
                List<KnowledgeDoc> results = hybridSearch(queryText, authorityFilter, topK);
                if (results != null && !results.isEmpty()) {
                    // 缓存热词结果
                    cacheHotwordResult(queryText, results);
                    return results;
                }
                log.debug("Qdrant 混合检索无结果，降级到 MySQL LIKE query={}", truncate(queryText, 50));
            } catch (Exception e) {
                log.error("Qdrant 混合检索异常，降级到 MySQL LIKE query={}", truncate(queryText, 50), e);
            }
        }

        // 降级：Qdrant 不可用或灰度未命中，使用 MySQL LIKE + Redis 热词缓存
        return fallbackSearch(queryText, authorityFilter, topK);
    }

    /**
     * Qdrant 混合检索（Dense Vector + BM25 → RRF → Rerank）。
     */
    private List<KnowledgeDoc> hybridSearch(String queryText, List<String> authorityFilter, int topK) {
        float[] queryEmbedding = embeddingService.embed(queryText);
        if (queryEmbedding == null) {
            log.warn("向量生成失败，无法使用 Qdrant 混合检索 query={}", truncate(queryText, 50));
            return List.of();
        }
        return vectorStoreService.hybridSearch(queryText, queryEmbedding, authorityFilter, topK);
    }

    /**
     * 降级检索：Redis 热词缓存 + MySQL LIKE。
     */
    private List<KnowledgeDoc> fallbackSearch(String queryText, List<String> authorityFilter, int topK) {
        // 1. 先查 Redis 热词缓存
        String cacheKey = HOTWORD_CACHE_PREFIX + queryText.hashCode();
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("命中热词缓存 query={}", truncate(queryText, 50));
                // 缓存的是文档 ID 列表（逗号分隔），需从 DB 查询
                String[] ids = cached.split(",");
                List<KnowledgeDoc> docs = new java.util.ArrayList<>();
                for (String id : ids) {
                    try {
                        KnowledgeDoc doc = mapper.selectById(Long.parseLong(id));
                        if (doc != null) {
                            docs.add(doc);
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
                if (!docs.isEmpty()) {
                    return docs.subList(0, Math.min(topK, docs.size()));
                }
            }
        } catch (Exception e) {
            log.debug("Redis 缓存查询异常，跳过缓存 query={}", truncate(queryText, 50));
        }

        // 2. 降级到 MySQL LIKE 模糊搜索
        try {
            String keyword = extractKeywords(queryText);
            List<KnowledgeDoc> docs = mapper.searchByKeyword(keyword, authorityFilter, topK);

            // 3. 缓存到 Redis
            if (!docs.isEmpty()) {
                String cacheValue = docs.stream()
                        .map(d -> String.valueOf(d.getId()))
                        .reduce((a, b) -> a + "," + b)
                        .orElse("");
                try {
                    redisTemplate.opsForValue().set(cacheKey, cacheValue,
                            HOTWORD_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
                } catch (Exception e) {
                    log.debug("Redis 缓存写入失败", e);
                }
            }
            return docs;
        } catch (Exception e) {
            log.error("MySQL LIKE 搜索失败 query={}", truncate(queryText, 50), e);
            return List.of();
        }
    }

    /**
     * 从查询文本提取关键词（用于 MySQL LIKE 搜索）。
     */
    private String extractKeywords(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        // 去掉常见停用词，提取核心关键词
        return text.replaceAll("[？?，,。.！!、\\s]+", " ")
                .replaceAll("什么是|怎么|如何|为什么|是什么|怎么办|告诉我|请|一下|帮我|可以", "")
                .trim();
    }

    /**
     * 缓存高频问题的检索结果。
     */
    private void cacheHotwordResult(String queryText, List<KnowledgeDoc> docs) {
        if (docs.isEmpty()) return;
        try {
            String cacheKey = HOTWORD_CACHE_PREFIX + queryText.hashCode();
            String cacheValue = docs.stream()
                    .map(d -> String.valueOf(d.getId()))
                    .reduce((a, b) -> a + "," + b)
                    .orElse("");
            redisTemplate.opsForValue().set(cacheKey, cacheValue,
                    HOTWORD_CACHE_TTL_MINUTES, TimeUnit.MINUTES);
        } catch (Exception e) {
            log.debug("热词缓存写入失败", e);
        }
    }

    /**
     * 判断当前请求是否应走 Qdrant 混合检索。
     */
    private boolean shouldUseQdrant() {
        if (!trafficEnabled) {
            // 灰度未启用，直接全量（ratio > 0.5 时走 Qdrant）
            return trafficRatio >= 0.5;
        }
        // 灰度启用：按比例分流
        return Math.random() < trafficRatio;
    }

    @Override
    public boolean isMedicalCoreQuestion(String queryText) {
        return MEDICAL_CORE_PATTERN.matcher(queryText).find();
    }

    @Override
    public String buildKnowledgeContext(List<KnowledgeDoc> docs) {
        if (docs == null || docs.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【权威知识参考】\n");
        for (int i = 0; i < docs.size(); i++) {
            KnowledgeDoc doc = docs.get(i);
            sb.append(String.format("%d. [%s] 《%s》%s: %s\n",
                    i + 1, doc.getAuthorityLevel(), doc.getSourceName(),
                    doc.getTitle(), doc.getContent()));
        }
        return sb.toString();
    }

    @Override
    @Transactional
    public KnowledgeDoc importDocument(String title, String content, String category,
                                       String sourceName, String authorityLevel, String version) {
        float[] embedding = embeddingService.embed(content);
        KnowledgeDoc doc = new KnowledgeDoc();
        doc.setTitle(title);
        doc.setContent(content);
        doc.setCategory(category);
        doc.setSourceName(sourceName);
        doc.setAuthorityLevel(authorityLevel);
        doc.setEmbedding(vectorToString(embedding));
        doc.setVersion(version);
        doc.setIsActive(1);
        mapper.insert(doc);

        // 同步写入 Qdrant
        if (embedding != null) {
            vectorStoreService.upsert(doc, embedding);
        }

        log.info("知识文档导入成功 title={} category={} level={} qdrant={}",
                title, category, authorityLevel, embedding != null);
        return doc;
    }

    @Override
    public float[] generateEmbedding(String text) {
        return embeddingService.embed(text);
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
     * 同步导入知识文档（MQ Consumer 调用）。
     */
    public KnowledgeDoc importDocumentSync(AiTaskMessage message) {
        String title = message.getParams() != null
                ? message.getParams().getOrDefault("title", "未命名文档")
                : "未命名文档";
        String category = message.getParams() != null
                ? message.getParams().getOrDefault("category", "general")
                : "general";
        String sourceName = message.getParams() != null
                ? message.getParams().getOrDefault("sourceName", "unknown")
                : "unknown";
        String authorityLevel = message.getParams() != null
                ? message.getParams().getOrDefault("authorityLevel", "C")
                : "C";
        String version = message.getParams() != null
                ? message.getParams().getOrDefault("version", "1.0")
                : "1.0";
        return importDocument(title, message.getPayload(), category, sourceName, authorityLevel, version);
    }

    private String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}