package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.common.BusinessException;
import com.example.dto.SleepRecordSubmitDTO;
import com.example.entity.SleepRecord;
import com.example.mapper.SleepRecordMapper;
import com.example.monitor.DeepSeekCostMonitor;
import com.example.properties.DeepSeekProperties;
import com.example.service.SleepService;
import com.example.vo.SleepRecordVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SleepServiceImpl implements SleepService {

    private final SleepRecordMapper sleepRecordMapper;
    private final DeepSeekCostMonitor costMonitor;
    private final DeepSeekProperties deepSeekProperties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    public SleepServiceImpl(SleepRecordMapper sleepRecordMapper,
                             DeepSeekCostMonitor costMonitor,
                             DeepSeekProperties deepSeekProperties,
                             ObjectMapper objectMapper) {
        this.sleepRecordMapper = sleepRecordMapper;
        this.costMonitor = costMonitor;
        this.deepSeekProperties = deepSeekProperties;
        this.objectMapper = objectMapper;
        this.webClient = WebClient.builder()
                .baseUrl(deepSeekProperties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + deepSeekProperties.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @Override
    @Transactional
    public SleepRecordVO submit(Long userId, SleepRecordSubmitDTO dto) {
        // 计算时长
        long diffMin = ChronoUnit.MINUTES.between(dto.getSleepTime(), dto.getWakeTime());
        if (diffMin < 0) {
            diffMin += 24 * 60; // 跨天
        }
        if (diffMin < 30 || diffMin > 1440) {
            throw new BusinessException("睡眠时长不合理，请检查入睡和起床时间");
        }

        // 检查当日是否已有记录
        SleepRecord existing = getByDateEntity(userId, dto.getRecordDate());
        if (existing != null) {
            existing.setSleepTime(dto.getSleepTime());
            existing.setWakeTime(dto.getWakeTime());
            existing.setDurationMin((int) diffMin);
            existing.setQuality(dto.getQuality());
            existing.setDreamNotes(dto.getDreamNotes());
            sleepRecordMapper.updateById(existing);
            return toVO(existing);
        }

        SleepRecord record = new SleepRecord();
        record.setUserId(userId);
        record.setRecordDate(dto.getRecordDate());
        record.setSleepTime(dto.getSleepTime());
        record.setWakeTime(dto.getWakeTime());
        record.setDurationMin((int) diffMin);
        record.setQuality(dto.getQuality());
        record.setDreamNotes(dto.getDreamNotes());
        sleepRecordMapper.insert(record);

        log.info("睡眠记录提交 userId={} date={} duration={}min quality={}", userId, dto.getRecordDate(), diffMin, dto.getQuality());
        return toVO(record);
    }

    @Override
    public SleepRecordVO getByDate(Long userId, LocalDate date) {
        SleepRecord record = getByDateEntity(userId, date);
        return record != null ? toVO(record) : null;
    }

    @Override
    public List<SleepRecordVO> getList(Long userId, int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);
        LambdaQueryWrapper<SleepRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SleepRecord::getUserId, userId)
                .between(SleepRecord::getRecordDate, start, end)
                .orderByDesc(SleepRecord::getRecordDate);
        return sleepRecordMapper.selectList(wrapper).stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public String analyzeSleep(Long userId) {
        if (costMonitor.isGlobalCostExceeded()) {
            return "今日AI额度已用尽，请明天再试";
        }

        List<SleepRecordVO> records = getList(userId, 14);
        if (records.isEmpty()) {
            return "暂无足够的睡眠数据进行分析，请先记录至少3天的睡眠数据。";
        }
        if (records.size() < 3) {
            return "睡眠数据不足（需要至少3天），继续记录后可获得AI分析。";
        }

        double avgDuration = records.stream().mapToInt(SleepRecordVO::getDurationMin).average().orElse(0);
        double avgQuality = records.stream().mapToInt(SleepRecordVO::getQuality).average().orElse(0);
        long lateCount = records.stream().filter(r -> r.getSleepTime().getHour() >= 24 || r.getSleepTime().getHour() <= 1).count();

        String stats = String.format(
                "近14天平均睡眠时长：%.1f分钟（%.1f小时），平均质量：%.1f/5分，超过午夜入睡天数：%d天。",
                avgDuration, avgDuration / 60.0, avgQuality, lateCount);

        String prompt = "你是专业睡眠健康顾问。请根据以下数据给出改善建议。" +
                stats + "请给出3-5条具体的、可执行的睡眠改善建议，用中文回答。";

        try {
            Map<String, Object> requestBody = Map.of(
                    "model", deepSeekProperties.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", "你是专业睡眠健康顾问。"),
                            Map.of("role", "user", "content", prompt)
                    )
            );

            String response = webClient.post()
                    .uri("/chat/completions")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            com.fasterxml.jackson.databind.JsonNode root = objectMapper.readTree(response);
            int inputTokens = root.path("usage").path("prompt_tokens").asInt();
            int outputTokens = root.path("usage").path("completion_tokens").asInt();
            costMonitor.recordCall(inputTokens, outputTokens);

            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            log.error("AI睡眠分析失败", e);
            return "AI分析暂时不可用。建议：1.保持规律的作息时间 2.睡前1小时避免使用电子设备 3.每天运动30分钟有助于改善睡眠质量";
        }
    }

    private SleepRecord getByDateEntity(Long userId, LocalDate date) {
        LambdaQueryWrapper<SleepRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SleepRecord::getUserId, userId)
                .eq(SleepRecord::getRecordDate, date);
        return sleepRecordMapper.selectOne(wrapper);
    }

    private SleepRecordVO toVO(SleepRecord record) {
        SleepRecordVO vo = new SleepRecordVO();
        vo.setId(record.getId());
        vo.setRecordDate(record.getRecordDate());
        vo.setSleepTime(record.getSleepTime());
        vo.setWakeTime(record.getWakeTime());
        vo.setDurationMin(record.getDurationMin());
        vo.setQuality(record.getQuality());
        vo.setDreamNotes(record.getDreamNotes());
        vo.setCreateTime(record.getCreateTime());
        return vo;
    }
}