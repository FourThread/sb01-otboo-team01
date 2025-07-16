package com.fourthread.ozang.module.domain.weather.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * 날씨 데이터 전용 Redis 캐시 설정
 * 3단계 캐시 전략 구현을 위한 설정
 */
@Configuration
@EnableCaching
public class WeatherRedisConfig {

    @Value("${weather.cache.current.ttl:PT1H}")  // 현재 날씨 1시간
    private Duration currentWeatherTtl;

    @Value("${weather.cache.forecast.ttl:PT3H}")  // 5일 예보 3시간
    private Duration forecastWeatherTtl;

    @Value("${weather.cache.location.ttl:PT24H}")  // 위치 정보 24시간
    private Duration locationTtl;

    /**
     * 날씨 전용 RedisTemplate 설정
     */
    @Bean(name = "weatherRedisTemplate")
    public RedisTemplate<String, Object> weatherRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // JSON 직렬화를 위한 ObjectMapper 설정
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // 직렬화 설정
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }

    /**
     * 날씨 캐시 매니저 설정
     */
    @Bean
    public CacheManager weatherCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                new GenericJackson2JsonRedisSerializer(createObjectMapper())))
            .entryTtl(Duration.ofHours(1)); // 기본 TTL

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            // 캐시별 TTL 설정
            .withCacheConfiguration("currentWeather", defaultConfig.entryTtl(currentWeatherTtl))
            .withCacheConfiguration("forecastWeather", defaultConfig.entryTtl(forecastWeatherTtl))
            .withCacheConfiguration("weatherLocation", defaultConfig.entryTtl(locationTtl))
            .build();
    }

    private ObjectMapper createObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );
        return objectMapper;
    }
}