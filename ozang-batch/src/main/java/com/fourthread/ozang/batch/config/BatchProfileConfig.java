package com.fourthread.ozang.batch.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 배치 프로파일 전용 설정
 */
@Configuration
public class BatchProfileConfig {

    @Bean
    public BatchEnableMarker batchEnableMarker() {
        return new BatchEnableMarker();
    }

    /**
     * 배치 활성화 마커 클래스
     */
    public static class BatchEnableMarker{
    }
}
