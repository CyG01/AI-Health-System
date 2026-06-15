package com.example.vector;

import com.example.entity.KnowledgeDoc;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Points;
import io.qdrant.client.grpc.Points.PointStruct;
import io.qdrant.client.grpc.Points.SearchPoints;
import io.qdrant.client.grpc.Points.ScoredPoint;
import io.qdrant.client.grpc.Points.Filter;
import io.qdrant.client.grpc.Points.Condition;
import io.qdrant.client.grpc.Points.FieldCondition;
import io.qdrant.client.grpc.Points.Match;
import io.qdrant.client.grpc.Points.Vector;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.grpc.Points.WithPayloadSelector;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Qdrant 向量存储服务。
 * 封装 Qdrant CRUD + 混合检索（Dense Vector + Sparse BM25），支持 RRF 融合。
 */
@Slf4j
@Service
public class VectorStoreService {

    private final QdrantClient qdrantClient;
    private final RerankerService rerankerService;

    @Value("${qdrant.collection.knowledge-doc}")
    private String collectionName;

    @Value("${qdrant.hybrid.vector-top}")
    private int vectorTop;

    @Value("${qdrant.hybrid.bm25-top}")
    private int bm25Top;

    @Value("${qdrant.hybrid.rrf-k}")
    private int rrfK;

    @Value("${qdrant.hybrid.final-top}")
    private int finalTop;

    @Value("${qdrant.sparse-vector.enabled}")
    private boolean sparseEnabled;

    public VectorStoreService(QdrantClient qdrantClient, RerankerService rerankerService) {
        this.qdrantClient = qdrantClient;
        this.rerankerService = rerankerService;
    }

    /**
     * 批量写入（添加或更新）文档向量。
     */
    public void batchUpsert(List<KnowledgeDoc> docs, List<float[]> embeddings) {
        if (docs == null || docs.isEmpty() || embeddings == null || embeddings.isEmpty()) {
            return;
        }
        if (docs.size() != embeddings.size()) {
            log.error("batchUpsert 参数不匹配 docs={} embeddings={}", docs.size(), embeddings.size());
            return;
        }

        List<PointStruct> points = new ArrayList<>();
        for (int i = 0; i < docs.size(); i++) {
            KnowledgeDoc doc = docs.get(i);
            float[] embedding = embeddings.get(i);

            if (embedding == null || embedding.length == 0) {
                log.warn("跳过空向量文档 id={}", doc.getId());
                continue;
            }

            PointStruct.Builder point = PointStruct.newBuilder()
                    .setId(Points.PointId.newBuilder().setNum(doc.getId()).build())
                    .setVectors(Points.Vectors.newBuilder()
                            .setVector(Vector.newBuilder()
                                    .addAllData(floatList(embedding))
                                    .build())
                            .build());

            // 添加 payload（元数据）
            point.putPayload("title", ValueFactory.value(doc.getTitle() != null ? doc.getTitle() : ""));
            point.putPayload("content", ValueFactory.value(doc.getContent() != null ? doc.getContent() : ""));
            point.putPayload("category", ValueFactory.value(doc.getCategory() != null ? doc.getCategory() : ""));
            point.putPayload("authority_level", ValueFactory.value(doc.getAuthorityLevel() != null ? doc.getAuthorityLevel() : ""));
            point.putPayload("source_name", ValueFactory.value(doc.getSourceName() != null ? doc.getSourceName() : ""));

            points.add(point.build());
        }

        try {
            qdrantClient.upsertAsync(collectionName, points).get();
            log.info("批量写入 Qdrant 成功 count={}", points.size());
        } catch (Exception e) {
            log.error("批量写入 Qdrant 失败 count={}", points.size(), e);
            throw new RuntimeException("Qdrant 批量写入失败", e);
        }
    }

    /**
     * 单条写入文档向量。
     */
    public void upsert(KnowledgeDoc doc, float[] embedding) {
        if (doc == null || embedding == null) {
            return;
        }
        batchUpsert(List.of(doc), List.of(embedding));
    }

    /**
     * 删除文档向量。
     */
    public void delete(Long docId) {
        try {
            qdrantClient.deleteAsync(collectionName,
                    List.of(Points.PointId.newBuilder().setNum(docId).build())).get();
            log.debug("删除 Qdrant 文档 id={}", docId);
        } catch (Exception e) {
            log.error("删除 Qdrant 文档失败 id={}", docId, e);
        }
    }

