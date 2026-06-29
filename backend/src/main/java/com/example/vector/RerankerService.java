package com.example.vector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 重排序服务。
 * 优先使用 Cohere Rerank API 快速上线，后续可切换本地 BGE-Reranker。
 */
@Slf4j
@Service
public class RerankerService {

    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${cohere.api-key}")
    private String cohereApiKey;

    @Value("${cohere.rerank-model:rerank-v3.5}")
    private String rerankModel;

    @Value("${cohere.local-rerank.enabled:false}")
    private boolean localRerankEnabled;

    public RerankerService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl("https://api.cohere.ai/v1")
                .build();
    }

    /**
     * 对 RRF 融合后的候选文档进行重排序。
     *
     * @param query        原始查询文本
     * @param documents    候选文档（id -> content）
     * @param topN         返回 Top-N 结果
     * @return 重排序后的文档 ID 列表
     */
    public List<String> rerank(String query, Map<String, String> documents, int topN) {
        if (documents == null || documents.isEmpty()) {
            return List.of();
        }
        if (cohereApiKey == null || cohereApiKey.isBlank()) {
            log.warn("Cohere API Key 未配置，跳过重排序，返回原始顺序");
            return new ArrayList<>(documents.keySet()).subList(0, Math.min(topN, documents.size()));
        }

        try {
            // 构建 Cohere Rerank 请求
            List<String> docList = new ArrayList<>(documents.values());
            List<String> docIds = new ArrayList<>(documents.keySet());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", rerankModel);
            body.put("query", query);
            body.put("documents", docList);
            body.put("top_n", Math.min(topN, docList.size()));
            body.put("return_documents", false);

            String response = webClient.post()
                    .uri("/rerank")
                    .header("Authorization", "Bearer " + cohereApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode node = objectMapper.readTree(response);
            JsonNode results = node.get("results");

            List<String> rerankedIds = new ArrayList<>();
            for (JsonNode result : results) {
                int index = result.get("index").asInt();
                if (index < docIds.size()) {
                    rerankedIds.add(docIds.get(index));
                }
            }

            log.debug("重排序完成 query={} candidates={} reranked={}",
                    truncate(query, 50), documents.size(), rerankedIds.size());
            return rerankedIds;
        } catch (Exception e) {
            log.error("重排序失败，返回原始顺序 query={}", truncate(query, 50), e);
            // 降级：返回原始顺序
            return new ArrayList<>(documents.keySet()).subList(0, Math.min(topN, documents.size()));
        }
    }

    private String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}