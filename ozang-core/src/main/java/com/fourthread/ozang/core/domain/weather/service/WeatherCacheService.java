package com.fourthread.ozang.core.domain.weather.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fourthread.ozang.core.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.core.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.core.domain.weather.entity.GridCoordinate;
import com.fourthread.ozang.core.domain.weather.util.CoordinateConverter;
import com.fourthread.ozang.core.domain.weather.util.WeatherCacheKeyGenerator;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
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

    private final WeatherCacheKeyGenerator cacheKeyGenerator;

    private final CoordinateConverter coordinateConverter;


    public WeatherCacheService(
        @Qualifier("weatherRedisTemplate") RedisTemplate<String, Object> redisTemplate,
        ObjectMapper objectMapper, WeatherCacheKeyGenerator cacheKeyGenerator,
        CoordinateConverter coordinateConverter) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.cacheKeyGenerator = cacheKeyGenerator;
        this.coordinateConverter = coordinateConverter;
        log.info("WeatherCacheService 초기화 완료 - Redis DB: 1번 (Weather 전용)");
    }

    private static final Duration LOCATION_TTL = Duration.ofHours(24);
    private static final String ACTIVE_REGIONS_KEY = "weather:active:regions";

    /**
     * 5일 예보 캐시 관리
     */
    public List<WeatherDto> getForecastFromCache(double latitude, double longitude,
        String baseTime) {
        String key = cacheKeyGenerator.generateForecastWeatherKey(latitude, longitude, baseTime);

        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                List<WeatherDto> result = convertToWeatherDtoList(cached);
                if (result != null) {
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

    // 배치 기반 캐시 업데이트: TTL 없이 배치에서 직접 관리
    public void cacheForecast(double latitude, double longitude, String baseTime,
        List<WeatherDto> forecast) {
        String key = cacheKeyGenerator.generateForecastWeatherKey(latitude, longitude, baseTime);

        try {
            redisTemplate.opsForValue().set(key, forecast);
            log.debug("Redis 캐시 저장 - 5일 예보: {}", key);
            recordActiveRegion(latitude, longitude);
        } catch (Exception e) {
            log.error("Redis 캐시 저장 실패: {}", key, e);
        }
    }

    /**
     * 위치 정보 캐시 관리
     */
    public WeatherAPILocation getLocationFromCache(double latitude, double longitude) {
        String key = cacheKeyGenerator.generateLocationKey(latitude, longitude);

        try {
            Object cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                WeatherAPILocation result = convertToWeatherAPILocation(cached);
                if (result != null) {
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
        String key = cacheKeyGenerator.generateLocationKey(latitude, longitude);

        try {
            redisTemplate.opsForValue().set(key, location, LOCATION_TTL);
            log.debug("Redis 캐시 저장 - 위치 정보: {}", key);
        } catch (Exception e) {
            log.error("Redis 캐시 저장 실패: {}", key, e);
        }
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
     * 활성 지역 관리
     */
    public void recordActiveRegion(double latitude, double longitude) {
        try {
            GridCoordinate grid = coordinateConverter.convertToGrid(latitude, longitude);
            String regionKey = String.format("%d:%d", grid.getX(), grid.getY());

            redisTemplate.opsForSet().add(ACTIVE_REGIONS_KEY, regionKey);
            // 24시간 TTL 설정
            redisTemplate.expire(ACTIVE_REGIONS_KEY, Duration.ofHours(24));

            log.debug("활성 지역 기록: 격자({}, {}) <- 위경도({}, {})",
                grid.getX(), grid.getY(), latitude, longitude);
        } catch (Exception e) {
            log.error("활성 지역 기록 실패: 위경도({}, {})", latitude, longitude, e);
        }
    }

    /**
     * 격자 키 기반 활성 지역 등록
     */
    public void registerActiveRegionsFromGridKeys(Set<String> gridKeys) {
        if (gridKeys == null || gridKeys.isEmpty()) {
            log.info("등록할 격자 키가 없습니다");
            return;
        }

        try {
            redisTemplate.opsForSet().add(ACTIVE_REGIONS_KEY, gridKeys.toArray(new String[0]));
            // 24시간 TTL 설정
            redisTemplate.expire(ACTIVE_REGIONS_KEY, Duration.ofHours(24));

            log.info("격자 키 기반 활성 지역 등록 완료 - {}개 격자", gridKeys.size());
        } catch (Exception e) {
            log.error("격자 키 기반 활성 지역 등록 실패", e);
        }
    }

    /**
     * 활성 지역 조회 (격자 좌표 기반)
     */
    public List<double[]> getAllActiveRegions() {
        try {
            Set<Object> gridKeys = redisTemplate.opsForSet().members(ACTIVE_REGIONS_KEY);

            if (gridKeys == null || gridKeys.isEmpty()) {
                log.info("Redis에서 활성 지역 데이터를 찾을 수 없습니다");
                return List.of();
            }

            List<double[]> result = gridKeys.stream()
                .map(key -> {
                    try {
                        String gridKey = (String) key;
                        String[] parts = gridKey.split(":");
                        int x = Integer.parseInt(parts[0]);
                        int y = Integer.parseInt(parts[1]);

                        // 격자 좌표를 위경도로 역변환 (대표 위경도 사용)
                        GridCoordinate grid = new GridCoordinate(x, y);
                        return coordinateConverter.convertGridToLatLon(grid);
                    } catch (Exception e) {
                        log.warn("활성 지역 데이터 파싱 실패: {}", key);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            log.info("모든 활성 지역 조회 완료 - 총 {}개 격자", result.size());
            return result;

        } catch (Exception e) {
            log.error("활성 지역 조회 실패", e);
            return List.of();
        }
    }

    /**
     * 이전 배치의 캐시 데이터 정리
     */
    public void cleanupOldCacheData() {
        try {
            // 이전 시간대의 예보 캐시 삭제
            Set<String> keys = redisTemplate.keys("weather:forecast:*");
            if (!keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("이전 예보 캐시 정리 완료 - {}개 키 삭제", keys.size());
            }
        } catch (Exception e) {
            log.error("캐시 정리 실패", e);
        }
    }


    /**
     * 캐시 워밍업
     */
    public void warmupCache(double latitude, double longitude, List<WeatherDto> forecast,
        WeatherAPILocation location) {
        try {
            // 5일 예보 캐시
            if (forecast != null && !forecast.isEmpty()) {
                String baseTime = LocalDateTime.now()
                    .format(WeatherCacheKeyGenerator.HOUR_FORMATTER);
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