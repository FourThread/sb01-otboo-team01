package com.fourthread.ozang.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@SpringBootApplication
@EnableBatchProcessing
@EnableScheduling
@ComponentScan(basePackages = {
    "com.fourthread.ozang.core",
    "com.fourthread.ozang.batch"
})
public class OzangBatchApplication {

    public static void main(String[] args) {
        System.setProperty("spring.profiles.default", "batch");

        log.info("🚀 Starting O-ZANG Batch Application");
        log.info("📋 Application arguments: {}", String.join(" ", args));
        log.info("🔧 Active profiles: {}", System.getProperty("spring.profiles.active", "batch"));

        SpringApplication app = new SpringApplication(OzangBatchApplication.class);

        // 배치 애플리케이션 특성 설정
        app.setAllowBeanDefinitionOverriding(true);
        app.setAllowCircularReferences(true);

        // 배치 작업 자동 실행 비활성화 (스케줄러나 API를 통해 실행)
        System.setProperty("spring.batch.job.enabled", "false");

        try {
            app.run(args);
            log.info("✅ O-ZANG Batch Application started successfully");
        } catch (Exception e) {
            log.error("❌ Failed to start O-ZANG Batch Application: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}