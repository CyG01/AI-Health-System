package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.dto.OnboardingRequest;
import com.example.entity.UserMemory;
import com.example.entity.UserProfile;
import com.example.mapper.UserProfileMapper;
import com.example.service.MemoryService;
import com.example.service.OnboardingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class OnboardingServiceImpl implements OnboardingService {

    /** 问卷问题定义 */
    private static final List<OnboardingQuestion> QUESTIONS = List.of(
            new OnboardingQuestion("你的健康目标是什么？",
                    Arrays.asList("减重", "增肌", "保持健康", "康复", "减压")),
            new OnboardingQuestion("你当前的运动基础如何？",
                    Arrays.asList("久坐不动", "偶尔运动", "规律运动", "专业训练")),
            new OnboardingQuestion("你是否有慢性疾病或运动损伤？",
                    Arrays.asList("高血压", "糖尿病", "心脏病", "膝盖损伤", "腰部损伤", "颈椎病", "肩周炎", "痛风", "哮喘", "无")),
            new OnboardingQuestion("你的饮食偏好或忌口是什么？",
                    Arrays.asList("素食", "蛋奶素", "不吃海鲜", "不吃辣", "不吃红肉", "低碳水", "清真", "无")),
            new OnboardingQuestion("你每天可用于运动的时间是多少？",
                    Arrays.asList("<30分钟", "30-60分钟", "60-90分钟", ">90分钟"))
    );

    private final UserProfileMapper profileMapper;
    private final MemoryService memoryService;

    public OnboardingServiceImpl(UserProfileMapper profileMapper, MemoryService memoryService) {
        this.profileMapper = profileMapper;
        this.memoryService = memoryService;
    }

    @Override
    @Transactional
    public UserProfile submitOnboarding(Long userId, OnboardingRequest request) {
        // 分离慢性疾病和运动损伤
        String conditions = request.getConditions();
        String diseases = "";
        String injuries = "";
        if (conditions != null && !conditions.equals("无")) {
            diseases = extractDiseases(conditions);
            injuries = extractInjuries(conditions);
        }

        // 创建/更新用户画像
        UserProfile profile = getProfile(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
            profile.setOnboardingCompleted(0);
        }

        profile.setHealthGoal(request.getHealthGoal());
        profile.setFitnessLevel(request.getFitnessLevel());
        profile.setChronicDiseases(diseases.isEmpty() ? "无" : diseases);
        profile.setInjuries(injuries.isEmpty() ? "无" : injuries);
        profile.setDietPreferences(normalize(request.getDietPreferences()));
        profile.setDailyAvailableMin(request.getDailyAvailableMin());
        profile.setSleepQuality(normalizeSingle(request.getSleepQuality()));
        profile.setStressLevel(normalizeSingle(request.getStressLevel()));
        profile.setPreferredTone("NEUTRAL");
        profile.setOnboardingCompleted(1);
        profile.setOnboardingCompletedAt(LocalDateTime.now());
        profile.setRegistrationDay(1);

        if (profile.getId() != null) {
            profileMapper.updateById(profile);
        } else {
            profileMapper.insert(profile);
        }

        // 生成初始记忆（重要性=8，来源=ONBOARDING）
        String summary = buildProfileSummary(userId);
        memoryService.store(userId, summary, "ONBOARDING", 8, "ONBOARDING");

        // 生成各维度独立记忆
        if (!"无".equals(diseases)) {
            memoryService.store(userId, "用户患有：" + diseases, "INJURY", 9, "ONBOARDING");
        }
        if (!"无".equals(injuries)) {
            memoryService.store(userId, "用户运动损伤：" + injuries, "INJURY", 9, "ONBOARDING");
        }
        if (request.getDietPreferences() != null && !request.getDietPreferences().equals("无")) {
            memoryService.store(userId, "用户饮食偏好/忌口：" + request.getDietPreferences(),
                    "PREFERENCE", 6, "ONBOARDING");
        }

        log.info("新手引导完成 userId={} goal={} fitnessLevel={}", userId,
                request.getHealthGoal(), request.getFitnessLevel());
        return profile;
    }

    @Override
    public UserProfile getProfile(Long userId) {
        LambdaQueryWrapper<UserProfile> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserProfile::getUserId, userId);
        return profileMapper.selectOne(wrapper);
    }

    @Override
    public boolean isOnboardingCompleted(Long userId) {
        UserProfile profile = getProfile(userId);
        return profile != null && profile.getOnboardingCompleted() != null
                && profile.getOnboardingCompleted() == 1;
    }

    @Override
    public String buildProfileSummary(Long userId) {
        UserProfile profile = getProfile(userId);
        if (profile == null) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("目标").append(translateGoal(profile.getHealthGoal()));
        sb.append("，运动基础").append(profile.getFitnessLevel());

        if (!"无".equals(profile.getChronicDiseases())) {
            sb.append("，患有").append(profile.getChronicDiseases());
        }
        if (!"无".equals(profile.getInjuries())) {
            sb.append("，").append(profile.getInjuries());
        }
        if (profile.getDietPreferences() != null && !profile.getDietPreferences().equals("无")) {
            sb.append("，饮食").append(profile.getDietPreferences());
        }
        sb.append("，每天可运动约").append(profile.getDailyAvailableMin()).append("分钟");
        return sb.toString();
    }

    @Override
    public String getActivationMessage(Long userId) {
        UserProfile profile = getProfile(userId);
        int day = profile != null && profile.getRegistrationDay() != null
                ? profile.getRegistrationDay() : 1;

        return switch (day) {
            case 1 -> "欢迎加入AI健康教练！你的专属计划已生成，点击查看你的第一次健康计划吧~";
            case 3 -> "你已经坚持了3天，真不错！坚持下去，身体会给你惊喜的。试试记录今天的饮食和运动吧？";
            case 7 -> "恭喜完成第一周！来看看你本周的健康总结，看看AI教练给你的专属分析~";
            default -> null;
        };
    }

    @Override
    public void incrementRegistrationDay(Long userId) {
        UserProfile profile = getProfile(userId);
        if (profile != null) {
            profile.setRegistrationDay((profile.getRegistrationDay() != null
                    ? profile.getRegistrationDay() : 0) + 1);
            profile.setLastActiveAt(LocalDateTime.now());
            profileMapper.updateById(profile);
        }
    }

    /** 获取问卷问题列表 */
    public List<OnboardingQuestion> getQuestions() {
        return QUESTIONS;
    }

    // --- 私有方法 ---

    /** 疾病/损伤关键词（与 SafetyCheckerService.COMMON_CONDITIONS 保持一致） */
    private static final List<String> DISEASE_KEYWORDS = List.of(
            "高血压", "糖尿病", "心脏病", "哮喘", "痛风", "骨质疏松",
            "颈椎病", "肩周炎", "腰椎间盘", "肥胖", "术后", "孕期"
    );

    /** 损伤关键词 */
    private static final List<String> INJURY_KEYWORDS = List.of(
            "膝盖损伤", "膝盖受伤", "膝伤", "半月板",
            "腰部损伤", "腰部受伤", "腰伤", "腰肌劳损",
            "肩部损伤", "肩关节", "肩袖损伤",
            "脚踝", "踝关节", "韧带", "扭伤", "骨折"
    );

    private String extractDiseases(String conditions) {
        return Arrays.stream(conditions.split("[,，]"))
                .map(String::trim)
                .filter(s -> DISEASE_KEYWORDS.stream().anyMatch(s::contains))
                .collect(Collectors.joining("、"));
    }

    private String extractInjuries(String conditions) {
        return Arrays.stream(conditions.split("[,，]"))
                .map(String::trim)
                .filter(s -> INJURY_KEYWORDS.stream().anyMatch(s::contains))
                .collect(Collectors.joining("、"));
    }

    private String normalize(String val) {
        return (val == null || val.isBlank() || "无".equals(val.trim())) ? "无" : val.trim();
    }

    private String normalizeSingle(String val) {
        return (val == null || val.isBlank()) ? null : val.trim();
    }

    private String translateGoal(String goal) {
        return switch (goal) {
            case "LOSE_WEIGHT" -> "减重";
            case "GAIN_MUSCLE" -> "增肌";
            case "STAY_HEALTHY" -> "保持健康";
            case "REHABILITATION" -> "康复";
            case "STRESS_RELIEF" -> "减压";
            default -> goal;
        };
    }

    public record OnboardingQuestion(String question, List<String> options) {}
}