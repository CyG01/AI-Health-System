package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.common.BusinessException;
import com.example.entity.*;
import com.example.mapper.*;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.service.HealthReportService;
import com.example.service.HealthService;
import com.example.vo.HealthReportVO;
import com.example.vo.HealthRecordVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import com.example.util.PromptSanitizer;

@Slf4j
@Service
public class HealthReportServiceImpl implements HealthReportService {

    private final HealthReportMapper healthReportMapper;
    private final HealthService healthService;
    private final DailyCheckinMapper checkinMapper;
    private final ExerciseRecordMapper exerciseRecordMapper;
    private final DietRecordMapper dietRecordMapper;
    private final SysUserMapper sysUserMapper;
    private final DeepSeekCostMonitor costMonitor;
    private final ObjectMapper objectMapper;
    private final com.example.properties.DeepSeekProperties deepSeekProperties;
    private final WebClient webClient;

    public HealthReportServiceImpl(HealthReportMapper healthReportMapper,
                                    HealthService healthService,
                                    DailyCheckinMapper checkinMapper,
                                    ExerciseRecordMapper exerciseRecordMapper,
                                    DietRecordMapper dietRecordMapper,
                                    SysUserMapper sysUserMapper,
                                    DeepSeekCostMonitor costMonitor,
                                    ObjectMapper objectMapper,
                                    com.example.properties.DeepSeekProperties deepSeekProperties) {
        this.healthReportMapper = healthReportMapper;
        this.healthService = healthService;
        this.checkinMapper = checkinMapper;
        this.exerciseRecordMapper = exerciseRecordMapper;
        this.dietRecordMapper = dietRecordMapper;
        this.sysUserMapper = sysUserMapper;
        this.costMonitor = costMonitor;
        this.objectMapper = objectMapper;
        this.deepSeekProperties = deepSeekProperties;
        this.webClient = WebClient.builder()
                .baseUrl(deepSeekProperties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + deepSeekProperties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    @Transactional
    public HealthReportVO generateReport(Long userId, String reportType) {
        if (costMonitor.isGlobalCostExceeded()) {
            throw new BusinessException("今日AI调用额度已用尽，请明天再试");
        }

        // Issue 6: 健康档案显式校验，避免异常信息被吞没
        HealthRecordVO health;
        try {
            health = healthService.getLatestHealthRecord(userId);
        } catch (BusinessException e) {
            throw new BusinessException("请先创建健康档案后再生成报告");
        }

        // Issue 3: 同周期重复报告生成防护
        String period = "weekly".equals(reportType)
                ? LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy'W'w"))
                : LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
        Long existingCount = healthReportMapper.selectCount(
                new LambdaQueryWrapper<HealthReport>()
                        .eq(HealthReport::getUserId, userId)
                        .eq(HealthReport::getReportType, reportType)
                        .eq(HealthReport::getReportPeriod, period));
        if (existingCount > 0) {
            throw new BusinessException("该周期报告已存在，无需重复生成");
        }

        // Issue 5: Prompt 注入防护
        String sanitizedGoal = PromptSanitizer.sanitize(
                health.getGoal() != null ? health.getGoal() : "未设定");

        String stats = gatherStats(userId, reportType);
        String periodLabel = getPeriodLabel(reportType);

        String prompt = String.format(
                "你是专业健康分析师。请根据以下数据生成一份%s健康报告。\n" +
                        "用户信息：身高%.1fcm，体重%.1fkg，BMI%.1f，健康目标：%s\n" +
                        "数据统计：\n%s\n\n" +
                        "请严格按JSON格式输出：{\"summary\":\"总体概述(100字)\",\"achievements\":[\"亮点1\",\"亮点2\"]," +
                        "\"concerns\":[\"关注点1\",\"关注点2\"],\"suggestions\":[\"建议1\",\"建议2\",\"建议3\"]," +
                        "\"score\":数字(1-100综合评分)}。只输出JSON。",
                periodLabel,
                health.getHeight(), health.getWeight(), health.getBmi(),
                sanitizedGoal,
                stats);

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", deepSeekProperties.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", "你是专业健康分析师。"),
                            Map.of("role", "user", "content", prompt)
                    ),
                    "response_format", Map.of("type", "json_object")
            );

