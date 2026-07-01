package com.example.service;

import com.example.entity.MultimodalUpload;

import java.util.Map;

/**
 * 多模态处理服务接口
 * 支持图片、语音等多模态输入的智能解析与自动入库
 */
public interface MultimodalService {

    /**
     * 提交多模态文件进行处理
     *
     * @param userId     用户ID
     * @param uploadType 上传类型：IMAGE/AUDIO
     * @param fileUrl    文件URL
     * @param fileSize   文件大小
     * @param mimeType   MIME类型
     * @return 上传记录
     */
    MultimodalUpload submitMultimodal(Long userId, String uploadType,
                                       String fileUrl, Long fileSize, String mimeType);

    /**
     * 处理图片（食物识别、运动识别等）
     *
     * @param uploadId 上传记录ID
     * @param imageUrl 图片URL
     * @return 解析结果
     */
    Map<String, Object> processImage(Long uploadId, String imageUrl);

    /**
     * 处理语音（语音转文字、意图识别）
     *
     * @param uploadId   上传记录ID
     * @param audioUrl   音频URL
     * @param duration   时长（秒）
     * @return 解析结果
     */
    Map<String, Object> processAudio(Long uploadId, String audioUrl, Integer duration);

    /**
     * 根据解析结果自动创建对应记录（饮食、运动等）
     *
     * @param userId   用户ID
     * @param uploadId 上传记录ID
     * @param result   解析结果
     * @return 创建的记录信息
     */
    Map<String, Object> autoCreateRecord(Long userId, Long uploadId, Map<String, Object> result);

    /**
     * 获取多模态处理结果
     *
     * @param uploadId 上传记录ID
     * @param userId   用户ID（用于权限校验）
     * @return 处理结果
     */
    MultimodalUpload getUploadResult(Long uploadId, Long userId);

    /**
     * 用户修正解析结果
     *
     * @param uploadId    上传记录ID
     * @param userId      用户ID
     * @param correctedData 修正后的数据
     * @return 是否成功
     */
    boolean correctResult(Long uploadId, Long userId, Map<String, Object> correctedData);

    /**
     * 获取用户上传历史
     *
     * @param userId 用户ID
     * @param limit  数量
     * @return 上传记录列表
     */
    java.util.List<MultimodalUpload> getUserUploadHistory(Long userId, int limit);
}
