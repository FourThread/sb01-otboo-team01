package com.fourthread.ozang.app.domain.recommend.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class RecommendationRedisConfig {

  @Value("${spring.data.redis.host}")
  private String host;

  @Value("${spring.data.redis.port}")
  private int port;

  @Value("${spring.data.redis.ssl.enabled:false}")
  private boolean sslEnabled;

  @Value("${spring.data.redis.timeout:2000}")
  private long timeout;

  @Bean
  public RedisCacheManager recommendationCacheManager() {
    return RedisCacheManager.builder(
            RedisCacheWriter.nonLockingRedisCacheWriter(recommendationRedisConnectionFactory())) // 직접 지정
        .cacheDefaults(
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(100))
                .serializeValuesWith(
                    RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer())
                )
        )
        .build();
  }

  @Bean
  public RedisConnectionFactory recommendationRedisConnectionFactory() {
    RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
    redisStandaloneConfiguration.setHostName(host);
    redisStandaloneConfiguration.setPort(port);
    redisStandaloneConfiguration.setDatabase(2);

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

    // =============== 공통 타임아웃 설정 추가 ===============
    configBuilder.commandTimeout(java.time.Duration.ofMillis(timeout));

    LettuceClientConfiguration clientConfig = configBuilder.build();

    return new LettuceConnectionFactory(redisStandaloneConfiguration, clientConfig);
  }

  @Bean
  public RedisTemplate<String, Object> recommendationRedisTemplate() {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(recommendationRedisConnectionFactory());

    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(new StringRedisSerializer());

    redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashValueSerializer(new StringRedisSerializer());

    redisTemplate.afterPropertiesSet();

    return redisTemplate;
  }

}
