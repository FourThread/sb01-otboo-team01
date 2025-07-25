package com.fourthread.ozang;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * O-ZANG 배치 애플리케이션
 *
 * 역할:
 * - 스케줄링 기반 배치 작업 실행
 * - 날씨 데이터 정리/캐시 워밍업
 * - 시스템 유지보수 작업
 * - 배치 작업 모니터링 API
 *
 * 포트: 8081 (관리 API용)
 * 프로필: batch
 */
@SpringBootApplication(
    scanBasePackages = "com.fourthread.ozang",
    exclude = {
        // 웹 보안 설정 제외 (배치에서는 불필요)
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class,
        // Elasticsearch 자동 설정 제외 (배치에서는 불필요)
        org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchDataAutoConfiguration.class,
        org.springframework.boot.autoconfigure.data.elasticsearch.ElasticsearchRepositoriesAutoConfiguration.class,
        org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchClientAutoConfiguration.class
    }
)
@EnableJpaRepositories(basePackages = "com.fourthread.ozang")
@EntityScan(basePackages = "com.fourthread.ozang")
@EnableBatchProcessing
@EnableScheduling
public class OzangBatchApplication {

    public static void main(String[] args) {
        // 배치 프로필 자동 활성화
        System.setProperty("spring.profiles.active",
            System.getProperty("spring.profiles.active", "batch,local"));

        SpringApplication.run(OzangBatchApplication.class, args);
    }
}