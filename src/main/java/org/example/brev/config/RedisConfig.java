package org.example.brev.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration class for setting up Redis connection and templates
 */
@Configuration
public class RedisConfig {

    /**
     * -- GETTER --
     *  Get the TTL configuration for URL mappings
     */
    @Value("${app.cache.url-mapping.ttl:3600}")
    private long urlMappingTtl;

    /**
     * Configure Redis template for String operations
     *
     * @param connectionFactory Redis connection factory
     * @return Configured RedisTemplate
     */
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String serializer for both keys and values
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);

        template.afterPropertiesSet();
        return template;
    }

}
