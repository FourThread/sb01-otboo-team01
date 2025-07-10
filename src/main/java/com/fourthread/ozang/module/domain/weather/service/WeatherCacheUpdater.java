package com.fourthread.ozang.module.domain.weather.service;

import com.fourthread.ozang.module.domain.weather.entity.GridCoordinate;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import com.fourthread.ozang.module.domain.weather.repository.WeatherRepository;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 날씨 캐시 업데이트 서비스
 * 배치 작업에서 호출되어 활성 지역의 날씨 정보를 미리 캐싱
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WeatherCacheUpdater {

    private final WeatherRepository weatherRepository;
    private final WeatherService weatherService;
    private final WeatherCacheService weatherCacheService;

    @Value("${weather.cache.batch.thread-pool-size:5}")
    private int threadPoolSize;

    @Value("${weather.cache.batch.recent-days:7}")
    private int recentDays;

    /**
     * 날씨 캐시 업데이트
     * 최근 조회된 지역들의 날씨 정보를 미리 업데이트
     */
    @Transactional(readOnly = true)
    public int updateWeatherCache() {
        log.info("날씨 캐시 업데이트 시작");

        // 최근 활성 지역 추출
        Set<GridCoordinate> activeGrids = getActiveGridCoordinates();
        log.info("활성 지역 수: {}", activeGrids.size());

        if (activeGrids.isEmpty()) {
            log.info("업데이트할 활성 지역이 없습니다");
            return 0;
        }

        // 병렬로 날씨 정보 업데이트
        ExecutorService executorService = Executors.newFixedThreadPool(threadPoolSize);
        int updatedCount = 0;

        try {
            List<CompletableFuture<Boolean>> futures = activeGrids.stream()
                .map(grid -> CompletableFuture.supplyAsync(() ->
                    updateWeatherForGrid(grid), executorService))
                .collect(Collectors.toList());

            CompletableFuture<Void> allFutures = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );

            allFutures.get(5, TimeUnit.MINUTES);

            // 성공한 업데이트 수 계산
            updatedCount = (int) futures.stream()
                .filter(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        return false;
                    }
                })
                .count();

        } catch (Exception e) {
            log.error("날씨 캐시 업데이트 중 오류 발생", e);
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        log.info("날씨 캐시 업데이트 완료 - 성공: {}/{}", updatedCount, activeGrids.size());
        return updatedCount;
    }

    /**
     * 활성 지역 좌표 추출
     * 최근 N일 동안 조회된 지역들의 Grid 좌표 추출
     */
    private Set<GridCoordinate> getActiveGridCoordinates() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(recentDays);

        // 최근 조회된 날씨 데이터에서 grid 좌표 추출
        List<Weather> recentWeatherData = weatherRepository.findRecentlyAccessedLocations(cutoffDate);

        Set<GridCoordinate> activeGrids = new HashSet<>();
        for (Weather weather : recentWeatherData) {
            if (weather.getLocation() != null) {
                activeGrids.add(new GridCoordinate(
                    weather.getLocation().x(),
                    weather.getLocation().y()
                ));
            }
        }

        // 추가로 주요 도시 좌표 포함 (서울, 부산, 대구, 인천, 광주, 대전, 울산)
        addMajorCityGrids(activeGrids);

        return activeGrids;
    }

    /**
     * 주요 도시 Grid 좌표 추가
     * 항상 캐시되어야 하는 주요 도시들
     */
    private void addMajorCityGrids(Set<GridCoordinate> grids) {
        // 서울
        grids.add(new GridCoordinate(60, 127));
        // 부산
        grids.add(new GridCoordinate(98, 76));
        // 대구
        grids.add(new GridCoordinate(89, 90));
        // 인천
        grids.add(new GridCoordinate(55, 124));
        // 광주
        grids.add(new GridCoordinate(58, 74));
        // 대전
        grids.add(new GridCoordinate(67, 100));
        // 울산
        grids.add(new GridCoordinate(102, 84));
    }

    /**
     * 특정 grid 좌표의 날씨 정보 업데이트
     */
    private boolean updateWeatherForGrid(GridCoordinate grid) {
        try {
            log.debug("Grid ({}, {}) 날씨 정보 업데이트 시작", grid.getX(), grid.getY());

            double latitude = calculateLatitudeFromGrid(grid);
            double longitude = calculateLongitudeFromGrid(grid);

            // 날씨 정보 조회 (캐시 저장)
            weatherService.getWeatherForecast(longitude, latitude);

            log.debug("Grid ({}, {}) 날씨 정보 업데이트 완료", grid.getX(), grid.getY());
            return true;

        } catch (Exception e) {
            log.error("Grid ({}, {}) 날씨 정보 업데이트 실패: {}",
                grid.getX(), grid.getY(), e.getMessage());
            return false;
        }
    }

    /**
     * Grid Y 좌표를 위도로 변환 (근사치)
     */
    private double calculateLatitudeFromGrid(GridCoordinate grid) {
        double RE = 6371.00877;
        double GRID = 5.0;
        double SLAT1 = 30.0;
        double SLAT2 = 60.0;
        double OLON = 126.0;
        double OLAT = 38.0;
        double XO = 43;
        double YO = 136;

        double DEGRAD = Math.PI / 180.0;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double xn = grid.getX() - XO;
        double yn = ro - (grid.getY() - YO);
        double ra = Math.sqrt(xn * xn + yn * yn);
        double alat = Math.pow((re * sf / ra), (1.0 / sn));
        alat = 2.0 * Math.atan(alat) - Math.PI * 0.5;

        return alat * 180.0 / Math.PI;
    }

    /**
     * Grid X 좌표를 경도로 변환 (근사치)
     */
    private double calculateLongitudeFromGrid(GridCoordinate grid) {
        double RE = 6371.00877;
        double GRID = 5.0;
        double SLAT1 = 30.0;
        double SLAT2 = 60.0;
        double OLON = 126.0;
        double OLAT = 38.0;
        double XO = 43;
        double YO = 136;

        double DEGRAD = Math.PI / 180.0;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double xn = grid.getX() - XO;
        double yn = ro - (grid.getY() - YO);
        double theta = 0.0;

        if (xn != 0.0) {
            theta = Math.atan2(xn, yn);
        }

        double alon = theta / sn + olon;

        return alon * 180.0 / Math.PI;
    }
}