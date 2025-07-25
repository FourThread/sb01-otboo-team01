package com.ozang.web.config.batch;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 배치 프로파일 전용 설정
 */
@Configuration
@Profile("batch")
public class BatchProfileConfig {

    @Bean
    @ConditionalOnProperty(name = "batch.enabled", havingValue = "true", matchIfMissing = true)
    public BatchEnableMarker batchEnableMarker() {
        return new BatchEnableMarker();
    }

    /**
     * 배치 활성화 마커 클래스
     */
    public static class BatchEnableMarker{
    }
}
