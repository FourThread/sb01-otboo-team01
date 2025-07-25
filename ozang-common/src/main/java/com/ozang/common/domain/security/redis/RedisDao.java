package com.ozang.common.domain.security.redis;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
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
    try {
      if (ttl != null && !ttl.isNegative() && !ttl.isZero()) {
        redisTemplate.opsForValue().set(key, value, ttl);
        log.debug("Redis에 데이터 저장 성공: key={}, ttl={}", key, ttl);
      } else {
        log.warn("유효하지 않은 TTL 값: {} → TTL 없이 저장 시도하지 않음", ttl);
      }
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 데이터 저장 실패: key={}, error={}", key, e.getMessage());
    } catch (Exception e) {
      log.error("Redis 데이터 저장 중 예외 발생: key={}, error={}", key, e.getMessage(), e);
    }
  }

  // 데이터 조회
  public Object getValue(String key) {
    try {
      log.debug("Redis에서 데이터 조회 시도: key={}", key);
      Object value = redisTemplate.opsForValue().get(key);
      log.debug("Redis 데이터 조회 결과: key={}, found={}", key, value != null);
      return value;
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 데이터 조회 실패: key={}, error={}", key, e.getMessage());
      return null; // 연결 실패 시 null 반환
    } catch (Exception e) {
      log.error("Redis 데이터 조회 중 예외 발생: key={}, error={}", key, e.getMessage(), e);
      return null;
    }
  }

  // 데이터 삭제
  public void delete(String key) {
    try {
      log.debug("Redis에서 데이터 삭제 시도: key={}", key);
      Boolean deleted = redisTemplate.delete(key);
      log.debug("Redis 데이터 삭제 결과: key={}, deleted={}", key, deleted);
    } catch (RedisConnectionFailureException e) {
      log.error("Redis 연결 실패로 데이터 삭제 실패: key={}, error={}", key, e.getMessage());
    } catch (Exception e) {
      log.error("Redis 데이터 삭제 중 예외 발생: key={}, error={}", key, e.getMessage(), e);
    }
  }
}
