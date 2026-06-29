package com.example.util;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Prompt 缓存服务 — 减少重复 Prompt 的 Token 消耗。
 * 使用 Caffeine 本地缓存，对 system prompt / 低变化频率的 prompt 做 SHA256 哈希去重。
 */
@Component
public class PromptCacheService {

    private static final Logger log = LoggerFactory.getLogger(PromptCacheService.class);

    /**
     * Prompt 内容缓存（promptHash → promptContent）。
     * 低变化频率的 system prompt 命中后直接复用，避免重复计算 Token。
     */
    private final Cache<String, String> promptCache = Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .recordStats()
            .build();

    /**
     * Prompt 结果缓存（promptHash → AI 响应）。
     * 相同输入的 prompt 直接返回缓存结果。
     */
    private final Cache<String, String> resultCache = Caffeine.newBuilder()
            .maximumSize(2000)
            .expireAfterWrite(30, TimeUnit.MINUTES)
            .recordStats()
            .build();

    /**
     * 语义指纹缓存 — 相同语义查询复用结果。
     */
    private final Cache<String, Boolean> semanticFingerprintCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();

    /**
     * 计算 Prompt 的 SHA256 哈希。
     */
    public String computeHash(String prompt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(prompt.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            return Integer.toHexString(prompt.hashCode());
        }
    }

    /**
     * 构建带参数的 prompt key。
     */
    public String buildKey(String promptTemplate, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder(promptTemplate);
        if (params != null) {
            for (var entry : params.entrySet()) {
                sb.append("|").append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        return computeHash(sb.toString());
    }

    /**
     * 获取缓存的 prompt。
     */
    public String getCachedPrompt(String hash) {
        return promptCache.getIfPresent(hash);
    }

    /**
     * 缓存 prompt。
     */
    public void cachePrompt(String hash, String prompt) {
        promptCache.put(hash, prompt);
        log.debug("Prompt已缓存 hash={}", hash.substring(0, 16));
    }

    /**
     * 获取缓存的 AI 响应结果。
     */
    public String getCachedResult(String hash) {
        String result = resultCache.getIfPresent(hash);
        if (result != null) {
            log.info("命中Prompt结果缓存 hash={}", hash.substring(0, 16));
        }
        return result;
    }

    /**
     * 缓存 AI 响应结果。
     */
    public void cacheResult(String hash, String result) {
        resultCache.put(hash, result);
        log.debug("AI响应结果已缓存 hash={}", hash.substring(0, 16));
    }

    /**
     * 使指定 hash 的缓存失效。
     */
    public void invalidate(String hash) {
        promptCache.invalidate(hash);
        resultCache.invalidate(hash);
    }

    /**
     * 获取缓存统计信息。
     */
    public String getStats() {
        return String.format(
                "PromptCache stats: promptHits=%d, promptMisses=%d, resultHits=%d, resultMisses=%d",
                promptCache.stats().hitCount(),
                promptCache.stats().missCount(),
                resultCache.stats().hitCount(),
                resultCache.stats().missCount()
        );
    }

    /**
     * 清空全部缓存。
     */
    public void clearAll() {
        promptCache.invalidateAll();
        resultCache.invalidateAll();
        log.info("PromptCache已全部清空");
    }
}