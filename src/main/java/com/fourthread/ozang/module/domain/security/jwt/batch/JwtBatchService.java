package com.fourthread.ozang.module.domain.security.jwt.batch;

import com.fourthread.ozang.module.domain.security.jwt.JwtTokenRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * JWT 토큰 관련 배치 작업 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class JwtBatchService {

    private final JwtTokenRepository jwtTokenRepository;

    /**
     * 만료된 JWT 토큰 정리
     */
    public int cleanupExpiredTokens() {
        log.info("만료된 JWT 토큰 정리 시작");

        try {
            Instant now = Instant.now();
            log.debug("현재 시간 기준으로 만료된 토큰 조회: {}", now);

            // 먼저 삭제할 토큰 개수 확인
            long deleteCount = jwtTokenRepository.countExpiredTokens(now);

            if (deleteCount > 0) {
                log.info("삭제 예정 만료 토큰: {}건", deleteCount);

                // 만료된 토큰 삭제
                jwtTokenRepository.deleteExpiredTokens(now);
                log.info("만료된 JWT 토큰 {}건 삭제 완료", deleteCount);
            } else {
                log.info("삭제할 만료된 토큰이 없습니다");
            }

            return (int) deleteCount;

        } catch (Exception e) {
            log.error("만료된 토큰 정리 중 오류 발생, e");
            throw new RuntimeException("토큰 정리 실패, e");
        }
    }

    /**
     * 특정 시점 이전의 토큰 정리 (관리자용)
     * @param cutoffTime 기준 시점
     */
    public int cleanupTokensBefore(Instant cutoffTime) {
        log.info("특정 시점 이전 JWT 토큰 정리 시작 - 기준 시점: {}", cutoffTime);

        try {
            long deleteCount = jwtTokenRepository.countExpiredTokens(cutoffTime);

            if (deleteCount > 0) {
                jwtTokenRepository.deleteExpiredTokens(cutoffTime);
                log.info("{}건의 토큰 삭제 완료", deleteCount);
            } else {
                log.info("삭제할 토큰이 없습니다");
            }

            return (int) deleteCount;

        } catch (Exception e) {
            log.error("토큰 정리 중 오류 발생", e);
            throw new RuntimeException("토큰 정리 실패", e);
        }
    }

}
