package com.example.vector;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections.Distance;
import io.qdrant.client.grpc.Collections.VectorParams;
import io.qdrant.client.grpc.Collections.VectorsConfig;
import io.qdrant.client.grpc.Collections.SparseVectorConfig;
import io.qdrant.client.grpc.Collections.SparseIndexConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutionException;

/**
 * Qdrant 向量数据库客户端配置。
 * 支持 Qdrant Cloud 托管版和自建节点，自动创建 knowledge_doc 集合。
 */
@Slf4j
@Configuration
public class QdrantConfig {

    @Value("${qdrant.host}")
    private String host;

    @Value("${qdrant.port}")
    private int port;

    @Value("${qdrant.api-key}")
    private String apiKey;

    @Value("${qdrant.use-tls}")
    private boolean useTls;

    @Value("${qdrant.collection.knowledge-doc}")
    private String knowledgeDocCollection;

    @Value("${qdrant.vector-dim}")
    private int vectorDim;

    @Value("${qdrant.sparse-vector.enabled}")
    private boolean sparseVectorEnabled;

    @Bean
    public QdrantClient qdrantClient() {
        QdrantGrpcClient.Builder grpcBuilder = QdrantGrpcClient.newBuilder()
                .host(host)
                .port(port)
                .useTls(useTls);

        if (apiKey != null && !apiKey.isBlank()) {
            grpcBuilder.withApiKey(apiKey);
        }

        QdrantClient client = new QdrantClient(grpcBuilder.build());
        log.info("Qdrant 客户端初始化完成 host={} port={} tls={} apikey={}",
                host, port, useTls, apiKey != null && !apiKey.isBlank());

        // 自动创建集合
        try {
            initCollection(client);
        } catch (Exception e) {
            log.error("Qdrant 集合初始化失败，将在首次调用时重试", e);
        }

        return client;
    }

    private void initCollection(QdrantClient client) throws ExecutionException, InterruptedException {
        boolean exists = client.collectionExistsAsync(knowledgeDocCollection).get();
        if (exists) {
            log.info("集合 {} 已存在，跳过创建", knowledgeDocCollection);
            return;
        }

        // 构建向量配置（dense vector 1536 维）
        VectorParams vectorParams = VectorParams.newBuilder()
                .setSize(vectorDim)
                .setDistance(Distance.Cosine)
                .build();

        VectorsConfig.Builder vectorsBuilder = VectorsConfig.newBuilder()
                .setParams(vectorParams);

        // 可选：稀疏向量（BM25）
        if (sparseVectorEnabled) {
            SparseVectorConfig sparseConfig = SparseVectorConfig.newBuilder()
                    .setIndex(SparseIndexConfig.newBuilder().build())
                    .build();
            vectorsBuilder.setSparseVectors(sparseConfig);
        }

        client.createCollectionAsync(knowledgeDocCollection, vectorsBuilder.build()).get();
        log.info("集合 {} 创建成功 vectorDim={} sparse={}",
                knowledgeDocCollection, vectorDim, sparseVectorEnabled);
    }
}