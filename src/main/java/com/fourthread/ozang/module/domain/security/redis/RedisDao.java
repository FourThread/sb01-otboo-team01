package com.fourthread.ozang.module.domain.security.redis;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RedisDao {

  private final RedisTemplate<String, Object> redisTemplate;

  public RedisDao(RedisTemplate<String, Object> redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  // 만료 시간이 있는 데이터 저장
  public void setValue(String key, String value, Duration ttl) {
    if (ttl != null && !ttl.isNegative() && !ttl.isZero()) {
      redisTemplate.opsForValue().set(key, value, ttl);
    } else {
      log.warn("유효하지 않은 TTL 값: {} → TTL 없이 저장 시도", ttl);
    }
  }

  // 데이터 조회
  public Object getValue(String key) {
    log.debug("[RedisDao] Redis에 데이터를 조회합니다");
    return redisTemplate.opsForValue().get(key);
  }

  // 데이터 삭제
  public void delete(String key) {
    log.debug("[RedisDao] Redis에 데이터를 제거합니다");
    redisTemplate.delete(key);
  }
}
