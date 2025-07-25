package com.fourthread.ozang.core.domain.weather.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.core.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.core.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.core.domain.weather.util.WeatherCacheKeyGenerator;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

/**
 * 날씨 캐시 관리 서비스
 * Redis를 활용한 3단계 캐시 전략 구현
 */
@Slf4j
@Service
public class WeatherCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public WeatherCacheService(@Qualifier("weatherRedisTemplate") RedisTemplate<String, Object> redisTemplate,
        ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        log.info("WeatherCacheService 초기화 완료 - Redis DB: 1번 (Weather 전용)");
    }

    private static final Duration CURRENT_WEATHER_TTL = Duration.ofHours(1);
    private static final Duration FORECAST_WEATHER_TTL = Duration.ofHours(3);
    private static final Duration LOCATION_TTL = Duration.ofHours(24);
    private static final String ACTIVE_REGIONS_KEY = "weather:active:regions";

    /**
     *  현재 날씨 캐시 관리
     */
    public WeatherDto getCurrentWeatherFromCache(double latitude, double longitude) {
        String key = WeatherCacheKeyGenerator.generateCurrentWeatherKey(latitude, longitude);
        try {
            WeatherDto cached = (WeatherDto) redisTemplate.opsForValue().get(key);
            if (cached != null) {
                WeatherDto result = convertToWeatherDto(cached);
                if(result != null) {
                    log.debug("Redis 캐시 히트 - 현재 날씨: {}", key);
                    recordActiveRegion(latitude, longitude);
                    return result;
                }
            }
            return cached;
        } catch (Exception e) {
            log.error("Redis 캐시 조회 실패: {} - 캐시를 삭제합니다.", key, e);
            try {
                redisTemplate.delete(key);
                log.info("손상된 캐시 삭제 완료: {}", key);
            } catch (Exception deleteEx) {
                log.error("캐시 삭제 실패: {}", key, deleteEx);
            }
            return null;
        }
    }

    public void cacheCurrentWeather(double latitude, double longitude, WeatherDto weather) {
        String key = WeatherCacheKeyGenerator.generateCurrentWeatherKey(latitude, longitude);

        try {
            redisTemplate.opsForValue().set(key, weather, CURRENT_WEATHER_TTL);
            log.debug("Redis 캐시 저장 - 현재 날씨: {}", key);
            // 활성 지역으로 기록
            recordActiveRegion(latitude, longitude);
        } catch (Exception e) {
            log.error("Redis 캐시 저장 실패: {}", key, e);
        }
    }

    /**
     *  5일 예보 캐시 관리
     */
    public List<WeatherDto> getForecastFromCache(double latitude, double longitude, String baseTime) {
        String key = WeatherCacheKeyGenerator.generateForecastWeatherKey(latitude, longitude, baseTime);

        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                List<WeatherDto> result = convertToWeatherDtoList(cached);
                if(result != null) {
                    log.debug("Redis 캐시 히트 - 5일 예보: {}", key);
                    recordActiveRegion(latitude, longitude);
                    return result;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Redis 캐시 조회 실패: {} - 캐시를 삭제합니다.", key, e);
            try {
                redisTemplate.delete(key);
                log.info("손상된 캐시 삭제 완료: {}", key);
            } catch (Exception deleteEx) {
                log.error("캐시 삭제 실패: {}", key, deleteEx);
            }
            return null;
        }
    }

    public void cacheForecast(double latitude, double longitude, String baseTime, List<WeatherDto> forecast) {
        String key = WeatherCacheKeyGenerator.generateForecastWeatherKey(latitude, longitude, baseTime);

        try {
            redisTemplate.opsForValue().set(key, forecast, FORECAST_WEATHER_TTL);
            log.debug("Redis 캐시 저장 - 5일 예보: {}", key);
            recordActiveRegion(latitude, longitude);
        } catch (Exception e) {
            log.error("Redis 캐시 저장 실패: {}", key, e);
        }
    }

    /**
     *  위치 정보 캐시 관리
     */
    public WeatherAPILocation getLocationFromCache(double latitude, double longitude) {
        String key = WeatherCacheKeyGenerator.generateLocationKey(latitude, longitude);

        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                WeatherAPILocation result = convertToWeatherAPILocation(cached);
                if(result !=null) {
                    log.debug("Redis 캐시 히트 - 위치 정보: {}", key);
                    return result;
                }
            }
            return null;
        } catch (Exception e) {
            log.error("Redis 캐시 조회 실패: {} - 캐시를 삭제합니다.", key, e);
            try {
                redisTemplate.delete(key);
                log.info("손상된 캐시 삭제 완료: {}", key);
            } catch (Exception deleteEx) {
                log.error("캐시 삭제 실패: {}", key, deleteEx);
            }
            return null;
        }
    }

    public void cacheLocation(double latitude, double longitude, WeatherAPILocation location) {
        String key = WeatherCacheKeyGenerator.generateLocationKey(latitude, longitude);

        try {
            redisTemplate.opsForValue().set(key, location, LOCATION_TTL);
            log.debug("Redis 캐시 저장 - 위치 정보: {}", key);
        } catch (Exception e) {
            log.error("Redis 캐시 저장 실패: {}", key, e);
        }
    }

    /**
     * 타입 안전한 변환 메서드
     * @return WeatherDto
     */
    private WeatherDto convertToWeatherDto(Object cached) {
        try {
            if (cached instanceof WeatherDto) {
                return (WeatherDto) cached;
            }

            if (cached instanceof Map) {
                return objectMapper.convertValue(cached, WeatherDto.class);
            }
        } catch (Exception e) {
            log.warn("WeatherDto 변환 실패", e);
        }
        return null;
    }

    /**
     * 타입 안전한 변환 메서드
     * @return List<WeatherDto>
     */
    private List<WeatherDto> convertToWeatherDtoList(Object cached) {
        try {
            if (cached instanceof List<?> list) {
                return list.stream()
                    .map(item -> {
                        if (item instanceof WeatherDto) {
                            return (WeatherDto) item;
                        }
                        if (item instanceof Map) {
                            return objectMapper.convertValue(item, WeatherDto.class);
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("WeatherDto List 변환 실패", e);
        }
        return null;
    }

    /**
     * 타입 안전한 변환 메서드
     * @return WeatherAPILocation
     */
    private WeatherAPILocation convertToWeatherAPILocation(Object cached) {
        try {
            if (cached instanceof WeatherAPILocation) {
                return (WeatherAPILocation) cached;
            }

            if (cached instanceof Map) {
                return objectMapper.convertValue(cached, WeatherAPILocation.class);
            }
        } catch (Exception e) {
            log.warn("WeatherAPILocation 변환 실패", e);
        }
        return null;
    }

    /**
     * 활성 지역 관리 (배치 작업용)
     */
    private void recordActiveRegion(double latitude, double longitude) {
        String regionKey = String.format("%.2f:%.2f", latitude, longitude);
        double score = System.currentTimeMillis();

        try {
            redisTemplate.opsForZSet().add(ACTIVE_REGIONS_KEY, regionKey, score);
            // 최근 7일간의 데이터만 유지
            long sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7);
            redisTemplate.opsForZSet().removeRangeByScore(ACTIVE_REGIONS_KEY, 0, sevenDaysAgo);
        } catch (Exception e) {
            log.error("활성 지역 기록 실패: {}", regionKey, e);
        }
    }

    /**
     * 최근 활성 지역 조회 (배치 작업에서 사용)
     */
    public List<double[]> getActiveRegions(int limit) {
        try {
            // 최근 조회된 순으로 정렬하여 가져오기
            Set<ZSetOperations.TypedTuple<Object>> regions = redisTemplate.opsForZSet()
                .reverseRangeWithScores(ACTIVE_REGIONS_KEY, 0, limit - 1);

            if (regions == null) {
                return List.of();
            }

            return regions.stream()
                .map(tuple -> {
                    String regionKey = (String) tuple.getValue();
                    String[] parts = regionKey.split(":");
                    return new double[]{
                        Double.parseDouble(parts[0]),
                        Double.parseDouble(parts[1])
                    };
                })
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("활성 지역 조회 실패", e);
            return List.of();
        }
    }

    /**
     * 캐시 워밍업
     */
    public void warmupCache(double latitude, double longitude, WeatherDto currentWeather,
        List<WeatherDto> forecast, WeatherAPILocation location) {
        try {
            // 현재 날씨 캐시
            if (currentWeather != null) {
                cacheCurrentWeather(latitude, longitude, currentWeather);
            }

            // 5일 예보 캐시
            if (forecast != null && !forecast.isEmpty()) {
                String baseTime = LocalDateTime.now().format(WeatherCacheKeyGenerator.HOUR_FORMATTER);
                cacheForecast(latitude, longitude, baseTime, forecast);
            }

            // 위치 정보 캐시
            if (location != null) {
                cacheLocation(latitude, longitude, location);
            }

            log.info("캐시 워밍업 완료 - 위도: {}, 경도: {}", latitude, longitude);
        } catch (Exception e) {
            log.error("캐시 워밍업 실패 - 위도: {}, 경도: {}", latitude, longitude, e);
        }
    }

}