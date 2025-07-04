package com.fourthread.ozang.module.domain.security.jwt;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 블랙 리스트에 포함된 토큰이면 서버는 해당 요청을 무시하고 클라이언트에게 인증 실패를 반환합니다
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtBlacklist {

  private final RedisDao redisDao;

  public void put(String token, Instant expirationTime) {
    Duration ttl = Duration.between(Instant.now(), expirationTime);
    redisDao.setValues("blacklist:" + token, "blacklisted", ttl);
    log.info("[JwtBlacklist] 블랙리스트 등록 - 만료 시간: {}", expirationTime);
  }

  public boolean contains(String token) {
    return redisDao.getValue("blacklist:" + token) != null;
  }
}

