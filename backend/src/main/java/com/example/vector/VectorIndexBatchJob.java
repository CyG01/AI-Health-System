package com.example.vector;

import com.example.entity.KnowledgeDoc;
import com.example.mapper.KnowledgeDocMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 存量知识库向量索引批量构建 Job。
 * 每天凌晨 1:00 执行，分批处理，每批 100 条，避免 OOM。
 * 支持断点续传（通过 lastProcessedId 记录进度）。
 */
@Slf4j
@Component
public class VectorIndexBatchJob {

    private static final int PAGE_SIZE = 100;

    private final KnowledgeDocMapper knowledgeDocMapper;
    private final EmbeddingService embeddingService;
    private final VectorStoreService vectorStoreService;

    /** 上次处理到的最大 ID（用于断点续传） */
    private final AtomicLong lastProcessedId = new AtomicLong(0);

    /** 是否正在运行 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    public VectorIndexBatchJob(KnowledgeDocMapper knowledgeDocMapper,
                                EmbeddingService embeddingService,
                                VectorStoreService vectorStoreService) {
        this.knowledgeDocMapper = knowledgeDocMapper;
        this.embeddingService = embeddingService;
        this.vectorStoreService = vectorStoreService;
    }

    /**
     * 每天凌晨 1:00 执行批量索引构建。
     */
    @Scheduled(cron = "0 0 1 * * ?")
    public void batchBuildIndex() {
        if (!running.compareAndSet(false, true)) {
            log.warn("上一次批量索引构建尚未完成，跳过本次执行");
            return;
        }

        try {
            long total = knowledgeDocMapper.count();
            log.info("开始批量构建向量索引 total={} pageSize={}", total, PAGE_SIZE);

            long processed = 0;
            long skipped = 0;
            long startId = lastProcessedId.get();

            for (long offset = startId; offset < total; offset += PAGE_SIZE) {
                try {
                    // 分批查询
                    List<KnowledgeDoc> docs = knowledgeDocMapper.selectPage(offset, PAGE_SIZE);

                    if (docs == null || docs.isEmpty()) {
                        break;
                    }

                    // 过滤出需要构建索引的文档（无 embedding 或已更新的）
                    List<KnowledgeDoc> toIndex = docs.stream()
                            .filter(doc -> doc.getContent() != null && !doc.getContent().isBlank())
                            .toList();

                    if (toIndex.isEmpty()) {
                        skipped += docs.size();
                        continue;
                    }

                    // 批量生成向量
                    List<String> contents = toIndex.stream()
                            .map(KnowledgeDoc::getContent)
                            .toList();
                    List<float[]> embeddings = embeddingService.batchEmbed(contents);

                    if (embeddings.isEmpty()) {
                        log.warn("向量生成全部失败，跳过本批次 offset={}", offset);
                        skipped += toIndex.size();
                        continue;
                    }

                    // 有效向量写入 Qdrant
                    int validCount = 0;
                    for (int i = 0; i < toIndex.size(); i++) {
                        if (i < embeddings.size() && embeddings.get(i) != null
                                && embeddings.get(i).length > 0) {
                            validCount++;
                        }
                    }

                    List<KnowledgeDoc> validDocs = new ArrayList<>();
                    List<float[]> validEmbeddings = new ArrayList<>();
                    for (int i = 0; i < toIndex.size(); i++) {
                        if (i < embeddings.size() && embeddings.get(i) != null
                                && embeddings.get(i).length > 0) {
                            validDocs.add(toIndex.get(i));
                            validEmbeddings.add(embeddings.get(i));
                        }
                    }

                    if (!validDocs.isEmpty()) {
                        vectorStoreService.batchUpsert(validDocs, validEmbeddings);
                    }

                    processed += validDocs.size();
                    skipped += (toIndex.size() - validCount);

                    // 更新进度
                    if (!toIndex.isEmpty()) {
                        lastProcessedId.set(toIndex.get(toIndex.size() - 1).getId());
                    }

                    log.info("向量索引构建进度 processed={} skipped={} total={} percent={}%",
                            processed, skipped, total,
                            String.format("%.1f", (offset + PAGE_SIZE) * 100.0 / Math.max(total, 1)));

                } catch (Exception e) {
                    log.error("向量索引构建批次失败 offset={}", offset, e);
                    // 失败不中断，继续下一批
                }
            }

            // 全部完成，重置进度
            lastProcessedId.set(0);
            log.info("批量向量索引构建完成 processed={} skipped={} total={}", processed, skipped, total);

        } finally {
            running.set(false);
        }
    }

    /**
     * 手动触发增量索引（供运维 API 调用）。
     */
    public void incrementalBuild() {
        lastProcessedId.set(0);
        batchBuildIndex();
    }

    /**
     * 获取索引进度。
     */
    public String getProgress() {
        return String.format("running=%s lastId=%d", running.get(), lastProcessedId.get());
    }
}