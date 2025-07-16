package com.fourthread.ozang.module.domain.weather.util;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 날씨 캐시 키 생성 유틸리티
 * 5km 반경을 동일한 날씨 그룹으로 처리
 */
@Slf4j
@UtilityClass
public class WeatherCacheKeyGenerator {

    private static final double GRID_SIZE_KM = 5.0; // 5km 그리드
    private static final double KM_PER_LAT_DEGREE = 111.0; // 위도 1도당 약 111km
    private static final DateTimeFormatter HOUR_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHH");

    /**
     * 현재 날씨 캐시 키 생성
     * 형식: weather:current:{gridX}:{gridY}:{hour}
     */
    public static String generateCurrentWeatherKey(double latitude, double longitude) {
        GridCell grid = convertToGrid(latitude, longitude);
        String hourKey = LocalDateTime.now().format(HOUR_FORMATTER);

        String key = String.format("weather:current:%d:%d:%s",
            grid.x, grid.y, hourKey);

        log.debug("현재 날씨 캐시 키 생성: {}", key);
        return key;
    }

    /**
     * 5일 예보 캐시 키 생성
     * 형식: weather:forecast:{gridX}:{gridY}:{baseTime}
     */
    public static String generateForecastWeatherKey(double latitude, double longitude, String baseTime) {
        GridCell grid = convertToGrid(latitude, longitude);

        String key = String.format("weather:forecast:%d:%d:%s",
            grid.x, grid.y, baseTime);

        log.debug("예보 날씨 캐시 키 생성: {}", key);
        return key;
    }

    /**
     * 위치 정보 캐시 키 생성
     * 형식: weather:location:{gridX}:{gridY}
     */
    public static String generateLocationKey(double latitude, double longitude) {
        GridCell grid = convertToGrid(latitude, longitude);

        String key = String.format("weather:location:%d:%d",
            grid.x, grid.y);

        log.debug("위치 정보 캐시 키 생성: {}", key);
        return key;
    }

    /**
     * 활성 지역 키 생성 (배치 작업용)
     * 형식: weather:active:regions
     */
    public static String generateActiveRegionsKey() {
        return "weather:active:regions";
    }

    /**
     * 위경도를 5km 그리드 셀로 변환
     * 동일한 그리드 셀 내의 좌표는 동일한 날씨로 간주
     */
    private static GridCell convertToGrid(double latitude, double longitude) {
        // 경도 1도당 거리는 위도에 따라 변함
        double kmPerLonDegree = KM_PER_LAT_DEGREE * Math.cos(Math.toRadians(latitude));

        // 5km 그리드로 변환
        double gridLatSize = GRID_SIZE_KM / KM_PER_LAT_DEGREE;
        double gridLonSize = GRID_SIZE_KM / kmPerLonDegree;

        int gridX = (int) Math.floor(longitude / gridLonSize);
        int gridY = (int) Math.floor(latitude / gridLatSize);

        log.debug("좌표 변환 - 위도: {}, 경도: {} -> 그리드 X: {}, Y: {}",
            latitude, longitude, gridX, gridY);

        return new GridCell(gridX, gridY);
    }

    /**
     * 그리드 셀 정보
     */
    private static class GridCell {
        final int x;
        final int y;

        GridCell(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * 캐시 키에서 그리드 좌표 추출
     */
    public static int[] extractGridFromKey(String key) {
        String[] parts = key.split(":");
        if (parts.length >= 4) {
            try {
                int x = Integer.parseInt(parts[2]);
                int y = Integer.parseInt(parts[3]);
                return new int[]{x, y};
            } catch (NumberFormatException e) {
                log.error("캐시 키에서 그리드 좌표 추출 실패: {}", key, e);
            }
        }
        return null;
    }
}