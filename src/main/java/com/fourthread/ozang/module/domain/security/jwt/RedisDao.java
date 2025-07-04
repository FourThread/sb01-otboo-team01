package com.fourthread.ozang.module.domain.security.jwt;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisDao {

  private final RedisTemplate<String, Object> redisTemplate;
  private final ValueOperations<String, Object> valueOperations;

  // 데이터 저장
  public void setValue(String key, String value) {
    valueOperations.set(key, value);
  }

  // 만료 시간이 있는 데이터 저장
  public void setValues(String key, String data, Duration duration) {
    valueOperations.set(key, data, duration);
  }

  // 데이터 조회
  public Object getValue(String key) {
    return valueOperations.get(key);
  }

  // 데이터 삭제
  public void delete(String key) {
    redisTemplate.delete(key);
  }
}
