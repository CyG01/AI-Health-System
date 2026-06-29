package com.example;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.ActiveProfiles;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class BaseTest {

    @MockBean
    protected StringRedisTemplate stringRedisTemplate;

    protected void mockRedisSetIfAbsent(boolean result) {
        ValueOperations<String, String> ops = mockValueOps();
        when(stringRedisTemplate.opsForValue()).thenReturn(ops);
        when(ops.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(result);
    }

    @SuppressWarnings("unchecked")
    protected ValueOperations<String, String> mockValueOps() {
        return org.mockito.Mockito.mock(ValueOperations.class);
    }
}