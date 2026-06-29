package com.example.service.impl;

import com.example.entity.UserMemory;
import com.example.mapper.UserMemoryMapper;
import com.example.mapper.UserProfileMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MemoryServiceImplTest {

    @Mock private UserMemoryMapper memoryMapper;
    @Mock private UserProfileMapper userProfileMapper;

    private MemoryServiceImpl memoryService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        memoryService = new MemoryServiceImpl(memoryMapper, userProfileMapper, objectMapper);
        // Set @Value fields via reflection
        ReflectionTestUtils.setField(memoryService, "apiKey", "test-api-key");
        ReflectionTestUtils.setField(memoryService, "baseUrl", "http://localhost:8080/v1");
    }

    // ==================== @PostConstruct ====================

    @Test
    @DisplayName("webClient is properly initialized after @PostConstruct")
    void shouldInitializeWebClientAfterPostConstruct() {
        assertNull(getWebClient(), "webClient should be null before init");

        memoryService.init();

        assertNotNull(getWebClient(), "webClient should be initialized after @PostConstruct");
    }

    // ==================== store() ====================

    @Test
    @DisplayName("store() generates embedding and saves new memory")
    void shouldStoreMemoryWithEmbedding() {
        memoryService.init();
        mockEmbeddingResponse("[0.1, 0.2, 0.3]");

        // No duplicates found
        when(memoryMapper.findSimilar(eq(1L), anyString(), eq(1))).thenReturn(Collections.emptyList());
        when(memoryMapper.insert(any(UserMemory.class))).thenReturn(1);

        UserMemory result = memoryService.store(1L, "I like running", "PREFERENCE", 5, "USER_INPUT");

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("PREFERENCE", result.getMemoryType());
        assertEquals("I like running", result.getContent());
        assertEquals("[0.1,0.2,0.3]", result.getEmbedding());
        assertEquals(1, result.getAccessCount());
        verify(memoryMapper).insert(any(UserMemory.class));
    }

    @Test
    @DisplayName("store() returns existing memory when duplicate found")
    void shouldReturnExistingMemoryOnDuplicate() {
        memoryService.init();
        mockEmbeddingResponse("[0.1, 0.2, 0.3]");

        UserMemory existing = new UserMemory();
        existing.setId(42L);
        existing.setUserId(1L);
        existing.setContent("I like running");

        when(memoryMapper.findSimilar(eq(1L), anyString(), eq(1))).thenReturn(List.of(existing));

        UserMemory result = memoryService.store(1L, "I like running", "PREFERENCE", 5, "USER_INPUT");

        assertSame(existing, result);
        verify(memoryMapper).incrementAccessCount(42L);
        verify(memoryMapper, never()).insert(any(UserMemory.class));
    }

    @Test
    @DisplayName("store() returns null when embedding generation fails")
    void shouldReturnNullWhenEmbeddingFails() {
        memoryService.init();
        // Mock webClient to throw exception
        mockEmbeddingFailure();

        UserMemory result = memoryService.store(1L, "test content", "HEALTH", 5, "USER_INPUT");

        assertNull(result);
        verify(memoryMapper, never()).insert(any(UserMemory.class));
    }

    // ==================== retrieveRelevant() ====================

    @Test
    @DisplayName("retrieveRelevant() returns sorted results from DB")
    void shouldReturnSortedResultsFromDB() {
        memoryService.init();
        mockEmbeddingResponse("[0.1, 0.2, 0.3]");

        UserMemory mem1 = new UserMemory();
        mem1.setId(1L);
        mem1.setContent("Memory 1");

        UserMemory mem2 = new UserMemory();
        mem2.setId(2L);
        mem2.setContent("Memory 2");

        when(memoryMapper.findSimilar(eq(1L), anyString(), eq(5))).thenReturn(List.of(mem1, mem2));

        List<UserMemory> result = memoryService.retrieveRelevant(1L, "health query", 5);

        assertEquals(2, result.size());
        verify(memoryMapper).incrementAccessCount(1L);
        verify(memoryMapper).incrementAccessCount(2L);
    }

    @Test
    @DisplayName("retrieveRelevant() returns empty list when embedding fails")
    void shouldReturnEmptyListWhenEmbeddingFails() {
        memoryService.init();
        mockEmbeddingFailure();

        List<UserMemory> result = memoryService.retrieveRelevant(1L, "query", 5);

        assertTrue(result.isEmpty());
        verify(memoryMapper, never()).findSimilar(anyLong(), anyString(), anyInt());
    }

    @Test
    @DisplayName("retrieveRelevant() falls back to app-level cosine similarity on DB error")
    void shouldFallbackToAppLevelSimilarity() {
        memoryService.init();
        mockEmbeddingResponse("[0.1, 0.2, 0.3]");

        when(memoryMapper.findSimilar(eq(1L), anyString(), eq(5)))
                .thenThrow(new RuntimeException("VEC_COSINE_DISTANCE not supported"));

        UserMemory mem1 = new UserMemory();
        mem1.setId(1L);
        mem1.setEmbedding("[0.1, 0.2, 0.3]");

        when(memoryMapper.findAllWithEmbedding(1L)).thenReturn(List.of(mem1));

        List<UserMemory> result = memoryService.retrieveRelevant(1L, "query", 5);

        assertEquals(1, result.size());
        verify(memoryMapper).incrementAccessCount(1L);
    }

    // ==================== helpers ====================

    private Object getWebClient() {
        return ReflectionTestUtils.getField(memoryService, "webClient");
    }

    @SuppressWarnings("unchecked")
    private void mockEmbeddingResponse(String embeddingJson) {
        try {
            String responseJson = "{\"data\":[{\"embedding\":" + embeddingJson + "}]}";

            WebClient mockWebClient = mock(WebClient.class);
            WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
            WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
            WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
            WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

            when(mockWebClient.post()).thenReturn(requestBodyUriSpec);
            when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
            when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
            when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
            when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
            when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.just(responseJson));

            ReflectionTestUtils.setField(memoryService, "webClient", mockWebClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private void mockEmbeddingFailure() {
        WebClient mockWebClient = mock(WebClient.class);
        WebClient.RequestBodyUriSpec requestBodyUriSpec = mock(WebClient.RequestBodyUriSpec.class);
        WebClient.RequestBodySpec requestBodySpec = mock(WebClient.RequestBodySpec.class);
        WebClient.RequestHeadersSpec requestHeadersSpec = mock(WebClient.RequestHeadersSpec.class);
        WebClient.ResponseSpec responseSpec = mock(WebClient.ResponseSpec.class);

        when(mockWebClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any())).thenReturn(requestBodySpec);
        when(requestBodySpec.bodyValue(any())).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class)).thenReturn(Mono.error(new RuntimeException("API error")));

        ReflectionTestUtils.setField(memoryService, "webClient", mockWebClient);
    }
}
