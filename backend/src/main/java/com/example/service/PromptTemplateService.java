package com.example.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.entity.PromptTemplate;
import com.example.llmops.PromptVersionManager;
import com.example.mapper.PromptTemplateMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Prompt 模板服务 —— 从数据库加载模板，缓存到 Redis，支持热更新。
 * 激活状态管理统一委托给 {@link PromptVersionManager}，避免两套独立的状态管理。
 */
@Service
public class PromptTemplateService {

    private static final Logger log = LoggerFactory.getLogger(PromptTemplateService.class);

    private static final String REDIS_KEY_PREFIX = "prompt:template:";
    private static final long REDIS_TTL_HOURS = 24;

    private final PromptTemplateMapper promptTemplateMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    private final PromptVersionManager promptVersionManager;

    public PromptTemplateService(PromptTemplateMapper promptTemplateMapper,
                                  RedisTemplate<String, Object> redisTemplate,
                                  PromptVersionManager promptVersionManager) {
        this.promptTemplateMapper = promptTemplateMapper;
        this.redisTemplate = redisTemplate;
        this.promptVersionManager = promptVersionManager;
    }

    @PostConstruct
    public void init() {
        refreshAllTemplates();
    }

    /**
     * 每 10 分钟刷新一次模板缓存（同步所有 isActive=true 的模板到 Redis）。
     */
    @Scheduled(fixedDelay = 600_000)
    public void refreshAllTemplates() {
        LambdaQueryWrapper<PromptTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromptTemplate::getIsActive, true);
        var templates = promptTemplateMapper.selectList(wrapper);
        for (PromptTemplate t : templates) {
            String key = buildKey(t.getTemplateKey(), t.getVersion());
            redisTemplate.opsForValue().set(key, t.getTemplateContent(), REDIS_TTL_HOURS, TimeUnit.HOURS);
        }
        log.info("Prompt模板刷新完成，共加载{}个模板", templates.size());
    }

    /**
     * 获取模板内容（取当前激活版本，委托 PromptVersionManager）。
     */
    public String getTemplate(String templateKey) {
        return getTemplate(templateKey, null);
    }

    /**
     * 获取模板内容（指定版本）。
     */
    public String getTemplate(String templateKey, Integer version) {
        // 使用统一的版本管理获取激活版本号
        int targetVersion = (version != null) ? version : promptVersionManager.getActiveVersion(templateKey);

        // 先从 Redis 获取
        String key = buildKey(templateKey, targetVersion);
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return cached.toString();
        }

        // Redis 未命中，从 DB 加载
        LambdaQueryWrapper<PromptTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(PromptTemplate::getTemplateKey, templateKey)
                .eq(PromptTemplate::getVersion, targetVersion);
        PromptTemplate template = promptTemplateMapper.selectOne(wrapper);
        if (template == null) {
            log.warn("Prompt模板未找到: key={} version={}", templateKey, targetVersion);
            return null;
        }

        // 写入缓存
        key = buildKey(templateKey, template.getVersion());
        redisTemplate.opsForValue().set(key, template.getTemplateContent(), REDIS_TTL_HOURS, TimeUnit.HOURS);
        return template.getTemplateContent();
    }

    public String getGlobalCacheKey() {
        return null;
    }

    /**
     * 获取模板版本号（委托 PromptVersionManager）。
     */
    public Integer getTemplateVersion(String templateKey) {
        return promptVersionManager.getActiveVersion(templateKey);
    }

    private String buildKey(String templateKey, Integer version) {
        return REDIS_KEY_PREFIX + templateKey + ":" + (version != null ? version : 1);
    }
}