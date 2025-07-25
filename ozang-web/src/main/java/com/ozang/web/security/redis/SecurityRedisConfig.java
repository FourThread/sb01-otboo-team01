package com.fourthread.ozang.module.domain.security.redis;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SslOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableRedisRepositories
public class SecurityRedisConfig {

  @Value("${spring.data.redis.host}")
  private String host;

  @Value("${spring.data.redis.port}")
  private int port;

  @Value("${spring.data.redis.database:0}")
  private int database;

  @Value("${spring.data.redis.ssl.enabled:false}")
  private boolean sslEnabled;

  @Value("${spring.data.redis.timeout:2000}")
  private long timeout;

  @Bean
  public RedisConnectionFactory redisConnectionFactory() {
    RedisStandaloneConfiguration redisStandaloneConfiguration = new RedisStandaloneConfiguration();
    redisStandaloneConfiguration.setHostName(host);
    redisStandaloneConfiguration.setPort(port);
    redisStandaloneConfiguration.setDatabase(database);

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

  @Bean
  public RedisTemplate<String, Object> redisTemplate() {
    RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
    redisTemplate.setConnectionFactory(redisConnectionFactory());

    redisTemplate.setKeySerializer(new StringRedisSerializer());
    redisTemplate.setValueSerializer(new StringRedisSerializer());

    redisTemplate.setHashKeySerializer(new StringRedisSerializer());
    redisTemplate.setHashValueSerializer(new StringRedisSerializer());

    redisTemplate.afterPropertiesSet();

    return redisTemplate;
  }
}
