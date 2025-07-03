package com.fourthread.ozang.module.config.batch;

import com.fourthread.ozang.module.domain.security.jwt.batch.JwtBatchService;
import com.fourthread.ozang.module.domain.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 배치 작업 종합
 * 각 도메인별 배치 서비스를 조합하여 전체 시스템 정리를 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BatchService {

    private final WeatherService weatherService;
    private final JwtBatchService jwtBatchService;

    /**
     * 오래된 날씨 데이터 정리 (기본 보관 기간 사용)
     */
    public int cleanupOldWeatherData() {
        log.info("날씨 데이터 정리 배치 작업 시작");

        try {
            int deletedCount = weatherService.cleanupOldWeatherData();
            log.info("날씨 데이터 정리 완료 - 삭제된 데이터: {}건", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("날씨 데이터 정리 실패", e);
            throw new RuntimeException("날씨 데이터 정리 실패", e);
        }
    }

    /**
     * 오래된 날씨 데이터 정리 (사용자 정의 보관 기간)
     * @param retentionDays 보관 기간 (일)
     * @return 삭제된 데이터 개수
     */
    public int cleanupOldWeatherData(int retentionDays) {
        log.info("날씨 데이터 정리 배치 작업 시작 - 보관 기간: {}일", retentionDays);

        try {
            int deletedCount = weatherService.cleanupOldWeatherData(retentionDays);
            log.info("날씨 데이터 정리 완료 - 삭제된 데이터: {}건", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("날씨 데이터 정리 실패", e);
            throw new RuntimeException("날씨 데이터 정리 실패", e);
        }
    }

    /**
     * 만료된 JWT 토큰 정리
     * @return 삭제된 토큰 개수
     */
    public int cleanupExpiredTokens() {
        log.info("JWT 토큰 정리 배치 작업 시작");

        try {
            int deletedCount = jwtBatchService.cleanupExpiredTokens();
            log.info("JWT 토큰 정리 완료 - 삭제된 토큰: {}건", deletedCount);
            return deletedCount;
        } catch (Exception e) {
            log.error("JWT 토큰 정리 실패", e);
            throw new RuntimeException("JWT 토큰 정리 실패", e);
        }
    }

    /**
     * 전체 시스템 정리 (날씨 데이터, JWT 토큰)
     * @param weatherRetentionDays 날씨 데이터 보관 기간
     * @return 정리 결과 정보
     */
    public BatchCleanupResult cleanupSystem(int weatherRetentionDays) {
        log.info("전체 시스템 정리 배치 작업 시작 - 날씨 데이터 보관 기간: {}일", weatherRetentionDays);

        try {
            int deletedWeatherCount = cleanupOldWeatherData(weatherRetentionDays);
            int deletedTokenCount = cleanupExpiredTokens();

            BatchCleanupResult result = new BatchCleanupResult(deletedWeatherCount, deletedTokenCount);

            log.info("전체 시스템 정리 완료 - 날씨 데이터: {}건, JWT 토큰: {}건, 총합: {}건",
                deletedWeatherCount, deletedTokenCount, result.getTotalDeleted());

            return result;

        } catch (Exception e) {
            log.error("전체 시스템 정리 실패", e);
            throw new RuntimeException("전체 시스템 정리 실패", e);
        }
    }

    /**
     * 전체 시스템 정리 (기본 보관 기간 사용)
     * @return 정리 결과 정보
     */
    public BatchCleanupResult cleanupSystem() {
        return cleanupSystem(30); // 기본 30일 보관
    }

    /**
     * 배치 정리 결과 DTO
     */
    public record BatchCleanupResult(
        int deletedWeatherCount,
        int deletedTokenCount
    ) {
        /**
         * 총 삭제된 데이터 개수
         * @return 날씨 데이터 + JWT 토큰 삭제 개수의 합
         */
        public int getTotalDeleted() {
            return deletedWeatherCount + deletedTokenCount;
        }

        /**
         * 정리 작업 성공 여부
         * @return 하나 이상의 데이터가 정리되었으면 true
         */
        public boolean hasCleanedData() {
            return getTotalDeleted() > 0;
        }

        /**
         * 날씨 데이터 정리 성공 여부
         * @return 날씨 데이터가 정리되었으면 true
         */
        public boolean hasCleanedWeatherData() {
            return deletedWeatherCount > 0;
        }

        /**
         * JWT 토큰 정리 성공 여부
         * @return JWT 토큰이 정리되었으면 true
         */
        public boolean hasCleanedTokens() {
            return deletedTokenCount > 0;
        }
    }
}