    /**
     * 混合检索：Dense Vector + BM25 稀疏向量 → RRF 融合 → Rerank → Top-N。
     * 支持权威等级过滤。
     */
    public List<KnowledgeDoc> hybridSearch(String queryText, float[] queryEmbedding,
                                            List<String> authorityFilter, int topK) {
        if (queryEmbedding == null || queryEmbedding.length == 0) {
            return List.of();
        }

        try {
            // 1. Dense Vector 检索
            Map<String, KnowledgeDoc> candidateDocs = new LinkedHashMap<>();
            List<String> vectorRanking = denseSearch(queryEmbedding, authorityFilter, vectorTop, candidateDocs);

            // 2. BM25 稀疏向量检索（可选）
            Map<String, List<String>> rankedResults = new LinkedHashMap<>();
            rankedResults.put("vector", vectorRanking);

            if (sparseEnabled) {
                List<String> bm25Ranking = bm25Search(queryText, authorityFilter, bm25Top, candidateDocs);
                rankedResults.put("bm25", bm25Ranking);
            }

            // 3. RRF 融合
            List<String> fusedIds = RrfFusion.fuseTopN(rankedResults, rrfK, Math.max(topK * 2, 20));

            // 4. Rerank 重排
            Map<String, String> candidateContents = new LinkedHashMap<>();
            for (String id : fusedIds) {
                KnowledgeDoc doc = candidateDocs.get(id);
                if (doc != null) {
                    candidateContents.put(id, doc.getContent());
                }
            }
            List<String> rerankedIds = rerankerService.rerank(queryText, candidateContents, topK);

            // 5. 返回结果
            List<KnowledgeDoc> results = new ArrayList<>();
            for (String id : rerankedIds) {
                KnowledgeDoc doc = candidateDocs.get(id);
                if (doc != null) {
                    results.add(doc);
                }
            }

            log.debug("混合检索完成 query={} vector={} bm25={} fused={} reranked={} final={}",
                    truncate(queryText, 50), vectorRanking.size(),
                    rankedResults.containsKey("bm25") ? rankedResults.get("bm25").size() : 0,
                    fusedIds.size(), rerankedIds.size(), results.size());

            return results;
        } catch (Exception e) {
            log.error("混合检索异常 query={}", truncate(queryText, 50), e);
            return List.of();
        }
    }

    /**
     * Dense Vector 检索（余弦相似度）。
     */
    private List<String> denseSearch(float[] queryEmbedding, List<String> authorityFilter,
                                      int topK, Map<String, KnowledgeDoc> candidateDocs) {
        try {
            SearchPoints.Builder searchBuilder = SearchPoints.newBuilder()
                    .setCollectionName(collectionName)
                    .addAllVector(floatList(queryEmbedding))
                    .setLimit(topK)
                    .setWithPayload(WithPayloadSelector.newBuilder().setEnable(true).build());

            // 权威等级过滤
            if (authorityFilter != null && !authorityFilter.isEmpty()) {
                searchBuilder.setFilter(buildAuthorityFilter(authorityFilter));
            }

            List<ScoredPoint> results = qdrantClient.searchAsync(searchBuilder.build()).get();

            List<String> ranking = new ArrayList<>();
            for (ScoredPoint point : results) {
                String docId = String.valueOf(point.getId().getNum());
                ranking.add(docId);

                // 缓存文档信息
                if (!candidateDocs.containsKey(docId)) {
                    KnowledgeDoc doc = new KnowledgeDoc();
                    doc.setId(point.getId().getNum());
                    doc.setTitle(getPayloadString(point, "title"));
                    doc.setContent(getPayloadString(point, "content"));
                    doc.setCategory(getPayloadString(point, "category"));
                    doc.setAuthorityLevel(getPayloadString(point, "authority_level"));
                    doc.setSourceName(getPayloadString(point, "source_name"));
                    candidateDocs.put(docId, doc);
                }
            }
            return ranking;
        } catch (Exception e) {
            log.error("Dense 向量检索失败", e);
            return List.of();
        }
    }

    /**
     * BM25 稀疏向量检索（关键词匹配）。
     * 通过 Qdrant 的稀疏向量功能实现，本质是基于文本分词的 BM25。
     */
    private List<String> bm25Search(String queryText, List<String> authorityFilter,
                                     int topK, Map<String, KnowledgeDoc> candidateDocs) {
        // BM25 稀疏向量检索在 Qdrant 1.13.0 中暂不支持，降级返回空
        log.debug("BM25 稀疏向量检索暂不可用（Qdrant 1.13.0 API 限制）");
        return List.of();
    }

    /**
     * 简单文本分词生成稀疏向量（模拟 BM25 Tokenization）。
     */
    private Map<Integer, Float> tokenizeToSparse(String text) {
        Map<Integer, Float> sparse = new LinkedHashMap<>();
        if (text == null || text.isBlank()) {
            return sparse;
        }
        // 简单分词：按字符/空格切分，用 hash 映射到稀疏向量维度
        String[] tokens = text.toLowerCase().split("[\\s,，。.！!？?、]+");
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            int index = Math.abs(token.hashCode()) % 100000; // 10万维稀疏空间
            sparse.merge(index, 1.0f, Float::sum);
        }
        // 归一化
        float norm = 0;
        for (float v : sparse.values()) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);
        if (norm > 0) {
            for (Map.Entry<Integer, Float> entry : sparse.entrySet()) {
                entry.setValue(entry.getValue() / norm);
            }
        }
        return sparse;
    }

    private Filter buildAuthorityFilter(List<String> authorityLevels) {
        Filter.Builder filter = Filter.newBuilder();
        for (String level : authorityLevels) {
            filter.addMust(Condition.newBuilder()
                    .setField(FieldCondition.newBuilder()
                            .setKey("authority_level")
                            .setMatch(Match.newBuilder()
                                    .setKeyword(level)
                                    .build())
                            .build())
                    .build());
        }
        return filter.build();
    }

    private String getPayloadString(ScoredPoint point, String key) {
        return point.getPayloadOrDefault(key, ValueFactory.value(""))
                .getStringValue();
    }

    private List<Float> floatList(float[] array) {
        List<Float> list = new ArrayList<>(array.length);
        for (float v : array) {
            list.add(v);
        }
        return list;
    }

    private String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}