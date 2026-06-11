package com.example.service;

import com.example.vo.FoodRecognizeVO;
import org.springframework.web.multipart.MultipartFile;

public interface FoodRecognitionService {

    /**
     * 识别食物图片
     */
    FoodRecognizeVO recognize(MultipartFile image, Long userId);
}