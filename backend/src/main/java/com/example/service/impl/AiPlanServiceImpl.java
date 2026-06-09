package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.BusinessException;
import com.example.convert.AiPlanConvert;
import com.example.dto.PlanGenerateDTO;
import com.example.entity.AiPlan;
import com.example.mapper.AiPlanMapper;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.properties.DeepSeekProperties;
import com.example.service.AiPlanService;
import com.example.service.DeepSeekService;
import com.example.service.HealthService;
import com.example.vo.AiPlanDetailVO;
import com.example.vo.AiPlanVO;
import com.example.vo.HealthRecordVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AiPlanServiceImpl implements AiPlanService {

    private static final String GLOBAL_CACHE_KEY = "ai:plan:global:%s";
    private static final String USER_LIMIT_KEY = "ai:plan:limit:%d:%s";
    private static final String USER_CACHE_KEY = "ai:plan:user:%d:%d";
    private static final String ACTIVE_PLAN_CACHE_KEY = "ai:plan:active:%d";
    private static final int DAILY_LIMIT = 3;
    private static final long GLOBAL_CACHE_TTL = 30;
    private static final long USER_CACHE_TTL = 7;

    private final HealthService healthService;
    private final DeepSeekService deepSeekService;
    private final DeepSeekCostMonitor deepSeekCostMonitor;
    private final DeepSeekProperties deepSeekProperties;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AiPlanMapper aiPlanMapper;
    private final AiPlanConvert aiPlanConvert;
    private final ObjectMapper objectMapper;
    private final AiPlanCollectionMapper aiPlanCollectionMapper;
    private final AiPlanServiceImpl self;

    public AiPlanServiceImpl(HealthService healthService,
                             DeepSeekService deepSeekService,
                             DeepSeekCostMonitor deepSeekCostMonitor,
                             DeepSeekProperties deepSeekProperties,
                             RedisTemplate<String, Object> redisTemplate,
                             AiPlanMapper aiPlanMapper,
                             AiPlanConvert aiPlanConvert,
                             ObjectMapper objectMapper,
                             AiPlanCollectionMapper aiPlanCollectionMapper,
                             @Lazy AiPlanServiceImpl self) {
        this.healthService = healthService;
        this.deepSeekService = deepSeekService;
        this.deepSeekCostMonitor = deepSeekCostMonitor;
        this.deepSeekProperties = deepSeekProperties;
        this.redisTemplate = redisTemplate;
        this.aiPlanMapper = aiPlanMapper;
        this.aiPlanConvert = aiPlanConvert;
        this.objectMapper = objectMapper;
        this.aiPlanCollectionMapper = aiPlanCollectionMapper;
        this.self = self;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiPlanDetailVO generatePlan(PlanGenerateDTO dto, Long userId) {
        if (deepSeekCostMonitor.isGlobalCostExceeded()) {
            throw new BusinessException("今日AI调用额度已用尽，请明天再试");
        }

        HealthRecordVO healthRecord = healthService.getLatestHealthRecord(userId);

        String featureHash = computeFeatureHash(
                healthRecord.getHeight(), healthRecord.getWeight(), healthRecord.getGoal(),
                dto.getDurationDays(), dto.getIntensity());

        String globalCacheKey = String.format(GLOBAL_CACHE_KEY, featureHash);
        Object cachedContent = redisTemplate.opsForValue().get(globalCacheKey);
        if (cachedContent != null) {
            log.info("命中L2全局缓存 featureHash={}", featureHash);
            AiPlan cloned = clonePlanForUser(userId, dto, cachedContent.toString());
            aiPlanMapper.insert(cloned);
            cachePlan(cloned);
            return aiPlanConvert.toAiPlanDetailVO(cloned);
        }

        String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String limitKey = String.format(USER_LIMIT_KEY, userId, dateStr);
        Long callCount = redisTemplate.opsForValue().increment(limitKey, 1);
        if (callCount != null && callCount == 1) {
            redisTemplate.expire(limitKey, 1, TimeUnit.DAYS);
        }
        if (callCount != null && callCount > DAILY_LIMIT) {
            throw new BusinessException("今日AI计划生成次数已达上限(3次)，请明天再试");
        }

        String model = selectModel(dto, healthRecord);

        String preference = buildPreference(dto);
        String aiContent = deepSeekService.callApi(
                healthRecord.getHeight(), healthRecord.getWeight(),
                healthRecord.getGoal(), dto.getDurationDays(),
                preference, model);

        AiPlan plan = new AiPlan();
        plan.setUserId(userId);
        plan.setPlanType(dto.getPlanType());
        plan.setPlanName(buildPlanName(dto));
        plan.setDurationDays(dto.getDurationDays());
        plan.setAiContent(aiContent);
        plan.setStartDate(LocalDate.now());
        plan.setStatus(1);

        aiPlanMapper.insert(plan);
        log.info("AI计划生成成功 userId={} planId={} model={}", userId, plan.getId(), model);

        redisTemplate.opsForValue().set(globalCacheKey, aiContent, GLOBAL_CACHE_TTL, TimeUnit.DAYS);
        cachePlan(plan);

        return aiPlanConvert.toAiPlanDetailVO(plan);
    }

    @Override
    @Async
    public void generatePlanStream(PlanGenerateDTO dto, Long userId, SseEmitter emitter) {
        try {
            if (deepSeekCostMonitor.isGlobalCostExceeded()) {
                emitter.send(SseEmitter.event().name("error").data("今日AI调用额度已用尽，请明天再试"));
                emitter.send(SseEmitter.event().name("message").data("[ERROR]"));
                emitter.complete();
                return;
            }

            HealthRecordVO healthRecord = healthService.getLatestHealthRecord(userId);

            String featureHash = computeFeatureHash(
                    healthRecord.getHeight(), healthRecord.getWeight(), healthRecord.getGoal(),
                    dto.getDurationDays(), dto.getIntensity());

            String globalCacheKey = String.format(GLOBAL_CACHE_KEY, featureHash);
            Object cachedContent = redisTemplate.opsForValue().get(globalCacheKey);
            if (cachedContent != null) {
                log.info("命中L2全局缓存 featureHash={}", featureHash);
                ((AiPlanServiceImpl) self).cloneAndCachePlan(userId, dto, cachedContent.toString());

                emitter.send(SseEmitter.event().name("message").data(cachedContent.toString()));
                emitter.send(SseEmitter.event().name("message").data("[DONE]"));
                emitter.complete();
                return;
            }

            String dateStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
            String limitKey = String.format(USER_LIMIT_KEY, userId, dateStr);
            Long callCount = redisTemplate.opsForValue().increment(limitKey, 1);
            if (callCount != null && callCount == 1) {
                redisTemplate.expire(limitKey, 1, TimeUnit.DAYS);
            }
            if (callCount != null && callCount > DAILY_LIMIT) {
                emitter.send(SseEmitter.event().name("error").data("今日AI计划生成次数已达上限(3次)，请明天再试"));
                emitter.send(SseEmitter.event().name("message").data("[ERROR]"));
                emitter.complete();
                return;
            }

            String model = selectModel(dto, healthRecord);
            String preference = buildPreference(dto);

            StringBuilder fullContent = new StringBuilder();

            deepSeekService.callApiStream(
                    healthRecord.getHeight(), healthRecord.getWeight(),
                    healthRecord.getGoal(), dto.getDurationDays(),
                    preference, model)
                    .doOnNext(chunk -> {
                        try {
                            if (fullContent.length() == 0 && chunk.trim().isEmpty()) {
                                return;
                            }
                            emitter.send(SseEmitter.event().name("message").data(chunk));
                            fullContent.append(chunk);
                        } catch (Exception e) {
                            log.error("SSE发送失败", e);
                        }
                    })
                    .doOnError(error -> {
                        log.error("AI流式生成失败 userId={}", userId, error);
                        try {
                            emitter.send(SseEmitter.event().name("error")
                                    .data(error instanceof BusinessException ? error.getMessage() : "AI服务调用失败"));
                            emitter.send(SseEmitter.event().name("message").data("[ERROR]"));
                        } catch (Exception ex) {
                            log.error("SSE错误发送失败", ex);
                        }
                        emitter.completeWithError(error);
                    })
                    .doOnComplete(() -> {
                        try {
                            String aiContent = fullContent.toString();
                            if (aiContent.isBlank()) {
                                emitter.send(SseEmitter.event().name("error").data("AI返回内容为空"));
                                emitter.send(SseEmitter.event().name("message").data("[ERROR]"));
                                emitter.complete();
                                return;
                            }

                            ((AiPlanServiceImpl) self).savePlanWithContent(userId, dto, aiContent, globalCacheKey);

                            emitter.send(SseEmitter.event().name("message").data("[DONE]"));
                            emitter.complete();
                        } catch (Exception e) {
                            log.error("计划保存失败 userId={}", userId, e);
                            try {
                                emitter.send(SseEmitter.event().name("error").data("计划保存失败"));
                                emitter.send(SseEmitter.event().name("message").data("[ERROR]"));
                            } catch (Exception ex) {
                                log.error("SSE错误发送失败", ex);
                            }
                            emitter.completeWithError(e);
                        }
                    })
                    .subscribe();
        } catch (Exception e) {
            log.error("流式生成计划异常 userId={}", userId, e);
            try {
                emitter.send(SseEmitter.event().name("error").data("系统繁忙，请稍后重试"));
                emitter.send(SseEmitter.event().name("message").data("[ERROR]"));
                emitter.complete();
            } catch (Exception ex) {
                log.error("SSE错误发送失败", ex);
            }
        }
    }

    @Override
    public Page<AiPlanVO> getPlanList(Long userId, int page, int size, String keyword) {
        Page<AiPlan> pageParam = new Page<>(page, size);
        LambdaQueryWrapper<AiPlan> wrapper = new LambdaQueryWrapper<AiPlan>()
                .eq(AiPlan::getUserId, userId)
                .like(!isBlank(keyword), AiPlan::getPlanName, keyword)
                .orderByDesc(AiPlan::getCreateTime);
        Page<AiPlan> result = aiPlanMapper.selectPage(pageParam, wrapper);
        Page<AiPlanVO> voPage = new Page<>(result.getCurrent(), result.getSize(), result.getTotal());
        voPage.setRecords(result.getRecords().stream().map(aiPlanConvert::toAiPlanVO).toList());
        return voPage;
    }

    private static boolean isBlank(String str) {
        return str == null || str.isBlank();
    }

    @Override
    public AiPlanDetailVO getPlanDetail(Long planId, Long userId) {
        AiPlan plan = getOwnPlan(planId, userId);
        return aiPlanConvert.toAiPlanDetailVO(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activePlan(Long planId, Long userId) {
        AiPlan plan = getOwnPlan(planId, userId);

        AiPlan updateWrapper = new AiPlan();
        updateWrapper.setStatus(0);
        LambdaQueryWrapper<AiPlan> deactivateWrapper = new LambdaQueryWrapper<AiPlan>()
                .eq(AiPlan::getUserId, userId)
                .eq(AiPlan::getStatus, 1);
        aiPlanMapper.update(updateWrapper, deactivateWrapper);

        plan.setStatus(1);
        plan.setUpdateTime(LocalDateTime.now());
        aiPlanMapper.updateById(plan);

        String activeKey = String.format(ACTIVE_PLAN_CACHE_KEY, userId);
        redisTemplate.delete(activeKey);
        log.info("切换生效计划 userId={} planId={}", userId, planId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(Long planId, Long userId) {
        AiPlan plan = getOwnPlan(planId, userId);
        aiPlanMapper.deleteById(plan.getId());
        String cacheKey = String.format(USER_CACHE_KEY, userId, planId);
        redisTemplate.delete(cacheKey);
        log.info("删除AI计划 userId={} planId={}", userId, planId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void savePlanWithContent(Long userId, PlanGenerateDTO dto, String aiContent, String globalCacheKey) {
        AiPlan plan = new AiPlan();
        plan.setUserId(userId);
        plan.setPlanType(dto.getPlanType());
        plan.setPlanName(buildPlanName(dto));
        plan.setDurationDays(dto.getDurationDays());
        plan.setAiContent(aiContent);
        plan.setStartDate(LocalDate.now());
        plan.setStatus(1);

        aiPlanMapper.insert(plan);

        redisTemplate.opsForValue().set(globalCacheKey, aiContent, GLOBAL_CACHE_TTL, TimeUnit.DAYS);
        cachePlan(plan);

        log.info("AI计划保存成功 userId={} planId={}", userId, plan.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public void cloneAndCachePlan(Long userId, PlanGenerateDTO dto, String cachedContent) {
        AiPlan cloned = clonePlanForUser(userId, dto, cachedContent);
        aiPlanMapper.insert(cloned);
        cachePlan(cloned);
    }

    private AiPlan getOwnPlan(Long planId, Long userId) {
        LambdaQueryWrapper<AiPlan> wrapper = new LambdaQueryWrapper<AiPlan>()
                .eq(AiPlan::getId, planId)
                .eq(AiPlan::getUserId, userId);
        AiPlan plan = aiPlanMapper.selectOne(wrapper);
        if (plan == null) {
            throw new BusinessException(404, "计划不存在");
        }
        return plan;
    }

    private AiPlan clonePlanForUser(Long userId, PlanGenerateDTO dto, String aiContent) {
        AiPlan plan = new AiPlan();
        plan.setUserId(userId);
        plan.setPlanType(dto.getPlanType());
        plan.setPlanName(buildPlanName(dto));
        plan.setDurationDays(dto.getDurationDays());
        plan.setAiContent(aiContent);
        plan.setStartDate(LocalDate.now());
        plan.setStatus(1);
        return plan;
    }

    private void cachePlan(AiPlan plan) {
        String userCacheKey = String.format(USER_CACHE_KEY, plan.getUserId(), plan.getId());
        redisTemplate.opsForValue().set(userCacheKey, plan.getAiContent(), USER_CACHE_TTL, TimeUnit.DAYS);
    }

    private String computeFeatureHash(BigDecimal height, BigDecimal weight, String goal,
                                      Integer durationDays, String intensity) {
        String raw = String.format("%.1f_%.1f_%s_%d_%s",
                height, weight, goal, durationDays, intensity != null ? intensity : "");
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException("系统内部错误");
        }
    }

    private String selectModel(PlanGenerateDTO dto, HealthRecordVO healthRecord) {
        boolean hasSevereHistory = healthRecord.getDiseaseHistory() != null
                && !healthRecord.getDiseaseHistory().isBlank();
        if (dto.getDurationDays() <= 7 && !hasSevereHistory) {
            return "deepseek-chat";
        }
        return "deepseek-reasoner";
    }

    private String buildPreference(PlanGenerateDTO dto) {
        StringBuilder sb = new StringBuilder();
        if (dto.getIntensity() != null && !dto.getIntensity().isBlank()) {
            sb.append("运动强度偏好:").append(dto.getIntensity()).append(";");
        }
        if (dto.getTastePreference() != null && !dto.getTastePreference().isBlank()) {
            sb.append("口味偏好:").append(dto.getTastePreference()).append(";");
        }
        if (sb.isEmpty()) {
            sb.append("通用");
        }
        return sb.toString();
    }

    private String buildPlanName(PlanGenerateDTO dto) {
        String typeLabel = "sport".equals(dto.getPlanType()) ? "运动计划" : "饮食计划";
        return typeLabel + "-" + dto.getDurationDays() + "天-" + LocalDate.now();
    }
}
