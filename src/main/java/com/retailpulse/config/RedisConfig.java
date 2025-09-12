package com.retailpulse.config;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.retailpulse.dto.response.BusinessEntityResponseDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class RedisConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        // Base config: key serializer + TTL, do not cache nulls
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()));

        // Plain ObjectMapper (no default typing)
        ObjectMapper om = new ObjectMapper().findAndRegisterModules();

        // Serializer for single BusinessEntityResponseDto
        Jackson2JsonRedisSerializer<BusinessEntityResponseDto> entitySer = new Jackson2JsonRedisSerializer<>(om, BusinessEntityResponseDto.class);

        // Serializer for List<BusinessEntityResponseDto> (handles [] correctly)
        JavaType listType = om.getTypeFactory().constructCollectionType(List.class, BusinessEntityResponseDto.class);
        Jackson2JsonRedisSerializer<Object> entityListSer = new Jackson2JsonRedisSerializer<>(om, listType);

        // Per-cache configurations with the correct value serializer
        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put("businessEntity", base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(entitySer))
        );
        cacheConfigs.put("businessEntityList", base.serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(entityListSer))
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base) // default if any other cache is added later
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }
}