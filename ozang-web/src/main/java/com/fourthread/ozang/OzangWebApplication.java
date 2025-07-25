package com.fourthread.ozang;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

/**
 * O-ZANG 웹 애플리케이션
 *
 * 역할:
 * - 사용자 대면 웹 서비스 제공
 * - REST API 서비스
 * - 실시간 웹소켓 통신
 * - 사용자 인증/인가
 *
 * 포트: 8080
 * 프로필: web
 */
@SpringBootApplication(scanBasePackages = "com.fourthread.ozang")
@EnableJpaRepositories(basePackages = "com.fourthread.ozang")
@EntityScan(basePackages = "com.fourthread.ozang")
@EnableWebSecurity
@EnableScheduling(enabled = false)  // 웹에서는 스케줄링 비활성화
public class OzangWebApplication {

    public static void main(String[] args) {
        // 웹 프로필 자동 활성화
        System.setProperty("spring.profiles.active",
            System.getProperty("spring.profiles.active", "web,local"));

        SpringApplication.run(OzangWebApplication.class, args);
    }
}