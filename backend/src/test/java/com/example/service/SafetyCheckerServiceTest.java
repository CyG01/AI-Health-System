package com.example.service;

import com.example.BaseTest;
import com.example.entity.HealthRecord;
import com.example.entity.SafetyReviewLog;
import com.example.entity.SafetyRule;
import com.example.entity.UserProfile;
import com.example.mapper.ComplianceRuleMapper;
import com.example.mapper.HealthRecordMapper;
import com.example.mapper.SafetyReviewLogMapper;
import com.example.mapper.SafetyRuleMapper;
import com.example.mapper.UserProfileMapper;
import com.example.vo.SafetyCheckResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * SafetyCheckerService 单元测试。
 *
 * 覆盖 Layer1 医疗红线拦截、Layer2 规则表匹配、合规校验等核心逻辑。
 */
class SafetyCheckerServiceTest extends BaseTest {

    @Autowired
    private SafetyCheckerService safetyCheckerService;

    @MockBean
    private SafetyRuleMapper safetyRuleMapper;

    @MockBean
    private ComplianceRuleMapper complianceRuleMapper;

    @MockBean
    private HealthRecordMapper healthRecordMapper;

    @MockBean
    private UserProfileMapper userProfileMapper;

    @MockBean
    private SafetyReviewLogMapper safetyReviewLogMapper;

    @MockBean
    private MedicalSafetyDict medicalSafetyDict;

    private static final Long USER_ID = 1L;

    @BeforeEach
    void setUp() {
        // 默认：健康记录无疾病史，无用户档案
        when(healthRecordMapper.selectOne(any())).thenReturn(null);
        when(userProfileMapper.selectById(USER_ID)).thenReturn(null);
        when(safetyRuleMapper.matchByConditions(anyString())).thenReturn(Collections.emptyList());
        when(complianceRuleMapper.matchByText(anyString())).thenReturn(Collections.emptyList());
        when(safetyReviewLogMapper.insert(any())).thenReturn(1);
    }

    // ==================== checkPlan 测试 ====================

    @Nested
    @DisplayName("checkPlan — 安全检查")
    class CheckPlanTests {