            // Issue 2: 添加超时配置
            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(deepSeekProperties.getTimeout()))
                    .block();

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
            int inputTokens = root.path("usage").path("prompt_tokens").asInt();
            int outputTokens = root.path("usage").path("completion_tokens").asInt();
            costMonitor.recordCall(inputTokens, outputTokens);

            String content = root.path("choices").get(0).path("message").path("content").asText();
            if (content == null || content.isBlank()) {
                throw new BusinessException("AI返回内容为空");
            }

            // Issue 1: 校验 AI 返回的 JSON 内容格式与字段完整性
            validateAiReportContent(content);

            HealthReport report = new HealthReport();
            report.setUserId(userId);
            report.setReportType(reportType);
            report.setReportPeriod(period);
            report.setAiContent(content);
            report.setIsRead(0);
            healthReportMapper.insert(report);

            return toVO(report);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("生成健康报告失败", e);
            throw new BusinessException("生成报告失败，请稍后重试");
        }
    }

    /**
     * 校验 AI 返回的健康报告 JSON 格式是否包含必要字段
     */
    private void validateAiReportContent(String content) {
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(content);
            if (!node.has("summary") || !node.has("score")) {
                throw new BusinessException("AI报告格式不完整，缺少必要字段");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("AI报告JSON解析失败 content={}", content.length() > 500 ? content.substring(0, 500) : content);
            throw new BusinessException("AI报告格式异常，请稍后重试");
        }
    }

    @Override
    public List<HealthReportVO> getReportList(Long userId, int page, int size) {
        LambdaQueryWrapper<HealthReport> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HealthReport::getUserId, userId)
                .orderByDesc(HealthReport::getCreateTime);
        Page<HealthReport> pageResult = healthReportMapper.selectPage(
                new Page<>(page, size), wrapper);
        return pageResult.getRecords().stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public HealthReportVO getReportDetail(Long reportId, Long userId) {
        HealthReport report = healthReportMapper.selectById(reportId);
        if (report == null || !report.getUserId().equals(userId)) {
            throw new BusinessException(404, "报告不存在");
        }
        return toVO(report);
    }

    @Override
    public void markAsRead(Long reportId, Long userId) {
        HealthReport report = healthReportMapper.selectById(reportId);
        if (report == null || !report.getUserId().equals(userId)) {
            throw new BusinessException(404, "报告不存在");
        }
        report.setIsRead(1);
        healthReportMapper.updateById(report);
    }

    @Scheduled(cron = "0 0 8 * * MON")
    @Override
    @Transactional
    public void autoGenerateWeeklyReports() {
        log.info("自动生成周报任务开始");
        List<SysUser> activeUsers = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1));
        for (SysUser user : activeUsers) {
            if (costMonitor.isGlobalCostExceeded()) {
                log.warn("AI额度用尽，周报生成任务中止");
                break;
            }
            try {
                generateReport(user.getId(), "weekly");
                log.info("周报生成成功 userId={}", user.getId());
            } catch (BusinessException e) {
                // Issue 7: 无健康档案/已有报告的用户跳过，不产生warn噪音
                log.debug("周报跳过 userId={} reason={}", user.getId(), e.getMessage());
            } catch (Exception e) {
                log.warn("周报生成失败 userId={} err={}", user.getId(), e.getMessage());
            }
        }
    }

    @Scheduled(cron = "0 0 8 1 * ?")
    @Override
    @Transactional
    public void autoGenerateMonthlyReports() {
        log.info("自动生成月报任务开始");
        List<SysUser> activeUsers = sysUserMapper.selectList(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getStatus, 1));
        for (SysUser user : activeUsers) {
            if (costMonitor.isGlobalCostExceeded()) {
                log.warn("AI额度用尽，月报生成任务中止");
                break;
            }
            try {
                generateReport(user.getId(), "monthly");
                log.info("月报生成成功 userId={}", user.getId());
            } catch (BusinessException e) {
                // Issue 7: 无健康档案/已有报告的用户跳过，不产生warn噪音
                log.debug("月报跳过 userId={} reason={}", user.getId(), e.getMessage());
            } catch (Exception e) {
                log.warn("月报生成失败 userId={} err={}", user.getId(), e.getMessage());
            }
        }
    }

    private String gatherStats(Long userId, String reportType) {
        int days = "weekly".equals(reportType) ? 7 : 30;
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);

        // 打卡统计（Issue 4: 按日期排序确保体重变化计算正确）
        LambdaQueryWrapper<DailyCheckin> checkinW = new LambdaQueryWrapper<>();
        checkinW.eq(DailyCheckin::getUserId, userId)
                .between(DailyCheckin::getCheckDate, start, end)
                .orderByAsc(DailyCheckin::getCheckDate);
        List<DailyCheckin> checkins = checkinMapper.selectList(checkinW);

        int totalDays = days;
        int checkedDays = checkins.size();
        int exerciseDays = (int) checkins.stream().filter(c -> c.getExerciseStatus() != null && c.getExerciseStatus() > 0).count();
        int dietDays = (int) checkins.stream().filter(c -> c.getDietStatus() != null && c.getDietStatus() > 0).count();

        // 体重变化
        double weightChange = 0;
        if (!checkins.isEmpty()) {
            DailyCheckin first = checkins.get(0);
            DailyCheckin last = checkins.get(checkins.size() - 1);
            if (first.getCurrentWeight() != null && last.getCurrentWeight() != null) {
                weightChange = last.getCurrentWeight() - first.getCurrentWeight();
            }
        }

        // 运动消耗
        LambdaQueryWrapper<ExerciseRecord> exW = new LambdaQueryWrapper<>();
        exW.eq(ExerciseRecord::getUserId, userId)
                .between(ExerciseRecord::getCreateTime, start.atStartOfDay(), end.atTime(23, 59, 59));
        List<ExerciseRecord> exercises = exerciseRecordMapper.selectList(exW);
        int totalExerciseCalories = exercises.stream().mapToInt(e -> e.getCaloriesBurned() != null ? e.getCaloriesBurned() : 0).sum();

        // 饮食摄入
        LambdaQueryWrapper<DietRecord> dietW = new LambdaQueryWrapper<>();
        dietW.eq(DietRecord::getUserId, userId)
                .between(DietRecord::getCreateTime, start.atStartOfDay(), end.atTime(23, 59, 59));
        List<DietRecord> diets = dietRecordMapper.selectList(dietW);
        int totalDietCalories = diets.stream().mapToInt(d -> d.getCaloriesConsumed() != null ? d.getCaloriesConsumed() : 0).sum();

        return String.format(
                "统计周期：最近%d天\n" +
                        "打卡天数：%d/%d (%.0f%%)\n" +
                        "运动完成天数：%d\n" +
                        "饮食记录天数：%d\n" +
                        "体重变化：%.1fkg\n" +
                        "总运动消耗：%dkcal\n" +
                        "总饮食摄入：%dkcal",
                days, checkedDays, totalDays, (double) checkedDays / totalDays * 100,
                exerciseDays, dietDays,
                weightChange, totalExerciseCalories, totalDietCalories);
    }

    private String getPeriodLabel(String reportType) {
        return "weekly".equals(reportType) ? "本周" : "本月";
    }

    private HealthReportVO toVO(HealthReport report) {
        HealthReportVO vo = new HealthReportVO();
        vo.setId(report.getId());
        vo.setReportType(report.getReportType());
        vo.setReportPeriod(report.getReportPeriod());
        vo.setAiContent(report.getAiContent());
        vo.setCreateTime(report.getCreateTime());
        vo.setIsRead(report.getIsRead());
        return vo;
    }
}