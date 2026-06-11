package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.convert.AiPlanConvert;
import com.example.dto.PlanGenerateDTO;
import com.example.entity.AiCallAuditLog;
import com.example.entity.AiPlan;
import com.example.entity.AiPlanDetail;
import com.example.entity.AiPlanFeedback;
import com.example.mapper.AiCallAuditLogMapper;
import com.example.mapper.AiPlanDetailMapper;
import com.example.mapper.AiPlanFeedbackMapper;
import com.example.mapper.AiPlanMapper;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.properties.DeepSeekProperties;
import com.example.service.AiPlanService;
import com.example.service.DeepSeekService;
import com.example.service.HealthService;
import com.example.service.MemoryService;
import com.example.resilience.ModelRouter;
import com.example.util.AiResponseParser;
import com.example.util.DataMaskingService;
import com.example.util.PromptSanitizer;
import com.example.vo.AiPlanDetailVO;
import com.example.vo.AiPlanVO;
import com.example.vo.HealthRecordVO;
import com.example.dto.AiTaskMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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
    private final AiPlanDetailMapper aiPlanDetailMapper;
    private final AiPlanFeedbackMapper aiPlanFeedbackMapper;
    private final AiPlanConvert aiPlanConvert;
    private final ObjectMapper objectMapper;
    private final AiPlanServiceImpl self;
    private final AiCallAuditLogMapper auditLogMapper;
    private final DataMaskingService dataMaskingService;
    private final MemoryService memoryService;
    private final ModelRouter modelRouter;

    public AiPlanServiceImpl(HealthService healthService,
                             DeepSeekService deepSeekService,
                             DeepSeekCostMonitor deepSeekCostMonitor,
                             DeepSeekProperties deepSeekProperties,
                             RedisTemplate<String, Object> redisTemplate,
                             AiPlanMapper aiPlanMapper,
                             AiPlanDetailMapper aiPlanDetailMapper,
                             AiPlanFeedbackMapper aiPlanFeedbackMapper,
                             AiPlanConvert aiPlanConvert,
                             ObjectMapper objectMapper,
                             @Lazy AiPlanServiceImpl self,
                             AiCallAuditLogMapper auditLogMapper,
                             DataMaskingService dataMaskingService,
                             MemoryService memoryService,
                             ModelRouter modelRouter) {
        this.healthService = healthService;
        this.deepSeekService = deepSeekService;
        this.deepSeekCostMonitor = deepSeekCostMonitor;
        this.deepSeekProperties = deepSeekProperties;
        this.redisTemplate = redisTemplate;
        this.aiPlanMapper = aiPlanMapper;
        this.aiPlanDetailMapper = aiPlanDetailMapper;
        this.aiPlanFeedbackMapper = aiPlanFeedbackMapper;
        this.aiPlanConvert = aiPlanConvert;
        this.objectMapper = objectMapper;
        this.self = self;
        this.auditLogMapper = auditLogMapper;
        this.dataMaskingService = dataMaskingService;
        this.memoryService = memoryService;
        this.modelRouter = modelRouter;
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
        String userProfile = buildUserProfile(healthRecord, userId);
        String planTypeLabel = getPlanTypeLabel(dto.getPlanType());
        String planTypeNote = getPlanTypeNote(dto.getPlanType());

        // 调用AI生成计划（DeepSeek 主路径 + ModelRouter 降级）
        String aiContent;
        try {
            aiContent = deepSeekService.callApi(
                    BigDecimal.valueOf(healthRecord.getHeight()), BigDecimal.valueOf(healthRecord.getWeight()),
                    healthRecord.getGoal(), dto.getDurationDays(),
                    preference, userProfile, planTypeLabel, planTypeNote, model);
        } catch (Exception e) {
            log.warn("DeepSeek V1 计划生成失败，降级到 ModelRouter userId={}", userId, e);
            try {
                String fallbackPrompt = buildFallbackPrompt(healthRecord, dto, userProfile, planTypeLabel, planTypeNote);
                aiContent = modelRouter.singleChat(
                        "你是专业健康教练，根据用户数据生成健康计划JSON。只输出JSON。",
                        fallbackPrompt,
                        "plan_generate");
            } catch (Exception fallbackError) {
                log.error("ModelRouter 降级也失败 userId={}", userId, fallbackError);
                throw new BusinessException("AI服务暂时不可用，请稍后重试");
            }
        }

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

        // 解析并保存 detail
        savePlanDetails(plan.getId(), aiContent, dto.getPlanType());

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
            String userProfile = buildUserProfile(healthRecord, userId);
            String planTypeLabel = getPlanTypeLabel(dto.getPlanType());
            String planTypeNote = getPlanTypeNote(dto.getPlanType());

            StringBuilder fullContent = new StringBuilder();
            deepSeekService.callApiStream(
                        BigDecimal.valueOf(healthRecord.getHeight()), BigDecimal.valueOf(healthRecord.getWeight()),
                        healthRecord.getGoal(), dto.getDurationDays(),
                        preference, userProfile, planTypeLabel, planTypeNote, model)
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
        AiPlanDetailVO vo = aiPlanConvert.toAiPlanDetailVO(plan);

        LambdaQueryWrapper<AiPlanDetail> detailWrapper = new LambdaQueryWrapper<AiPlanDetail>()
                .eq(AiPlanDetail::getPlanId, planId)
                .orderByAsc(AiPlanDetail::getDaySequence);
        List<AiPlanDetail> details = aiPlanDetailMapper.selectList(detailWrapper);
        vo.setDetails(details.stream().map(aiPlanConvert::toDetailVO).toList());

        return vo;
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

        // 级联删除计划明细
        LambdaQueryWrapper<AiPlanDetail> detailWrapper = new LambdaQueryWrapper<AiPlanDetail>()
                .eq(AiPlanDetail::getPlanId, planId);
        aiPlanDetailMapper.delete(detailWrapper);

        // 级联删除反馈记录
        LambdaQueryWrapper<AiPlanFeedback> feedbackWrapper = new LambdaQueryWrapper<AiPlanFeedback>()
                .eq(AiPlanFeedback::getPlanId, planId);
        aiPlanFeedbackMapper.delete(feedbackWrapper);

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

        // 解析 AI 返回的 JSON 并保存 detail 记录
        savePlanDetails(plan.getId(), aiContent, dto.getPlanType());

        redisTemplate.opsForValue().set(globalCacheKey, aiContent, GLOBAL_CACHE_TTL, TimeUnit.DAYS);
        cachePlan(plan);

        log.info("AI计划保存成功 userId={} planId={}", userId, plan.getId());
    }

    private void savePlanDetails(Long planId, String aiContent, String planType) {
        try {
            JsonNode root = AiResponseParser.extractJson(aiContent);
            JsonNode days = root.path("days");
            if (!days.isArray()) {
                log.warn("AI返回内容中无days数组 planId={}", planId);
                return;
            }
            for (JsonNode dayNode : days) {
                int daySeq = dayNode.path("d").asInt();
                JsonNode items = dayNode.path("items");
                if (!items.isArray()) continue;
                for (JsonNode itemText : items) {
                    String text = itemText.asText();
                    if (text == null || text.isBlank()) continue;

                    AiPlanDetail detail = new AiPlanDetail();
                    detail.setPlanId(planId);
                    detail.setDaySequence(daySeq);
                    detail.setItemType(planType);
                    detail.setItemName(text);
                    detail.setTargetAmount("");
                    detail.setStatus(0);
                    aiPlanDetailMapper.insert(detail);
                }
            }
            log.info("AI计划detail保存成功 planId={}", planId);
        } catch (Exception e) {
            log.error("解析AI计划detail失败 planId={}", planId, e);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void cloneAndCachePlan(Long userId, PlanGenerateDTO dto, String cachedContent) {
        AiPlan cloned = clonePlanForUser(userId, dto, cachedContent);
        aiPlanMapper.insert(cloned);
        savePlanDetails(cloned.getId(), cachedContent, dto.getPlanType());
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

    private String computeFeatureHash(Integer height, Integer weight, String goal,
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
        // 检查主模型健康状态，不健康时记录告警（实际降级由 DeepSeekService 内部重试处理）
        if (!modelRouter.isPrimaryHealthy()) {
            log.warn("DeepSeek 主模型健康状态异常，将以告警模式调用 model={}",
                    modelRouter.getModelStatus().get("deepseek"));
        }

        boolean hasSevereHistory = healthRecord.getDiseaseHistory() != null
                && !healthRecord.getDiseaseHistory().isBlank();
        if (dto.getDurationDays() <= 7 && !hasSevereHistory) {
            return "deepseek-chat";
        }
        return "deepseek-reasoner";
    }

    /**
     * 构建 ModelRouter 降级时使用的 Prompt（将结构化参数转为自然语言描述）
     */
    private String buildFallbackPrompt(HealthRecordVO healthRecord, PlanGenerateDTO dto,
                                        String userProfile, String planTypeLabel, String planTypeNote) {
        return String.format(
                "请为用户生成一个%d天的%s健康计划。\n" +
                "用户信息：身高%.1fcm，体重%.1fkg，健康目标：%s。\n" +
                "%s\n" +
                "计划类型：%s。%s\n" +
                "偏好：%s\n" +
                "请输出JSON格式：{\"days\":[{\"d\":天数,\"items\":[\"具体可执行任务1\",\"任务2\"]}]}",
                dto.getDurationDays(), planTypeLabel,
                healthRecord.getHeight(), healthRecord.getWeight(),
                healthRecord.getGoal() != null ? healthRecord.getGoal() : "未设定",
                userProfile,
                planTypeLabel, planTypeNote,
                buildPreference(dto)
        );
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
        String typeLabel = getPlanTypeLabel(dto.getPlanType());
        return typeLabel + "-" + dto.getDurationDays() + "天-" + LocalDate.now();
    }

    private String getPlanTypeLabel(String planType) {
        return switch (planType) {
            case "sport" -> "运动计划";
            case "diet" -> "饮食计划";
            case "comprehensive" -> "综合计划";
            case "rehabilitation" -> "康复计划";
            case "meditation" -> "冥想放松计划";
            default -> "健康计划";
        };
    }

    private String getPlanTypeNote(String planType) {
        return switch (planType) {
            case "sport" -> "运动类计划输出运动项目建议(含具体组数、时长、强度)";
            case "diet" -> "饮食类计划输出每餐食物搭配建议(含具体食材克数、热量)";
            case "comprehensive" -> "综合计划需同时包含运动安排与饮食建议,按天交替或结合";
            case "rehabilitation" -> "康复计划需低强度、渐进式,针对用户病史设计恢复性训练与营养建议";
            case "meditation" -> "冥想放松计划包含呼吸练习、正念冥想、瑜伽拉伸、渐进式肌肉放松等,每天15-30分钟";
            default -> "输出运动项目建议和食物搭配建议";
        };
    }

    private String buildUserProfile(HealthRecordVO healthRecord, Long userId) {
        StringBuilder sb = new StringBuilder();
        if (healthRecord.getDiseaseHistory() != null && !healthRecord.getDiseaseHistory().isBlank()) {
            // 敏感疾病泛化 + 注入防护
            String masked = dataMaskingService.maskDiseaseHistory(healthRecord.getDiseaseHistory());
            String sanitized = PromptSanitizer.sanitize(masked);
            if (!sanitized.isBlank()) {
                sb.append("病史:").append(sanitized).append(";");
            }
        }
        if (healthRecord.getAllergyHistory() != null && !healthRecord.getAllergyHistory().isBlank()) {
            String masked = dataMaskingService.maskAllergyHistory(healthRecord.getAllergyHistory());
            String sanitized = PromptSanitizer.sanitize(masked);
            if (!sanitized.isBlank()) {
                sb.append("过敏史:").append(sanitized).append(";");
            }
        }
        if (healthRecord.getFamilyHistory() != null && !healthRecord.getFamilyHistory().isBlank()) {
            String masked = dataMaskingService.maskFamilyHistory(healthRecord.getFamilyHistory());
            String sanitized = PromptSanitizer.sanitize(masked);
            if (!sanitized.isBlank()) {
                sb.append("家族病史:").append(sanitized).append(";");
            }
        }
        if (healthRecord.getMedication() != null && !healthRecord.getMedication().isBlank()) {
            String masked = dataMaskingService.maskMedication(healthRecord.getMedication());
            String sanitized = PromptSanitizer.sanitize(masked);
            if (!sanitized.isBlank()) {
                sb.append("当前用药:").append(sanitized).append(";");
            }
        }
        if (healthRecord.getExerciseHabit() != null && !healthRecord.getExerciseHabit().isBlank()) {
            String sanitized = PromptSanitizer.sanitize(healthRecord.getExerciseHabit());
            sb.append("运动习惯:").append(sanitized).append(";");
        }
        if (healthRecord.getDietHabit() != null && !healthRecord.getDietHabit().isBlank()) {
            String sanitized = PromptSanitizer.sanitize(healthRecord.getDietHabit());
            sb.append("饮食习惯:").append(sanitized).append(";");
        }
        if (healthRecord.getTargetWeight() != null) {
            sb.append("目标体重:").append(healthRecord.getTargetWeight()).append("kg;");
        }
        if (sb.isEmpty()) {
            sb.append("无特殊病史,一般健康人群;");
        }

        // Phase 2.1: 注入用户长期记忆上下文
        try {
            String memoryContext = memoryService.buildMemoryContext(userId,
                    "运动偏好 伤病 过敏 饮食禁忌 习惯 反馈", 5);
            if (!memoryContext.isEmpty()) {
                sb.append(" ").append(memoryContext);
            }
        } catch (Exception e) {
            log.warn("计划生成记忆检索失败 userId={}", userId, e);
        }

        return sb.toString();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeTask(Long detailId, Long userId) {
        AiPlanDetail detail = aiPlanDetailMapper.selectById(detailId);
        if (detail == null) {
            throw new BusinessException(404, "计划任务不存在");
        }
        AiPlan plan = aiPlanMapper.selectById(detail.getPlanId());
        if (plan == null || !plan.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此任务");
        }
        detail.setStatus(1);
        aiPlanDetailMapper.updateById(detail);
        log.info("用户完成任务 userId={} detailId={} planId={}", userId, detailId, detail.getPlanId());
    }

    /**
     * 同步生成 AI 计划（MQ Consumer 调用）。
     */
    public AiPlanDetailVO generatePlanSync(AiTaskMessage message) {
        try {
            PlanGenerateDTO dto = objectMapper.readValue(message.getPayload(), PlanGenerateDTO.class);
            return generatePlan(dto, message.getUserId());
        } catch (Exception e) {
            log.error("解析计划生成消息失败 taskId={}", message.getTaskId(), e);
            throw new RuntimeException("计划生成消息解析失败", e);
        }
    }
}
