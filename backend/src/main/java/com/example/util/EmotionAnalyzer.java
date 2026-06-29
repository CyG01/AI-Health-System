package com.example.util;

import com.example.entity.EmotionRecord;
import com.example.entity.UserMemory;
import com.example.entity.UserProfile;
import com.example.mapper.EmotionRecordMapper;
import com.example.mapper.UserProfileMapper;
import com.example.service.MemoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 情绪感知引擎。
 * 多维度识别用户情绪，驱动语气切换和计划自动调整。
 */
@Slf4j
@Component
public class EmotionAnalyzer {

    // --- 6种核心情绪关键词 ---
    private static final Map<String, Pattern> EMOTION_PATTERNS = new LinkedHashMap<>();

    static {
        EMOTION_PATTERNS.put("TIRED", Pattern.compile(
                "累|困|疲惫|疲劳|没力气|没精神|想睡|乏|倦|没劲|休息|不想动|缓一缓"));
        EMOTION_PATTERNS.put("FRUSTRATED", Pattern.compile(
                "沮丧|失败|做不到|太难|放弃|不想|厌倦|无聊|失望|没用|白费|没意思|坚持不了|算了|摆烂"));
        EMOTION_PATTERNS.put("EXCITED", Pattern.compile(
                "开心|兴奋|激动|太棒|好开心|完成|成功|达到|超过|进步|有成就|自豪|爽|坚持.*?天|打卡.*?天"));
        EMOTION_PATTERNS.put("ANXIOUS", Pattern.compile(
                "焦虑|紧张|担心|害怕|压力|烦恼|愁|心烦|睡不着|不安|怕|慌|怎么办"));
        EMOTION_PATTERNS.put("PAIN", Pattern.compile(
                "疼|痛|酸|麻|肿|伤|扭|拉伤|不舒服|难受|炎症|肌肉酸痛|抽筋|岔气"));
    }

    /** 情绪 → 推荐语气 */
    private static final Map<String, String> EMOTION_TO_TONE = Map.of(
            "TIRED", "COMFORTING",
            "FRUSTRATED", "COMFORTING",
            "EXCITED", "CELEBRATORY",
            "ANXIOUS", "NEUTRAL",
            "PAIN", "COMFORTING",
            "NEUTRAL", "NEUTRAL"
    );

    private final EmotionRecordMapper emotionRecordMapper;
    private final UserProfileMapper userProfileMapper;
    private final MemoryService memoryService;

    public EmotionAnalyzer(EmotionRecordMapper emotionRecordMapper,
                           UserProfileMapper userProfileMapper,
                           MemoryService memoryService) {
        this.emotionRecordMapper = emotionRecordMapper;
        this.userProfileMapper = userProfileMapper;
        this.memoryService = memoryService;
    }

    /**
     * 返回识别结果：EmotionResult（情绪类型 + 置信度 + 推荐语气）
     */
    public EmotionResult analyze(Long userId, Long sessionId, String text) {
        // 1. 关键词快速分类
        for (Map.Entry<String, Pattern> entry : EMOTION_PATTERNS.entrySet()) {
            if (entry.getValue().matcher(text).find()) {
                BigDecimal confidence = calcConfidence(text, entry.getValue());
                EmotionResult result = new EmotionResult(
                        entry.getKey(), confidence, EMOTION_TO_TONE.get(entry.getKey()), text);
                // 记录到数据库
                saveEmotionRecord(userId, sessionId, result);
                // 情绪驱动调整
                handleEmotionDrivenAction(userId, result);
                return result;
            }
        }

        // 2. 默认中立
        EmotionResult neutral = new EmotionResult("NEUTRAL", new BigDecimal("0.60"), "NEUTRAL", text);
        saveEmotionRecord(userId, sessionId, neutral);
        return neutral;
    }

    /**
     * 情绪驱动的计划调整
     */
    public void handleEmotionDrivenAction(Long userId, EmotionResult result) {
        String action = "NONE";

        // 连续3天检测到疲惫/沮丧/焦虑/疼痛 → 降低强度
        if (isNegativeEmotion(result.emotionType)) {
            int negativeDays = emotionRecordMapper.countNegativeDays(userId, 3);
            if (negativeDays >= 3) {
                action = "REDUCE_INTENSITY";
                log.info("用户连续{}天负面情绪，建议降低计划强度 userId={}", negativeDays, userId);
            }
        }

        // 疼痛 → 立即暂停高强度计划，存入记忆
        if ("PAIN".equals(result.emotionType)) {
            action = "REDUCE_INTENSITY";
            memoryService.store(userId,
                    "用户反馈身体疼痛：" + result.originalText,
                    "INJURY", 7, "USER_INPUT");
            log.info("检测到疼痛信号，已存储记忆并建议降强度 userId={}", userId);
        }

        // 兴奋 → 询问是否提升计划难度
        if ("EXCITED".equals(result.emotionType) && result.confidence.doubleValue() > 0.8) {
            action = "PUSH_ENCOURAGEMENT";
            log.info("检测到用户兴奋情绪，推荐进阶挑战 userId={}", userId);
        }

        // 更新记录中的动作
        List<EmotionRecord> recent = emotionRecordMapper.findRecentByUser(userId, 1);
        if (!recent.isEmpty()) {
            EmotionRecord latest = recent.get(0);
            latest.setActionTaken(action);
            emotionRecordMapper.updateById(latest);
        }
    }

    /**
     * 获取推荐的语气模板ID
     */
    public String getRecommendedTone(Long userId) {
        // 优先使用用户偏好语气
        var profile = userProfileMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.example.entity.UserProfile>()
                        .eq(com.example.entity.UserProfile::getUserId, userId));
        if (profile != null && profile.getPreferredTone() != null) {
            return profile.getPreferredTone();
        }
        // 默认中性
        return "NEUTRAL";
    }

    /**
     * 获取情绪对应的 Prompt 模板名称
     */
    public String getTonePromptTemplate(String tone) {
        return "coach_tone_" + tone.toLowerCase();
    }

    private void saveEmotionRecord(Long userId, Long sessionId, EmotionResult result) {
        try {
            EmotionRecord record = new EmotionRecord();
            record.setUserId(userId);
            record.setSessionId(sessionId);
            record.setEmotionType(result.emotionType);
            record.setConfidence(result.confidence);
            record.setOriginalText(truncate(result.originalText, 200));
            record.setTriggeredTone(result.recommendedTone);
            record.setActionTaken("NONE");
            record.setCreatedAt(LocalDateTime.now());
            emotionRecordMapper.insert(record);
        } catch (Exception e) {
            log.warn("保存情绪记录失败 userId={}", userId, e);
        }
    }

    private BigDecimal calcConfidence(String text, Pattern pattern) {
        long matches = pattern.matcher(text).results().count();
        double raw = Math.min(0.95, 0.5 + matches * 0.15);
        return new BigDecimal(raw).setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isNegativeEmotion(String type) {
        return "TIRED".equals(type) || "FRUSTRATED".equals(type)
                || "ANXIOUS".equals(type) || "PAIN".equals(type);
    }

    private String truncate(String s, int maxLen) {
        return s != null && s.length() > maxLen ? s.substring(0, maxLen) : s;
    }

    /**
     * 情绪分析结果
     */
    public record EmotionResult(
            String emotionType,
            BigDecimal confidence,
            String recommendedTone,
            String originalText
    ) {}
}