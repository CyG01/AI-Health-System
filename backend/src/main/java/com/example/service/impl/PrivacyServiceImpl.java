package com.example.service.impl;

import com.example.entity.UserProfile;
import com.example.mapper.UserProfileMapper;
import com.example.service.PrivacyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 用户隐私授权服务实现。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PrivacyServiceImpl implements PrivacyService {

    private final UserProfileMapper userProfileMapper;

    @Override
    public Map<String, Object> getConsent(Long userId) {
        UserProfile profile = userProfileMapper.selectById(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("dataConsentForModel", profile != null && profile.getDataConsentForModel() != null
                ? profile.getDataConsentForModel() : 0);
        result.put("dataConsentForRecommend", profile != null && profile.getDataConsentForRecommend() != null
                ? profile.getDataConsentForRecommend() : 0);
        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateConsent(Long userId, Integer dataConsentForModel, Integer dataConsentForRecommend) {
        UserProfile profile = userProfileMapper.selectById(userId);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUserId(userId);
        }

        if (dataConsentForModel != null) {
            profile.setDataConsentForModel(dataConsentForModel);
        }
        if (dataConsentForRecommend != null) {
            profile.setDataConsentForRecommend(dataConsentForRecommend);
        }

        if (profile.getId() == null) {
            userProfileMapper.insert(profile);
        } else {
            userProfileMapper.updateById(profile);
        }

        log.info("用户隐私授权更新 userId={} model={} recommend={}",
                userId, dataConsentForModel, dataConsentForRecommend);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("dataConsentForModel", profile.getDataConsentForModel());
        result.put("dataConsentForRecommend", profile.getDataConsentForRecommend());
        return result;
    }
}
