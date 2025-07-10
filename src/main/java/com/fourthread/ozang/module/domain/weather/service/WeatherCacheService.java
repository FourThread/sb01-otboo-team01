package com.fourthread.ozang.module.domain.weather.service;

import com.fourthread.ozang.module.domain.weather.dto.WeatherDto;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherCacheService {

    @Qualifier("weatherRedisTemplate")
    private final RedisTemplate<String, Object> weatherRedisTemplate;

    @Value("${weather.cache.ttl:3600}")
    private long cacheTtlSeconds;

    private static final String WEATHER_CACHE_PREFIX = "weather:";
    private static final String WEATHER_LOCATION_CACHE_PREFIX = "weather:location:";
    private static final String WEATHER_FIVE_DAY_CACHE_PREFIX = "weather:fiveday:";

    /**
     * 날씨 정보 캐시 조회
     * Grid 좌표를 기준으로 캐시 조회
     */
    public Optional<WeatherDto> getWeatherFromCache(Integer gridX, Integer gridY) {
        String cacheKey = generateCacheKey(gridX, gridY);

        try {
            Object cachedData = weatherRedisTemplate.opsForValue().get(cacheKey);
            if (cachedData instanceof WeatherDto) {
                log.debug("날씨 캐시 히트 - Key: {}", cacheKey);
                return Optional.of((WeatherDto) cachedData);
            }

            log.debug("날씨 캐시 미스 - Key: {}", cacheKey);
            return Optional.empty();
        } catch (Exception e) {
            log.error("날씨 캐시 조회 실패 - Key: {}, Error: {}", cacheKey, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 날씨 정보 캐시 저장
     * TTL을 설정하여 주기적 갱신 보장
     */
    public void saveWeatherToCache(Integer gridX, Integer gridY, WeatherDto weatherDto) {
        String cacheKey = generateCacheKey(gridX, gridY);

        try {
            weatherRedisTemplate.opsForValue().set(
                cacheKey,
                weatherDto,
                Duration.ofSeconds(cacheTtlSeconds)
            );
            log.debug("날씨 캐시 저장 - Key: {}, TTL: {}초", cacheKey, cacheTtlSeconds);
        } catch (Exception e) {
            log.error("날씨 캐시 저장 실패 - Key: {}, Error: {}", cacheKey, e.getMessage());
        }
    }

    /**
     * 5일 예보 캐시 조회
     * Grid 좌표 기준 5일 예보 캐시 조회
     */
    public Optional<List<WeatherDto>> getFiveDayForecastFromCache(Integer gridX, Integer gridY) {
        String cacheKey = generateFiveDayCacheKey(gridX, gridY);

        try {
            Object cachedData = weatherRedisTemplate.opsForValue().get(cacheKey);
            if (cachedData instanceof List) {
                log.debug("5일 예보 캐시 히트 - Key: {}", cacheKey);
                return Optional.of((List<WeatherDto>) cachedData);
            }

            log.debug("5일 예보 캐시 미스 - Key: {}", cacheKey);
            return Optional.empty();
        } catch (Exception e) {
            log.error("5일 예보 캐시 조회 실패 - Key: {}, Error: {}", cacheKey, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 5일 예보 캐시 저장
     * 5일 예보는 더 긴 TTL 설정 (변경 빈도가 낮음)
     */
    public void saveFiveDayForecastToCache(Integer gridX, Integer gridY, List<WeatherDto> forecastList) {
        String cacheKey = generateFiveDayCacheKey(gridX, gridY);

        try {
            // 5일 예보는 2시간 캐시
            long fiveDayTtl = cacheTtlSeconds * 2;
            weatherRedisTemplate.opsForValue().set(
                cacheKey,
                forecastList,
                Duration.ofSeconds(fiveDayTtl)
            );
            log.debug("5일 예보 캐시 저장 - Key: {}, TTL: {}초", cacheKey, fiveDayTtl);
        } catch (Exception e) {
            log.error("5일 예보 캐시 저장 실패 - Key: {}, Error: {}", cacheKey, e.getMessage());
        }
    }

    /**
     * 특정 지역 캐시 삭제
     * 배치 업데이트 시 사용
     */
    public void evictWeatherCache(Integer gridX, Integer gridY) {
        String cacheKey = generateCacheKey(gridX, gridY);
        String fiveDayCacheKey = generateFiveDayCacheKey(gridX, gridY);

        try {
            weatherRedisTemplate.delete(cacheKey);
            weatherRedisTemplate.delete(fiveDayCacheKey);
            log.debug("날씨 캐시 삭제 - Keys: {}, {}", cacheKey, fiveDayCacheKey);
        } catch (Exception e) {
            log.error("날씨 캐시 삭제 실패 - Error: {}", e.getMessage());
        }
    }

    /**
     * 전체 날씨 캐시 삭제
     * 전체 갱신 시 사용
     */
    public void evictAllWeatherCache() {
        try {
            var keys = weatherRedisTemplate.keys(WEATHER_CACHE_PREFIX + "*");
            if (keys != null && !keys.isEmpty()) {
                weatherRedisTemplate.delete(keys);
                log.info("전체 날씨 캐시 삭제 완료 - 삭제된 키 개수: {}", keys.size());
            }
        } catch (Exception e) {
            log.error("전체 날씨 캐시 삭제 실패 - Error: {}", e.getMessage());
        }
    }

    /**
     * 캐시 키 생성 - 단일 날씨 정보
     */
    private String generateCacheKey(Integer gridX, Integer gridY) {
        return WEATHER_CACHE_PREFIX + gridX + ":" + gridY;
    }

    /**
     * 캐시 키 생성 - 5일 예보
     */
    private String generateFiveDayCacheKey(Integer gridX, Integer gridY) {
        return WEATHER_FIVE_DAY_CACHE_PREFIX + gridX + ":" + gridY;
    }

    /**
     * 캐시 상태 확인 (모니터링)
     */
    public long getCacheSize() {
        try {
            var keys = weatherRedisTemplate.keys(WEATHER_CACHE_PREFIX + "*");
            return keys != null ? keys.size() : 0;
        } catch (Exception e) {
            log.error("캐시 크기 조회 실패 - Error: {}", e.getMessage());
            return 0;
        }
    }
}