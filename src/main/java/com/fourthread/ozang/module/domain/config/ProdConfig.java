package com.fourthread.ozang.module.domain.config;

import com.fourthread.ozang.module.domain.feed.elasticsearch.service.FeedSearchService;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Production 환경용 설정 클래스
 */
@Configuration
@Profile("prod")
public class ProdConfig {

    /**
     * Elasticsearch가 비활성화된 경우 FeedSearchService Optional을 빈 Optional로 제공
     */
    @Bean
    @ConditionalOnMissingBean(FeedSearchService.class)
    @ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "false", matchIfMissing = true)
    public Optional<FeedSearchService> feedSearchServiceOptional() {
        return Optional.empty();
    }

    /**
     * Elasticsearch가 활성화된 경우 FeedSearchService Optional을 실제 서비스로 제공
     */
    @Bean
    @ConditionalOnProperty(name = "elasticsearch.enabled", havingValue = "true")
    public Optional<FeedSearchService> feedSearchServiceOptionalEnabled(FeedSearchService feedSearchService) {
        return Optional.of(feedSearchService);
    }
}