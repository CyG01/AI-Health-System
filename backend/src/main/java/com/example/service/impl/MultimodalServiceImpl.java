package com.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.common.BusinessException;
import com.example.entity.MultimodalUpload;
import com.example.mapper.MultimodalUploadMapper;
import com.example.service.MultimodalService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 多模态处理服务实现类
 * 支持图片、语音等多模态输入的智能解析与自动入库
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MultimodalServiceImpl implements MultimodalService {

    private final MultimodalUploadMapper uploadMapper;
    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public MultimodalUpload submitMultimodal(Long userId, String uploadType,
                                              String fileUrl, Long fileSize, String mimeType) {
        // 验证上传类型
        if (!"IMAGE".equalsIgnoreCase(uploadType) && !"AUDIO".equalsIgnoreCase(uploadType)) {
            throw new BusinessException(400, "不支持的上传类型");
        }

        // 创建上传记录
        MultimodalUpload upload = new MultimodalUpload();
        upload.setUserId(userId);
        upload.setUploadType(uploadType.toUpperCase());
        upload.setFileUrl(fileUrl);
        upload.setFileSize(fileSize);
        upload.setMimeType(mimeType);
        upload.setProcessingStatus("PENDING");
        upload.setUserCorrected(0);
        upload.setCreatedAt(LocalDateTime.now());
        uploadMapper.insert(upload);

        // 发送到 MQ 异步处理
        try {
            Map<String, Object> task = new HashMap<>();
            task.put("uploadId", upload.getId());
            task.put("userId", userId);
            task.put("uploadType", uploadType);
            task.put("fileUrl", fileUrl);
            task.put("mimeType", mimeType);

            rocketMQTemplate.convertAndSend("multimodal-process", objectMapper.writeValueAsString(task));
            log.info("多模态任务已提交 MQ uploadId={} type={}", upload.getId(), uploadType);
        } catch (Exception e) {
            log.error("发送多模态任务 MQ 失败 uploadId={}", upload.getId(), e);
            // 标记为失败
            upload.setProcessingStatus("FAILED");
            upload.setErrorMessage("任务提交失败");
            uploadMapper.updateById(upload);
        }

        return upload;
    }

    @Override
    public Map<String, Object> processImage(Long uploadId, String imageUrl) {
        // 更新状态为处理中
        updateProcessingStatus(uploadId, "PROCESSING", null);

        try {
            // TODO: 实际项目中调用 Vision 模型（如 Qwen-VL、DeepSeek-VL）
            // 这里是模拟实现，实际应通过 ModelRouter 路由到支持 Vision 的模型

            // 模拟食物识别结果
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("recordType", "DIET");
            result.put("mealType", detectMealType());
            result.put("foods", Arrays.asList(
                    Map.of("name", "米饭", "amount", 150, "unit", "g", "calories", 174),
                    Map.of("name", "红烧肉", "amount", 100, "unit", "g", "calories", 350),
                    Map.of("name", "青菜", "amount", 80, "unit", "g", "calories", 20)
            ));
            result.put("totalCalories", 544);
            result.put("totalProtein", 25.5);
            result.put("totalCarbs", 60.2);
            result.put("totalFat", 22.8);
            result.put("confidence", 0.85);
            result.put("modelUsed", "qwen-vl-max");

            // 保存解析结果
            String resultJson = objectMapper.writeValueAsString(result);
            updateProcessingResult(uploadId, "SUCCESS", resultJson, "qwen-vl-max", new BigDecimal("0.85"));

            log.info("图片处理完成 uploadId={}", uploadId);
            return result;

        } catch (Exception e) {
            log.error("图片处理失败 uploadId={}", uploadId, e);
            updateProcessingStatus(uploadId, "FAILED", e.getMessage());
            throw new BusinessException(500, "图片处理失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> processAudio(Long uploadId, String audioUrl, Integer duration) {
        // 更新状态为处理中
        updateProcessingStatus(uploadId, "PROCESSING", null);

        try {
            // TODO: 实际项目中调用 ASR + LLM 进行语音转文字和意图识别
            // 这里是模拟实现

            // 模拟语音识别结果
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("transcript", "我今天中午吃了一碗米饭和一份红烧肉");
            result.put("intent", "DIET_RECORD");
            result.put("recordType", "DIET");
            result.put("mealType", "lunch");
            result.put("foods", Arrays.asList(
                    Map.of("name", "米饭", "amount", 200, "unit", "g", "calories", 232),
                    Map.of("name", "红烧肉", "amount", 150, "unit", "g", "calories", 525)
            ));
            result.put("totalCalories", 757);
            result.put("confidence", 0.90);
            result.put("modelUsed", "qwen-audio-turbo");

            // 保存解析结果
            String resultJson = objectMapper.writeValueAsString(result);
            updateProcessingResult(uploadId, "SUCCESS", resultJson, "qwen-audio-turbo", new BigDecimal("0.90"));

            log.info("语音处理完成 uploadId={}", uploadId);
            return result;

        } catch (Exception e) {
            log.error("语音处理失败 uploadId={}", uploadId, e);
            updateProcessingStatus(uploadId, "FAILED", e.getMessage());
            throw new BusinessException(500, "语音处理失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> autoCreateRecord(Long userId, Long uploadId, Map<String, Object> result) {
        Map<String, Object> response = new LinkedHashMap<>();

        try {
            String recordType = (String) result.get("recordType");

            // 根据类型自动创建对应记录
            switch (recordType) {
                case "DIET":
                    // TODO: 调用 FoodService 创建饮食记录
                    response.put("recordType", "DIET");
                    response.put("status", "CREATED");
                    response.put("message", "饮食记录已自动创建");
                    break;

                case "EXERCISE":
                    // TODO: 调用 ExerciseService 创建运动记录
                    response.put("recordType", "EXERCISE");
                    response.put("status", "CREATED");
                    response.put("message", "运动记录已自动创建");
                    break;

                case "SYMPTOM":
                    // TODO: 创建症状记录
                    response.put("recordType", "SYMPTOM");
                    response.put("status", "CREATED");
                    response.put("message", "症状记录已自动创建");
                    break;

                default:
                    response.put("status", "SKIPPED");
                    response.put("message", "未识别的记录类型，跳过自动创建");
            }

            // 更新上传记录，关联创建的记录
            if ("CREATED".equals(response.get("status"))) {
                uploadMapper.update(null,
                        new LambdaUpdateWrapper<MultimodalUpload>()
                                .eq(MultimodalUpload::getId, uploadId)
                                .set(MultimodalUpload::getRecordType, recordType)
                                // .set(MultimodalUpload::getRecordId, recordId) // 实际项目中设置真实ID
                );
            }

            response.put("uploadId", uploadId);
            return response;

        } catch (Exception e) {
            log.error("自动创建记录失败 uploadId={}", uploadId, e);
            response.put("status", "FAILED");
            response.put("error", e.getMessage());
            return response;
        }
    }

    @Override
    public MultimodalUpload getUploadResult(Long uploadId, Long userId) {
        MultimodalUpload upload = uploadMapper.selectById(uploadId);
        if (upload == null) {
            throw new BusinessException(404, "上传记录不存在");
        }
        if (!upload.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权查看该记录");
        }
        return upload;
    }

    @Override
    public boolean correctResult(Long uploadId, Long userId, Map<String, Object> correctedData) {
        MultimodalUpload upload = uploadMapper.selectById(uploadId);
        if (upload == null) {
            throw new BusinessException(404, "上传记录不存在");
        }
        if (!upload.getUserId().equals(userId)) {
            throw new BusinessException(403, "无权修改该记录");
        }

        try {
            // 保存修正后的数据
            String correctedJson = objectMapper.writeValueAsString(correctedData);

            uploadMapper.update(null,
                    new LambdaUpdateWrapper<MultimodalUpload>()
                            .eq(MultimodalUpload::getId, uploadId)
                            .set(MultimodalUpload::getProcessingResult, correctedJson)
                            .set(MultimodalUpload::getUserCorrected, 1)
                            .set(MultimodalUpload::getProcessedAt, LocalDateTime.now())
            );

            log.info("用户修正解析结果 uploadId={}", uploadId);
            return true;

        } catch (Exception e) {
            log.error("修正结果失败 uploadId={}", uploadId, e);
            return false;
        }
    }

    @Override
    public List<MultimodalUpload> getUserUploadHistory(Long userId, int limit) {
        return uploadMapper.selectRecentByUserId(userId, Math.min(limit, 100));
    }

    // ==================== 私有方法 ====================

    /**
     * 更新处理状态
     */
    private void updateProcessingStatus(Long uploadId, String status, String errorMessage) {
        uploadMapper.update(null,
                new LambdaUpdateWrapper<MultimodalUpload>()
                        .eq(MultimodalUpload::getId, uploadId)
                        .set(MultimodalUpload::getProcessingStatus, status)
                        .set(errorMessage != null, MultimodalUpload::getErrorMessage, errorMessage)
                        .set("SUCCESS".equals(status) || "FAILED".equals(status),
                                MultimodalUpload::getProcessedAt, LocalDateTime.now())
        );
    }

    /**
     * 更新处理结果
     */
    private void updateProcessingResult(Long uploadId, String status, String result,
                                         String modelUsed, BigDecimal confidence) {
        uploadMapper.update(null,
                new LambdaUpdateWrapper<MultimodalUpload>()
                        .eq(MultimodalUpload::getId, uploadId)
                        .set(MultimodalUpload::getProcessingStatus, status)
                        .set(MultimodalUpload::getProcessingResult, result)
                        .set(MultimodalUpload::getModelUsed, modelUsed)
                        .set(MultimodalUpload::getConfidence, confidence)
                        .set(MultimodalUpload::getProcessedAt, LocalDateTime.now())
        );
    }

    /**
     * 根据当前时间判断餐次
     */
    private String detectMealType() {
        int hour = LocalDateTime.now().getHour();
        if (hour >= 5 && hour < 10) {
            return "breakfast";
        } else if (hour >= 10 && hour < 14) {
            return "lunch";
        } else if (hour >= 14 && hour < 17) {
            return "snack";
        } else if (hour >= 17 && hour < 21) {
            return "dinner";
        } else {
            return "supper";
        }
    }

    /**
     * 解析处理结果为 Map
     */
    public Map<String, Object> parseProcessingResult(String resultJson) {
        if (resultJson == null || resultJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(resultJson, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("解析处理结果失败", e);
            return Collections.emptyMap();
        }
    }
}
