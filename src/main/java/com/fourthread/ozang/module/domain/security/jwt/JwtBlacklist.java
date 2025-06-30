package com.fourthread.ozang.module.domain.security.jwt;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 블랙 리스트에 포함된 토큰이면 서버는 해당 요청을 무시하고 클라이언트에게 인증 실패를 반환합니다
 */
@Slf4j
@Component
public class JwtBlacklist {

  private final Map<String, Instant> blacklist = new ConcurrentHashMap<>();

  public void put(String accessToken, Instant expirationTime) {
    blacklist.putIfAbsent(accessToken, expirationTime);
    log.info("[JwtBlackList] 블랙리스트에 Access Token 등록 완료");
  }

  public boolean contains(String accessToken) {
    boolean isBlacklisted = blacklist.containsKey(accessToken);
    if (isBlacklisted) {
      log.info("블랙리스트에 포함된 액세스 토큰 감지");
    }
    return isBlacklisted;
  }

  // 1시간마다 정리
  @Scheduled(fixedDelay = 60 * 60 * 1000)
  public void cleanUp() {
    int before = blacklist.size();
    blacklist.values().removeIf(expirationTime -> expirationTime.isBefore(Instant.now()));
    int after = blacklist.size();
    log.info("블랙리스트 정리 작업 수행 완료: {} → {}", before, after);
  }
}

