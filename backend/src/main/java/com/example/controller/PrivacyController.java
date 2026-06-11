package com.example.controller;

import com.example.common.Result;
import com.example.mapper.UserProfileMapper;
import com.example.entity.UserProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户隐私授权控制器。
 *
 * 提供数据授权查询/修改接口：
 * - GET  /api/privacy/consent    查询当前授权状态
 * - PUT  /api/privacy/consent    更新授权状态
 */
@Slf4j
@Tag(name = "隐私授权管理")
@RestController
@RequestMapping("/api/privacy")
@RequiredArgsConstructor
public class PrivacyController {

    private final UserProfileMapper userProfileMapper;

    @Operation(summary = "查询用户数据授权状态")
    @GetMapping("/consent")
    public Result<Map<String, Object>> getConsent(@RequestAttribute("userId") Long userId) {
        UserProfile profile = userProfileMapper.selectById(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("dataConsentForModel", profile != null && profile.getDataConsentForModel() != null
                ? profile.getDataConsentForModel() : 0);
        result.put("dataConsentForRecommend", profile != null && profile.getDataConsentForRecommend() != null
                ? profile.getDataConsentForRecommend() : 0);
        return Result.success(result);
    }

    @Operation(summary = "更新用户数据授权状态")
    @PutMapping("/consent")
    public Result<Map<String, Object>> updateConsent(
            @RequestAttribute("userId") Long userId,
            @RequestBody ConsentUpdateRequest request) {
        UserProfile profile = userProfileMapper.selectById(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
        }

        if (request.getDataConsentForModel() != null) {
            profile.setDataConsentForModel(request.getDataConsentForModel());
        }
        if (request.getDataConsentForRecommend() != null) {
            profile.setDataConsentForRecommend(request.getDataConsentForRecommend());
        }

        if (profile.getId() == null) {
            userProfileMapper.insert(profile);
        } else {
            userProfileMapper.updateById(profile);
        }

        log.info("用户隐私授权更新 userId={} model={} recommend={}",
                userId, request.getDataConsentForModel(), request.getDataConsentForRecommend());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("dataConsentForModel", profile.getDataConsentForModel());
        result.put("dataConsentForRecommend", profile.getDataConsentForRecommend());
        return Result.success(result);
    }

    /**
     * 授权更新请求 DTO。
     */
    public static class ConsentUpdateRequest {
        /** 数据用于模型训练授权 0=未授权 1=已授权 */
        private Integer dataConsentForModel;
        /** 数据用于个性化推荐授权 0=未授权 1=已授权 */
        private Integer dataConsentForRecommend;

        public Integer getDataConsentForModel() { return dataConsentForModel; }
        public void setDataConsentForModel(Integer dataConsentForModel) { this.dataConsentForModel = dataConsentForModel; }
        public Integer getDataConsentForRecommend() { return dataConsentForRecommend; }
        public void setDataConsentForRecommend(Integer dataConsentForRecommend) { this.dataConsentForRecommend = dataConsentForRecommend; }
    }
}