package com.ozang.batch.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 날씨 데이터 전용 Redis 캐시 설정
 * 3단계 캐시 전략 구현을 위한 설정
 */
@Configuration
@EnableCaching
public class WeatherRedisConfig {

    @Value("${spring.data.redis.host}")
    private String host;

    @Value("${spring.data.redis.port}")
    private int port;

    @Value("${weather.redis.database:1}")
    private int weatherDatabase;

    @Value("${spring.data.redis.ssl.enabled:false}")
    private boolean sslEnabled;

    @Value("${spring.data.redis.timeout:2000}")
    private long timeout;

    @Value("${weather.cache.current.ttl:PT1H}")  // 현재 날씨 1시간
    private Duration currentWeatherTtl;

    @Value("${weather.cache.forecast.ttl:PT3H}")  // 5일 예보 3시간
    private Duration forecastWeatherTtl;

    @Value("${weather.cache.location.ttl:PT24H}")  // 위치 정보 24시간
    private Duration locationTtl;


    /**
     * 날씨 전용 RedisConnectionFactory 생성
     */
    @Bean(name = "weatherRedisConnectionFactory")
    public RedisConnectionFactory weatherRedisConnectionFactory() {
        RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
        redisStandaloneConfiguration.setHostName(host);
        redisStandaloneConfiguration.setPort(port);
        redisStandaloneConfiguration.setDatabase(weatherDatabase);

        LettuceClientConfiguration.LettuceClientConfigurationBuilder configBuilder =
            LettuceClientConfiguration.builder();

        if (sslEnabled) {
            SslOptions sslOptions = SslOptions.builder()
                .jdkSslProvider() // JDK SSL Provider 사용
                .build();

            ClientOptions clientOptions = ClientOptions.builder()
                .sslOptions(sslOptions)
                .build();

            configBuilder
                .useSsl() // SSL 활성화
                .and()
                .clientOptions(clientOptions);
        } else {
            ClientOptions clientOptions = ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .autoReconnect(true)
                .build();

            configBuilder.clientOptions(clientOptions);
        }

        configBuilder.commandTimeout(java.time.Duration.ofMillis(timeout));

        LettuceClientConfiguration clientConfig = configBuilder.build();
        return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
    }

    /**
     * 날씨 전용 RedisTemplate 설정
     */
    @Bean(name = "weatherRedisTemplate")
    public RedisTemplate<String, Object> weatherRedisTemplate() {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(weatherRedisConnectionFactory()); // 전용 ConnectionFactory 사용

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTyping(
            objectMapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL,
            JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(
            objectMapper);

        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}