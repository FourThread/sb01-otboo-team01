package com.fourthread.ozang.core.domain.weather.util;

import com.fourthread.ozang.core.domain.weather.entity.GridCoordinate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 날씨 캐시 키 생성 유틸리티
 * 기상청 공식 Lambert Conformal Conic Projection을 사용한 정확한 격자 변환
 */
@Slf4j
@Component
public class WeatherCacheKeyGenerator {
    public static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern(
        "yyyyMMddHH");

    private final CoordinateConverter coordinateConverter;

    public WeatherCacheKeyGenerator(CoordinateConverter coordinateConverter) {
        this.coordinateConverter = coordinateConverter;
    }

    /**
     * 현재 날씨 캐시 키 생성
     * 형식: weather:current:{gridX}:{gridY}:{hour}
     */
    public String generateCurrentWeatherKey(double latitude, double longitude) {
        GridCoordinate grid = coordinateConverter.convertToGrid(latitude, longitude);
        String hourKey = LocalDateTime.now().format(HOUR_FORMATTER);

        String key = String.format("weather:current:%d:%d:%s",
            grid.getX(), grid.getY(), hourKey);

        log.debug("현재 날씨 캐시 키 생성: {} (격자 좌표: {}, {})",
            key, grid.getX(), grid.getY());
        return key;
    }

    /**
     * 5일 예보 캐시 키 생성
     * 형식: weather:forecast:{gridX}:{gridY}:{baseTime}
     */
    public String generateForecastWeatherKey(double latitude, double longitude,
        String baseTime) {
        GridCoordinate grid = coordinateConverter.convertToGrid(latitude, longitude);

        String key = String.format("weather:forecast:%d:%d:%s",
            grid.getX(), grid.getY(), baseTime);

        log.debug("예보 날씨 캐시 키 생성: {} (격자 좌표: {}, {})",
            key, grid.getX(), grid.getY());
        return key;
    }

    /**
     * 위치 정보 캐시 키 생성
     * 형식: weather:location:{lat3}:{lon3}
     * @return 위경도 기반 위치 캐시 키 (소수점 3자리, 약 100m 정밀도)
     */
    public String generateLocationKey(double latitude, double longitude) {
        String lat3 = String.format("%.3f", latitude);
        String lon3 = String.format("%.3f", longitude);

        String key = String.format("weather:location:%s:%s", lat3, lon3);

        log.debug("위치 정보 캐시 키 생성: {} (위경도: {}, {}) - 개선된 위치 기반 키",
            key, latitude, longitude);
        return key;
    }
//    /**
//     * 위치 정보 캐시 키 생성
//     * 형식: weather:location:{gridX}:{gridY}
//     */
//    public String generateLocationKey(double latitude, double longitude) {
//        GridCoordinate grid = coordinateConverter.convertToGrid(latitude, longitude);
//
//        String key = String.format("weather:location:%d:%d",
//            grid.getX(), grid.getY());
//
//        log.debug("위치 정보 캐시 키 생성: {} (격자 좌표: {}, {})",
//            key, grid.getX(), grid.getY());
//        return key;
//    }
}