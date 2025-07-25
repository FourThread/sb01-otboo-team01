package com.fourthread.ozang.module.domain.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

/**
 * Custom Health Configuration
 * Production 환경에서 외부 의존성 없는 안전한 Health Check 설정
 */
@Configuration
@Profile("prod")
public class CustomHealthConfiguration {

    /**
     * Elasticsearch Health Indicator 비활성화
     * Elasticsearch가 비활성화된 경우 건강한 상태로 응답하는 더미 health indicator
     */
    @Bean(name = "elasticsearchHealthIndicator")
    @Primary
    @ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "false", matchIfMissing = true)
    public HealthIndicator elasticsearchHealthIndicator() {
        return () -> Health.up()
            .withDetail("status", "disabled")
            .withDetail("reason", "Elasticsearch is intentionally disabled in production")
            .build();
    }

    /**
     * Redis Health Indicator 비활성화
     * Redis가 연결되지 않아도 건강한 상태로 응답하는 더미 health indicator
     */
    @Bean(name = "redisHealthIndicator")
    @Primary
    @ConditionalOnProperty(name = "management.health.redis.enabled", havingValue = "false", matchIfMissing = true)
    public HealthIndicator redisHealthIndicator() {
        return () -> Health.up()
            .withDetail("status", "disabled")
            .withDetail("reason", "Redis health check is disabled in production")
            .build();
    }

    /**
     * 기본 Application Health Indicator
     * 항상 건강한 상태를 보고하는 기본 health indicator
     */
    @Bean(name = "applicationHealthIndicator")
    public HealthIndicator applicationHealthIndicator() {
        return () -> Health.up()
            .withDetail("application", "ozang-app")
            .withDetail("status", "running")
            .withDetail("profile", "production")
            .withDetail("healthCheck", "basic-only")
            .build();
    }
}