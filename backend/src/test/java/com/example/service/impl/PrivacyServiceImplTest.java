package com.example.service.impl;

import com.example.entity.UserProfile;
import com.example.mapper.UserProfileMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrivacyServiceImplTest {

    @Mock
    private UserProfileMapper userProfileMapper;

    private PrivacyServiceImpl privacyService;

    @BeforeEach
    void setUp() {
        privacyService = new PrivacyServiceImpl(userProfileMapper);
    }

    // ==================== getConsent ====================

    @Test
    @DisplayName("getConsent returns defaults (0) when profile doesn't exist")
    void shouldReturnDefaultsWhenProfileNotFound() {
        when(userProfileMapper.selectById(999L)).thenReturn(null);

        Map<String, Object> result = privacyService.getConsent(999L);

        assertEquals(999L, result.get("userId"));
        assertEquals(0, result.get("dataConsentForModel"));
        assertEquals(0, result.get("dataConsentForRecommend"));
    }

    @Test
    @DisplayName("getConsent returns actual values when profile exists")
    void shouldReturnActualValuesWhenProfileExists() {
        UserProfile profile = new UserProfile();
        profile.setId(1L);
        profile.setUserId(1L);
        profile.setDataConsentForModel(1);
        profile.setDataConsentForRecommend(0);

        when(userProfileMapper.selectById(1L)).thenReturn(profile);

        Map<String, Object> result = privacyService.getConsent(1L);

        assertEquals(1L, result.get("userId"));
        assertEquals(1, result.get("dataConsentForModel"));
        assertEquals(0, result.get("dataConsentForRecommend"));
    }

    @Test
    @DisplayName("getConsent treats null consent fields as 0")
    void shouldTreatNullConsentAsZero() {
        UserProfile profile = new UserProfile();
        profile.setId(1L);
        profile.setUserId(1L);
        profile.setDataConsentForModel(null);
        profile.setDataConsentForRecommend(null);

        when(userProfileMapper.selectById(1L)).thenReturn(profile);

        Map<String, Object> result = privacyService.getConsent(1L);

        assertEquals(0, result.get("dataConsentForModel"));
        assertEquals(0, result.get("dataConsentForRecommend"));
    }

    // ==================== updateConsent ====================

    @Test
    @DisplayName("updateConsent creates new profile if none exists")
    void shouldCreateNewProfileWhenNoneExists() {
        when(userProfileMapper.selectById(1L)).thenReturn(null);
        when(userProfileMapper.insert(any(UserProfile.class))).thenReturn(1);

        Map<String, Object> result = privacyService.updateConsent(1L, 1, 1);

        assertEquals(1, result.get("dataConsentForModel"));
        assertEquals(1, result.get("dataConsentForRecommend"));

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileMapper).insert(captor.capture());

        UserProfile saved = captor.getValue();
        assertEquals(1L, saved.getUserId());
        assertEquals(1, saved.getDataConsentForModel());
        assertEquals(1, saved.getDataConsentForRecommend());
        assertNull(saved.getId(), "New profile should have no id before insert");
    }

    @Test
    @DisplayName("updateConsent updates existing profile")
    void shouldUpdateExistingProfile() {
        UserProfile existing = new UserProfile();
        existing.setId(10L);
        existing.setUserId(1L);
        existing.setDataConsentForModel(0);
        existing.setDataConsentForRecommend(0);

        when(userProfileMapper.selectById(1L)).thenReturn(existing);
        when(userProfileMapper.updateById(any(UserProfile.class))).thenReturn(1);

        Map<String, Object> result = privacyService.updateConsent(1L, 1, null);

        assertEquals(1, result.get("dataConsentForModel"));
        assertEquals(0, result.get("dataConsentForRecommend")); // unchanged

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileMapper).updateById(captor.capture());

        UserProfile updated = captor.getValue();
        assertEquals(10L, updated.getId(), "Should keep existing id");
        assertEquals(1, updated.getDataConsentForModel());
        assertEquals(0, updated.getDataConsentForRecommend()); // not changed since null was passed
    }

    @Test
    @DisplayName("updateConsent only updates non-null fields")
    void shouldOnlyUpdateNonNullFields() {
        UserProfile existing = new UserProfile();
        existing.setId(10L);
        existing.setUserId(1L);
        existing.setDataConsentForModel(1);
        existing.setDataConsentForRecommend(0);

        when(userProfileMapper.selectById(1L)).thenReturn(existing);
        when(userProfileMapper.updateById(any(UserProfile.class))).thenReturn(1);

        // Only update dataConsentForRecommend, leave dataConsentForModel as null
        privacyService.updateConsent(1L, null, 1);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileMapper).updateById(captor.capture());

        UserProfile updated = captor.getValue();
        assertEquals(1, updated.getDataConsentForModel());    // unchanged from existing
        assertEquals(1, updated.getDataConsentForRecommend()); // updated
        verify(userProfileMapper, never()).insert(any(UserProfile.class));
    }
}
