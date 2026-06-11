package com.example.service.impl;

import com.example.entity.KnowledgeDoc;
import com.example.mapper.KnowledgeDocMapper;
import com.example.service.KnowledgeService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
public class KnowledgeServiceImpl implements KnowledgeService {

    /** 医疗核心问题关键词：涉及疾病诊断、药物、治疗、康复等 */
    private static final Pattern MEDICAL_CORE_PATTERN = Pattern.compile(
            "(疾病|癌症|糖尿病|高血压|心脏病|中风|肝炎|肾病|肺病|甲状腺|痛风|关节炎|骨质|抑郁|焦虑|精神分裂" +
                    "|药物|服药|吃药|处方|诊断|确诊|治疗|治愈|根治|复发|症状|并发症|康复|术后|手术" +
                    "|血压|血糖|血脂|肝功|肾功|尿酸|胆固醇|体检|检查|化验|B超|CT|MRI|X光)");

    /** 非医疗问题分类 */
    private static final List<String> ALL_LEVELS = Arrays.asList("A", "B", "C", "D");
    private static final List<String> HIGH_LEVELS = Arrays.asList("A", "B");

    private final KnowledgeDocMapper mapper;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${deepseek.api-key}")
    private String apiKey;

    @Value("${deepseek.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    public KnowledgeServiceImpl(KnowledgeDocMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    public List<KnowledgeDoc> searchRelevant(String queryText, boolean isMedicalCore, int topK) {
        float[] embedding = generateEmbedding(queryText);
        if (embedding == null) {
            return Collections.emptyList();
        }

        List<String> authorityFilter = isMedicalCore ? HIGH_LEVELS : ALL_LEVELS;
        return mapper.findSimilar(vectorToString(embedding), authorityFilter, topK);
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
        float[] embedding = generateEmbedding(content);
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
        log.info("知识文档导入成功 title={} category={} level={}", title, category, authorityLevel);
        return doc;
    }

    @Override
    public float[] generateEmbedding(String text) {
        try {
            Map<String, Object> body = Map.of(
                    "model", "deepseek-chat",
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
}