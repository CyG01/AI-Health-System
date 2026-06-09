package com.example.service;

import com.example.BaseTest;
import com.example.dto.HealthCreateDTO;
import com.example.vo.HealthRecordVO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

class HealthServiceTest extends BaseTest {

    @Autowired
    private HealthService healthService;

    @Test
    void shouldCreateHealthRecord() {
        HealthCreateDTO dto = new HealthCreateDTO();
        dto.setHeight(170);
        dto.setWeight(65);
        dto.setTargetWeight(60);
        dto.setGender("男");

        HealthRecordVO record = healthService.createHealthRecord(999L, dto);
        assertNotNull(record);
        assertEquals(170, record.getHeight());
        assertEquals(65, record.getWeight());
        assertNotNull(record.getBmi());
    }

    @Test
    void shouldThrowWhenTargetWeightMissingForProgress() {
        assertThrows(Exception.class, () -> healthService.getHealthProgress(999L));
    }

    @Test
    void shouldReturnNullForNonExistentUser() {
        HealthRecordVO record = healthService.getLatestHealthRecord(99999L);
        assertNull(record);
    }
}