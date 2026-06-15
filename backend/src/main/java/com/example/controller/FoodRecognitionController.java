package com.example.controller;

import com.example.annotation.NoRepeatSubmit;
import com.example.annotation.RateLimit;
import com.example.common.Result;
import com.example.dto.FoodTextRecognizeDTO;
import com.example.service.FoodRecognitionService;
import com.example.vo.FoodRecognizeVO;
import com.example.vo.FoodTextRecognizeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Tag(name = "AI食物识别")
@RestController
@RequestMapping("/api/food")
public class FoodRecognitionController {

    /** 常见图片格式的魔数签名（前若干字节） */
    private static final Map<String, byte[]> IMAGE_MAGIC_BYTES = Map.of(
            "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF},
            "image/png",  new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47},
            "image/gif",  new byte[]{0x47, 0x49, 0x46, 0x38},
            "image/bmp",  new byte[]{0x42, 0x4D},
            "image/webp", new byte[]{0x52, 0x49, 0x46, 0x46}
    );

    @Autowired
    private FoodRecognitionService foodRecognitionService;

    @RateLimit(time = 60, count = 10)
    @NoRepeatSubmit
    @Operation(summary = "AI识别食物图片")
    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FoodRecognizeVO> recognize(@RequestParam("image") MultipartFile image,
                                              @RequestAttribute(value = "userId", required = false) Long userId) {
        if (image.isEmpty()) {
            return Result.error(400, "图片不能为空");
        }
        // 限制10MB
        if (image.getSize() > 10 * 1024 * 1024) {
            return Result.error(400, "图片大小不能超过10MB");
        }
        String contentType = image.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return Result.error(400, "请上传图片文件");
        }
        // 魔数校验：防止将非图片文件伪装为图片上传
        if (!verifyMagicBytes(image, contentType)) {
            return Result.error(400, "文件格式与声明类型不匹配，请上传真实图片");
        }
        return Result.success(foodRecognitionService.recognize(image, userId));
    }

    @RateLimit(time = 60, count = 10)
    @NoRepeatSubmit
    @Operation(summary = "自然语言食物识别（一句话记账）")
    @PostMapping("/recognize-text")
    public Result<FoodTextRecognizeVO> recognizeText(
            @Validated @RequestBody FoodTextRecognizeDTO dto,
            @RequestAttribute(value = "userId", required = false) Long userId) {
        return Result.success(foodRecognitionService.recognizeByText(userId, dto.getText()));
    }

    /**
     * 校验文件开头的魔数字节是否与声明的 Content-Type 匹配。
     * 防止攻击者将可执行文件/脚本的后缀改为 .jpg 绕过 Content-Type 检查。
     */
    private boolean verifyMagicBytes(MultipartFile file, String declaredContentType) {
        byte[] expected = IMAGE_MAGIC_BYTES.get(declaredContentType);
        if (expected == null) {
            // 未注册的 image/* 类型：拒绝（如 image/svg+xml 可能有 XXE 风险）
            return false;
        }
        try {
            byte[] fileBytes = file.getBytes();
            if (fileBytes.length < expected.length) {
                return false;
            }
            for (int i = 0; i < expected.length; i++) {
                if (fileBytes[i] != expected[i]) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}