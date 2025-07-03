package com.fourthread.ozang.module.config.batch;

import com.fourthread.ozang.module.domain.security.jwt.batch.JwtBatchService;
import com.fourthread.ozang.module.domain.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배치 작업 서비스
 * 도메일 별 배치 처리
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BatchService {

    private final WeatherService weatherService;
    private final JwtBatchService jwtBatchService;

    /**
     * 날씨 데이터 정리 (기본 보관 기간 사용)
     * @return 삭제된 데이터 개수
     */
    public int cleanupOldWeatherData() {
        log.info("날씨 데이터 정리 작업 위임");
        return weatherService.cleanupOldWeatherData();
    }

    /**
     * 날씨 데이터 정리 (사용자 정의 보관 기간)
     * @param retentionDays 보관 기간 (일)
     * @return 삭제된 데이터 개수
     */
    public int cleanupOldWeatherData(int retentionDays) {
        log.info("날씨 데이터 정리 작업 위임 - 보관 기간: {}일", retentionDays);
        return weatherService.cleanupOldWeatherData(retentionDays);
    }

    /**
     * JWT 토큰 정리
     * @return 삭제된 토큰 개수
     */
    public int cleanupExpiredTokens() {
        log.info("JWT 토큰 정리 작업 위임");
        return jwtBatchService.cleanupExpiredTokens();
    }

}