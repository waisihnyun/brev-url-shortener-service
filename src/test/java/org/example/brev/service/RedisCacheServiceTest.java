package org.example.brev.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisCacheServiceTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RedisCacheService redisCacheService;

    private static final long TEST_TTL = 3600L;

    @BeforeEach
    void setUp() {
        redisCacheService = new RedisCacheService(redisTemplate, TEST_TTL);
    }

    @Test
    void cacheUrlMapping_ShouldStoreValueWithTTL() {
        // Given
        String shortCode = "abc123";
        String longUrl = "https://example.com";
        String expectedKey = "url:" + shortCode;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        redisCacheService.cacheUrlMapping(shortCode, longUrl);

        // Then
        verify(valueOperations).set(expectedKey, longUrl, TEST_TTL, TimeUnit.SECONDS);
    }

    @Test
    void getCachedUrlMapping_ShouldReturnValueWhenExists() {
        // Given
        String shortCode = "abc123";
        String longUrl = "https://example.com";
        String expectedKey = "url:" + shortCode;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(longUrl);

        // When
        String result = redisCacheService.getCachedUrlMapping(shortCode);

        // Then
        assertEquals(longUrl, result);
        verify(valueOperations).get(expectedKey);
    }

    @Test
    void getCachedUrlMapping_ShouldReturnNullWhenNotExists() {
        // Given
        String shortCode = "abc123";
        String expectedKey = "url:" + shortCode;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenReturn(null);

        // When
        String result = redisCacheService.getCachedUrlMapping(shortCode);

        // Then
        assertNull(result);
        verify(valueOperations).get(expectedKey);
    }

    @Test
    void getCachedUrlMapping_ShouldReturnNullOnException() {
        // Given
        String shortCode = "abc123";
        String expectedKey = "url:" + shortCode;
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedKey)).thenThrow(new RuntimeException("Redis connection failed"));

        // When
        String result = redisCacheService.getCachedUrlMapping(shortCode);

        // Then
        assertNull(result);
    }

    @Test
    void evictUrlMapping_ShouldDeleteKey() {
        // Given
        String shortCode = "abc123";
        String expectedKey = "url:" + shortCode;
        when(redisTemplate.delete(expectedKey)).thenReturn(true);

        // When
        redisCacheService.evictUrlMapping(shortCode);

        // Then
        verify(redisTemplate).delete(expectedKey);
    }

    @Test
    void evictUrlMapping_ShouldHandleExceptionGracefully() {
        // Given
        String shortCode = "abc123";
        String expectedKey = "url:" + shortCode;
        when(redisTemplate.delete(expectedKey)).thenThrow(new RuntimeException("Redis connection failed"));

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> redisCacheService.evictUrlMapping(shortCode));
    }

    @Test
    void cacheUrlMapping_ShouldHandleExceptionGracefully() {
        // Given
        String shortCode = "abc123";
        String longUrl = "https://example.com";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new RuntimeException("Redis connection failed"))
                .when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

        // When & Then - should not throw exception
        assertDoesNotThrow(() -> redisCacheService.cacheUrlMapping(shortCode, longUrl));
    }
}
