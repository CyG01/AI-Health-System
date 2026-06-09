package com.example.controller;

import com.example.common.Result;
import com.example.service.FoodRecognitionService;
import com.example.vo.FoodRecognizeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "AI食物识别")
@RestController
@RequestMapping("/api/food")
public class FoodRecognitionController {

    @Autowired
    private FoodRecognitionService foodRecognitionService;

    @Operation(summary = "AI识别食物图片")
    @PostMapping(value = "/recognize", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<FoodRecognizeVO> recognize(@RequestParam("image") MultipartFile image) {
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
        return Result.success(foodRecognitionService.recognize(image));
    }
}