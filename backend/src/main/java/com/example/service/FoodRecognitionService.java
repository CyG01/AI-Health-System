package com.example.service;

import com.example.vo.FoodRecognizeVO;
import com.example.vo.FoodTextRecognizeVO;
import org.springframework.web.multipart.MultipartFile;

public interface FoodRecognitionService {

    /**
     * 识别食物图片
     */
    FoodRecognizeVO recognize(MultipartFile image, Long userId);

    /**
     * 自然语言食物识别（一句话记账）。
     * 用户输入自然语言描述，AI 解析为结构化食物列表。
     *
     * @param userId 用户ID
     * @param text   自然语言描述，如"中午吃了一碗兰州拉面加煎蛋"
     * @return 解析后的食物列表（含名称、重量、热量）
     */
    FoodTextRecognizeVO recognizeByText(Long userId, String text);
}