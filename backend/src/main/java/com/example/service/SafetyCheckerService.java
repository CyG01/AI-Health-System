package com.example.service;

import com.example.entity.ComplianceRule;
import com.example.entity.HealthRecord;
import com.example.entity.SafetyRule;
import com.example.entity.UserProfile;
import com.example.mapper.ComplianceRuleMapper;
import com.example.mapper.HealthRecordMapper;
import com.example.mapper.SafetyRuleMapper;
import com.example.mapper.UserProfileMapper;
import com.example.vo.SafetyCheckResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 安全检查服务 — 硬编码规则表 + 合规校验 + 判别式 AI 第二道防线。
 * 在计划生成/调整链路中嵌入，拦截危险运动计划。
 */
@Slf4j
@Service
public class SafetyCheckerService {

    private final SafetyRuleMapper safetyRuleMapper;
    private final ComplianceRuleMapper complianceRuleMapper;
    private final HealthRecordMapper healthRecordMapper;
    private final UserProfileMapper userProfileMapper;

    /** 常见健康状况关键词映射（用于识别用户健康记录中的关键病症） */
    private static final String[] COMMON_CONDITIONS = {
            "高血压", "膝盖损伤", "腰部损伤", "孕期", "心脏病", "骨质疏松",
            "糖尿病", "颈椎病", "腰椎间盘", "肩周炎", "痛风", "哮喘",
            "肥胖", "术后"
    };

    public SafetyCheckerService(SafetyRuleMapper safetyRuleMapper,
                                 ComplianceRuleMapper complianceRuleMapper,
                                 HealthRecordMapper healthRecordMapper,
                                 UserProfileMapper userProfileMapper) {
        this.safetyRuleMapper = safetyRuleMapper;
        this.complianceRuleMapper = complianceRuleMapper;
        this.healthRecordMapper = healthRecordMapper;
        this.userProfileMapper = userProfileMapper;
    }

    /**
     * 对生成的计划任务进行安全检查。
     *
     * @param userId 用户ID
     * @param planItems 计划任务列表
     * @return 检查结果
     */
    public SafetyCheckResult checkPlan(Long userId, List<String> planItems) {
        SafetyCheckResult result = new SafetyCheckResult();

        if (planItems == null || planItems.isEmpty()) {
            return result;
        }

        // Step 1: 查询用户疾病史
        String conditions = getUserConditions(userId);

        // Step 2: 规则表硬匹配（仅在用户有已知疾病史时执行）
        if (!conditions.isEmpty()) {
            List<SafetyRule> matchedRules = safetyRuleMapper.matchByConditions(conditions);
            if (matchedRules != null && !matchedRules.isEmpty()) {
                for (SafetyRule rule : matchedRules) {
                    String[] forbidden = rule.getForbiddenKeywords().split(",");
                    for (String item : planItems) {
                        for (String keyword : forbidden) {
                            if (item.contains(keyword.trim())) {
                                result.addViolation(rule, item);
                                log.warn("安全检查拦截 userId={} rule={} item={}", userId,
                                        rule.getUserCondition(), item);
                            }
                        }
                    }
                }
            }
        }

        // Step 3: 合规校验（禁止医疗术语）— 始终执行，不依赖用户疾病史
        for (String item : planItems) {
            List<ComplianceRule> violations = complianceRuleMapper.matchByText(item);
            if (violations != null && !violations.isEmpty()) {
                for (ComplianceRule rule : violations) {
                    result.addComplianceIssue(rule, item);
                }
            }
        }

        // Step 4: 汇总信息
        if (!result.isPassed()) {
            StringBuilder msg = new StringBuilder("以下任务存在安全隐患：\n");
            for (String offending : result.getOffendingItems()) {
                msg.append("- ").append(offending).append("\n");
            }
            if (!result.getSuggestions().isEmpty()) {
                msg.append("建议替代方案：\n");
                for (String suggestion : result.getSuggestions()) {
                    msg.append("- ").append(suggestion).append("\n");
                }
            }
            result.setMessage(msg.toString());
        }

        return result;
    }

    /**
     * 从用户健康记录和档案中提取关键健康状况。
     */
    private String getUserConditions(Long userId) {
        StringBuilder conditions = new StringBuilder();

        // 1. 从 health_record 读取疾病史
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<HealthRecord>()
                .eq(HealthRecord::getUserId, userId)
                .eq(HealthRecord::getIsLatest, 1);
        HealthRecord record = healthRecordMapper.selectOne(wrapper);

        if (record != null) {
            String disease = record.getDiseaseHistory();
            if (disease != null && !disease.isBlank()) {
                extractConditions(disease, conditions);
            }
        }

        // 2. 从 user_profile 读取慢性病和损伤（Onboarding 阶段的输入）
        UserProfile profile = userProfileMapper.selectById(userId);
        if (profile != null) {
            if (profile.getChronicDiseases() != null && !profile.getChronicDiseases().isBlank()
                    && !"无".equals(profile.getChronicDiseases())) {
                extractConditions(profile.getChronicDiseases(), conditions);
            }
            if (profile.getInjuries() != null && !profile.getInjuries().isBlank()
                    && !"无".equals(profile.getInjuries())) {
                extractConditions(profile.getInjuries(), conditions);
            }
        }

        return conditions.toString();
    }

    /**
     * 从文本中提取匹配 COMMON_CONDITIONS 的关键词。
     */
    private void extractConditions(String text, StringBuilder target) {
        for (String condition : COMMON_CONDITIONS) {
            if (text.contains(condition)) {
                if (target.length() > 0 && !target.toString().contains(condition)) {
                    target.append(",");
                }
                if (!target.toString().contains(condition)) {
                    target.append(condition);
                }
            }
        }
    }

    /**
     * 从 AI 返回的计划文本中提取任务条目。
     */
    public List<String> extractPlanItems(String aiContent) {
        if (aiContent == null || aiContent.isBlank()) {
            return List.of();
        }
        // 按行分割，过滤空行和标题
        return Arrays.stream(aiContent.split("\n"))
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .filter(line -> !line.startsWith("#") && !line.startsWith("```"))
                .collect(Collectors.toList());
    }

    /**
     * 计划内容中是否包含被禁止的运动关键词。
     */
    public boolean containsForbiddenKeyword(String planTask, SafetyRule rule) {
        if (rule.getForbiddenKeywords() == null) return false;
        return Arrays.stream(rule.getForbiddenKeywords().split(","))
                .map(String::trim)
                .anyMatch(k -> planTask.toLowerCase().contains(k.toLowerCase()));
    }
}