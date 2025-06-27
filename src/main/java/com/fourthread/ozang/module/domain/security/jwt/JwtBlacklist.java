package com.fourthread.ozang.module.domain.security.jwt;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class JwtBlacklist {

  private final Map<String, LocalDateTime> blacklist = new ConcurrentHashMap<>();

  public void put(String accessToken, LocalDateTime expirationTime) {
    blacklist.putIfAbsent(accessToken, expirationTime);
  }

  public boolean contains(String accessToken) {
    return blacklist.containsKey(accessToken);
  }

  // 1시간마다 정리
  @Scheduled(fixedDelay = 60 * 60 * 1000)
  public void cleanUp() {
    blacklist.values().removeIf(expirationTime -> expirationTime.isBefore(LocalDateTime.now()));
  }
}
