package com.fourthread.ozang.module.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
@Profile("test")
public class TestS3Config {

    @Bean
    public S3Client s3Client() {
        // 테스트용 더미 S3Client 반환
        return S3Client.builder()
            .region(Region.of("ap-northeast-2"))
            .build();
    }
} 