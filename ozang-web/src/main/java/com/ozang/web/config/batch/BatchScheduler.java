package com.ozang.web.config.batch;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 배치 작업 스케줄링 설정
 * 전역 스케줄링 활성화 및 중앙 관리
 * 실제 스케줄링 로직은 각 도메인의 scheduler 패키지에서 담당
 */
@Slf4j
@Configuration
@EnableScheduling
public class BatchScheduler {
    public BatchScheduler() {
        log.info("배치 스케줄링 시스템 초기화 완료");
    }
}
