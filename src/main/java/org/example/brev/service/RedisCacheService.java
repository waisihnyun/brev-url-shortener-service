package org.example.brev.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for handling Redis cache operations for URL mappings
 */
@Service
public class RedisCacheService {

    private static final Logger logger = LogManager.getLogger(RedisCacheService.class);
    private static final String URL_MAPPING_KEY_PREFIX = "url:";

    private final RedisTemplate<String, String> redisTemplate;
    private final long urlMappingTtl;

    public RedisCacheService(RedisTemplate<String, String> redisTemplate,
                           @Value("${app.cache.url-mapping.ttl:3600}") long urlMappingTtl) {
        this.redisTemplate = redisTemplate;
        this.urlMappingTtl = urlMappingTtl;
    }

    /**
     * Cache a URL mapping with TTL
     *
     * @param shortCode The short code key
     * @param longUrl The long URL value
     */
    public void cacheUrlMapping(String shortCode, String longUrl) {
        try {
            String key = buildUrlMappingKey(shortCode);
            redisTemplate.opsForValue().set(key, longUrl, urlMappingTtl, TimeUnit.SECONDS);
            logger.debug("Cached URL mapping - ShortCode: {}, LongUrl: {}, TTL: {}s",
                        shortCode, longUrl, urlMappingTtl);
        } catch (Exception e) {
            logger.error("Failed to cache URL mapping for short code: {}, error: {}",
                        shortCode, e.getMessage(), e);
        }
    }

    /**
     * Retrieve a URL mapping from cache
     *
     * @param shortCode The short code key
     * @return The cached long URL, or null if not found or on error
     */
    public String getCachedUrlMapping(String shortCode) {
        try {
            String key = buildUrlMappingKey(shortCode);
            String longUrl = redisTemplate.opsForValue().get(key);

            if (longUrl != null) {
                logger.debug("Cache hit for short code: {} -> {}", shortCode, longUrl);
            } else {
                logger.debug("Cache miss for short code: {}", shortCode);
            }

            return longUrl;
        } catch (Exception e) {
            logger.error("Failed to retrieve cached URL mapping for short code: {}, error: {}",
                        shortCode, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Remove a URL mapping from cache
     *
     * @param shortCode The short code key to remove
     */
    public void evictUrlMapping(String shortCode) {
        try {
            String key = buildUrlMappingKey(shortCode);
            Boolean deleted = redisTemplate.delete(key);
            if (deleted) {
                logger.debug("Evicted URL mapping from cache: {}", shortCode);
            } else {
                logger.debug("URL mapping not found in cache for eviction: {}", shortCode);
            }
        } catch (Exception e) {
            logger.error("Failed to evict URL mapping from cache for short code: {}, error: {}",
                        shortCode, e.getMessage(), e);
        }
    }

    /**
     * Check if Redis is available
     *
     * @return true if Redis is available, false otherwise
     */
    public boolean isRedisAvailable() {
        try {
            assert redisTemplate.getConnectionFactory() != null;
            redisTemplate.getConnectionFactory().getConnection().ping();
            return true;
        } catch (Exception e) {
            logger.warn("Redis is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Build the full Redis key for URL mapping
     *
     * @param shortCode The short code
     * @return The full Redis key
     */
    private String buildUrlMappingKey(String shortCode) {
        return URL_MAPPING_KEY_PREFIX + shortCode;
    }
}