        @Test
        @DisplayName("空计划列表 → 直接通过")
        void shouldPassForEmptyPlan() {
            SafetyCheckResult result = safetyCheckerService.checkPlan(USER_ID, List.of());
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("null 计划 → 直接通过")
        void shouldPassForNullPlan() {
            SafetyCheckResult result = safetyCheckerService.checkPlan(USER_ID, null);
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("无疾病史用户 → 不触发规则表匹配，但仍检查合规")
        void shouldSkipRuleMatchWhenNoConditions() {
            List<String> planItems = List.of("每日快走30分钟", "瑜伽拉伸15分钟");
            SafetyCheckResult result = safetyCheckerService.checkPlan(USER_ID, planItems);
            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("有高血压病史 + 包含禁忌运动 → 拦截")
        void shouldBlockForbiddenExerciseForHypertension() {
            // 模拟用户有高血压
            HealthRecord record = new HealthRecord();
            record.setUserId(USER_ID);
            record.setDiseaseHistory("高血压");
            record.setIsLatest(1);
            when(healthRecordMapper.selectOne(any())).thenReturn(record);

            // 模拟规则表：高血压禁止剧烈运动
            SafetyRule rule = new SafetyRule();
            rule.setUserCondition("高血压");
            rule.setForbiddenKeywords("跑步,冲刺,HIIT");
            rule.setAlternativeSuggestion("建议改为快走或太极");
            when(safetyRuleMapper.matchByConditions(anyString())).thenReturn(List.of(rule));

            List<String> planItems = List.of("每日跑步30分钟", "冲刺训练10分钟");
            SafetyCheckResult result = safetyCheckerService.checkPlan(USER_ID, planItems);

            assertFalse(result.isPassed());
            assertEquals(1, result.getViolations().size());
            assertFalse(result.getOffendingItems().isEmpty());
        }

        @Test
        @DisplayName("有膝盖损伤 + 不包含禁忌运动 → 通过")
        void shouldPassWhenNoForbiddenKeywordMatch() {
            UserProfile profile = new UserProfile();
            profile.setId(USER_ID);
            profile.setInjuries("膝盖损伤");
            when(userProfileMapper.selectById(USER_ID)).thenReturn(profile);

            SafetyRule rule = new SafetyRule();
            rule.setUserCondition("膝盖损伤");
            rule.setForbiddenKeywords("深蹲,跳跃,篮球");
            when(safetyRuleMapper.matchByConditions(anyString())).thenReturn(List.of(rule));

            List<String> planItems = List.of("游泳30分钟", "上肢力量训练");
            SafetyCheckResult result = safetyCheckerService.checkPlan(USER_ID, planItems);

            assertTrue(result.isPassed());
        }

        @Test
        @DisplayName("包含合规违规词 → 拦截")
        void shouldBlockComplianceViolation() {
            com.example.entity.ComplianceRule compRule = new com.example.entity.ComplianceRule();
            compRule.setAction("block");
            when(complianceRuleMapper.matchByText(anyString())).thenReturn(List.of(compRule));

            List<String> planItems = List.of("使用处方药治疗");
            SafetyCheckResult result = safetyCheckerService.checkPlan(USER_ID, planItems);

            assertFalse(result.isPassed());
            assertFalse(result.getComplianceIssues().isEmpty());
        }
    }

    // ==================== extractPlanItems 测试 ====================

    @Nested
    @DisplayName("extractPlanItems — 提取计划条目")
    class ExtractPlanItemsTests {

        @Test
        @DisplayName("标准AI计划文本 → 正确提取条目")
        void shouldExtractPlanItems() {
            String aiContent = """
                    第1天
                    - 快走30分钟，中等强度
                    - 晚餐控制碳水摄入
                    第2天
                    - 游泳20分钟
                    - 早餐增加蛋白质""";

            List<String> items = safetyCheckerService.extractPlanItems(aiContent);
            assertFalse(items.isEmpty());
            assertTrue(items.stream().anyMatch(s -> s.contains("快走")));
            assertTrue(items.stream().anyMatch(s -> s.contains("游泳")));
        }

        @Test
        @DisplayName("空文本 → 返回空列表")
        void shouldReturnEmptyForBlank() {
            assertTrue(safetyCheckerService.extractPlanItems("").isEmpty());
            assertTrue(safetyCheckerService.extractPlanItems(null).isEmpty());
        }

        @Test
        @DisplayName("包含标题行 → 过滤标题")
        void shouldFilterHeaders() {
            String aiContent = """
                    # 运动计划
                    快走30分钟
                    ## 饮食建议
                    控制碳水摄入""";

            List<String> items = safetyCheckerService.extractPlanItems(aiContent);
            assertTrue(items.stream().noneMatch(s -> s.startsWith("#")));
            assertTrue(items.stream().anyMatch(s -> s.contains("快走")));
        }
    }

    // ==================== containsForbiddenKeyword 测试 ====================

    @Nested
    @DisplayName("containsForbiddenKeyword — 关键词匹配")
    class ContainsForbiddenKeywordTests {

        @Test
        @DisplayName("包含禁止关键词 → 返回 true")
        void shouldDetectForbiddenKeyword() {
            SafetyRule rule = new SafetyRule();
            rule.setForbiddenKeywords("跑步,冲刺,HIIT");

            assertTrue(safetyCheckerService.containsForbiddenKeyword("每日跑步30分钟", rule));
            assertTrue(safetyCheckerService.containsForbiddenKeyword("HIIT训练", rule));
        }

        @Test
        @DisplayName("不包含禁止关键词 → 返回 false")
        void shouldNotDetectSafeKeyword() {
            SafetyRule rule = new SafetyRule();
            rule.setForbiddenKeywords("跑步,冲刺,HIIT");

            assertFalse(safetyCheckerService.containsForbiddenKeyword("游泳30分钟", rule));
        }
    }

    // ==================== layer1MedicalCheck 测试 ====================

    @Nested
    @DisplayName("layer1MedicalCheck — 医疗红线 Layer1 拦截")
    class Layer1MedicalCheckTests {

        @Test
        @DisplayName("无违规文本 → 返回 true")
        void shouldPassForCleanText() {
            MedicalSafetyDict.MedicalSafetyResult mockResult = MedicalSafetyDict.MedicalSafetyResult.pass();
            when(medicalSafetyDict.check(anyString())).thenReturn(mockResult);

            assertTrue(safetyCheckerService.layer1MedicalCheck(USER_ID, "今天天气不错，适合运动"));
        }

        @Test
        @DisplayName("包含处方药名 → 返回 false 并记录日志")
        void shouldBlockPrescriptionDrug() {
            List<MedicalSafetyDict.MedicalMatch> matches = List.of(
                    new MedicalSafetyDict.MedicalMatch("PRESCRIPTION_DRUG", "氨氯地平", 5)
            );
            MedicalSafetyDict.MedicalSafetyResult mockResult = MedicalSafetyDict.MedicalSafetyResult.block(matches);
            when(medicalSafetyDict.check(anyString())).thenReturn(mockResult);

            assertFalse(safetyCheckerService.layer1MedicalCheck(USER_ID, "建议服用氨氯地平降压"));
        }

        @Test
        @DisplayName("空文本 → 返回 true")
        void shouldPassForNullText() {
            assertTrue(safetyCheckerService.layer1MedicalCheck(USER_ID, null));
            assertTrue(safetyCheckerService.layer1MedicalCheck(USER_ID, ""));
        }
    }
}