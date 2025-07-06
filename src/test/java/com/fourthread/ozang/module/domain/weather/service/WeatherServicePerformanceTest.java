package com.fourthread.ozang.module.domain.weather.service;

import com.fourthread.ozang.module.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.repository.WeatherRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
@DisplayName("날씨 서비스 성능 테스트")
public class WeatherServicePerformanceTest {

    @Autowired
    private WeatherService weatherService;

    @Autowired
    private WeatherRepository weatherRepository;

    // 테스트용 좌표
    private static final Double[][] TEST_COORDINATES = {
        {126.9780, 37.5665}, // 서울시청
        {129.0756, 35.1796}, // 부산시청  
        {126.8526, 35.2028}, // 광주시청
        {127.3845, 36.3504}, // 대전시청
        {127.0245, 37.2617}, // 수원시청
        {128.6014, 35.8714}, // 대구시청
        {127.2898, 36.4804}, // 세종시청
        {126.4597, 33.4996}  // 제주시청
    };

    private static final int TEST_ITERATIONS = 3;

    @BeforeEach
    @Transactional
    void setUp() {
        log.info("데이터베이스 정리");
        try {
            weatherRepository.deleteAll();
            log.debug("데이터베이스 정리 완료");
        } catch (Exception e) {
            log.warn("데이터베이스 정리 실패: {}", e.getMessage());
        }
    }

    @Test
    @DisplayName("getWeatherForecast 성능 테스트")
    void performanceTest_getWeatherForecast() {
        List<Long> responseTimes = new ArrayList<>();
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        int successCount = 0;

        log.info("성능 측정 시작 ({}회 반복)...", TEST_ITERATIONS);

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Double[] coord = TEST_COORDINATES[i % TEST_COORDINATES.length];
            Double testLongitude = coord[0];
            Double testLatitude = coord[1];

            long startTime = System.nanoTime();
            try {
                WeatherDto result = weatherService.getWeatherForecast(testLongitude, testLatitude);
                long endTime = System.nanoTime();
                long responseTime = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

                // 결과 검증
                assertThat(result).isNotNull();
                assertThat(result.id()).isNotNull();
                assertThat(result.location()).isNotNull();

                // 온도 값 검증
                Double currentTemp = result.temperature().current();
                if (currentTemp == null || currentTemp == 0.0) {
                    log.warn("온도 값 이상 - 현재온도: {}°C, Weather ID: {}", currentTemp, result.id());
                }

                responseTimes.add(responseTime);
                totalTime += responseTime;
                minTime = Math.min(minTime, responseTime);
                maxTime = Math.max(maxTime, responseTime);
                successCount++;

                log.info("테스트 {}/{}: {}ms - 좌표: ({}, {}), 온도: {}°C, 위치: {}",
                    i + 1, TEST_ITERATIONS, responseTime,
                    String.format("%.4f", testLatitude), String.format("%.4f", testLongitude),
                    currentTemp != null ? String.format("%.1f", currentTemp) : "null",
                    result.location().locationNames().get(0));

                Thread.sleep(800);

            } catch (Exception e) {
                log.error("테스트 {}/{} 실패 - 좌표: ({}, {}): {}",
                    i + 1, TEST_ITERATIONS, testLatitude, testLongitude, e.getMessage());

                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        printPerformanceResults("getWeatherForecast", responseTimes, totalTime, minTime, maxTime, successCount);
    }

    @Test
    @DisplayName("getFiveDayForecast 성능 테스트")
    void performanceTest_getFiveDayForecast() {
        List<Long> responseTimes = new ArrayList<>();
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        int successCount = 0;

        log.info("성능 측정 시작 ({}회 반복)...", TEST_ITERATIONS);

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Double[] coord = TEST_COORDINATES[i % TEST_COORDINATES.length];
            Double testLongitude = coord[0];
            Double testLatitude = coord[1];

            long startTime = System.nanoTime();
            try {
                List<WeatherDto> result = weatherService.getFiveDayForecast(testLongitude, testLatitude);
                long endTime = System.nanoTime();
                long responseTime = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

                // 결과 검증
                assertThat(result).isNotNull().isNotEmpty();
                assertThat(result.size()).isLessThanOrEqualTo(5);

                // 온도 값 검증
                Double firstDayTemp = result.get(0).temperature().current();
                if (firstDayTemp == null || firstDayTemp == 0.0) {
                    log.warn("5일 예보 온도 값 이상 - 첫날온도: {}°C", firstDayTemp);
                }

                responseTimes.add(responseTime);
                totalTime += responseTime;
                minTime = Math.min(minTime, responseTime);
                maxTime = Math.max(maxTime, responseTime);
                successCount++;

                log.info("테스트 {}/{}: {}ms - 좌표: ({}, {}), 예보 개수: {}일, 첫날 온도: {}°C",
                    i + 1, TEST_ITERATIONS, responseTime,
                    String.format("%.4f", testLatitude), String.format("%.4f", testLongitude),
                    result.size(),
                    firstDayTemp != null ? String.format("%.1f", firstDayTemp) : "null");

                Thread.sleep(800);

            } catch (Exception e) {
                log.error("테스트 {}/{} 실패 - 좌표: ({}, {}): {}",
                    i + 1, TEST_ITERATIONS, testLatitude, testLongitude, e.getMessage());

                try {
                    Thread.sleep(1500);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        printPerformanceResults("getFiveDayForecast", responseTimes, totalTime, minTime, maxTime, successCount);
    }

    @Test
    @DisplayName("getWeatherLocation 성능 테스트")
    void performanceTest_getWeatherLocation() {
        List<Long> responseTimes = new ArrayList<>();
        long totalTime = 0;
        long minTime = Long.MAX_VALUE;
        long maxTime = Long.MIN_VALUE;
        int successCount = 0;

        log.info("성능 측정 시작 ({}회 반복)...", TEST_ITERATIONS);

        for (int i = 0; i < TEST_ITERATIONS; i++) {
            Double[] coord = TEST_COORDINATES[i % TEST_COORDINATES.length];
            Double testLongitude = coord[0];
            Double testLatitude = coord[1];

            long startTime = System.nanoTime();
            try {
                WeatherAPILocation result = weatherService.getWeatherLocation(testLongitude, testLatitude);
                long endTime = System.nanoTime();
                long responseTime = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);

                // 결과 검증
                assertThat(result).isNotNull();
                assertThat(result.latitude()).isEqualTo(testLatitude);
                assertThat(result.longitude()).isEqualTo(testLongitude);
                assertThat(result.locationNames()).isNotEmpty();

                responseTimes.add(responseTime);
                totalTime += responseTime;
                minTime = Math.min(minTime, responseTime);
                maxTime = Math.max(maxTime, responseTime);
                successCount++;

                log.info("테스트 {}/{}: {}ms - 좌표: ({}, {}), 위치: {}",
                    i + 1, TEST_ITERATIONS, responseTime,
                    String.format("%.4f", testLatitude), String.format("%.4f", testLongitude),
                    String.join(", ", result.locationNames()));

                Thread.sleep(400);

            } catch (Exception e) {
                log.error("테스트 {}/{} 실패 - 좌표: ({}, {}): {}",
                    i + 1, TEST_ITERATIONS, testLatitude, testLongitude, e.getMessage());
            }
        }

        printPerformanceResults("getWeatherLocation", responseTimes, totalTime, minTime, maxTime, successCount);
    }

    private void printPerformanceResults(String methodName, List<Long> responseTimes,
        long totalTime, long minTime, long maxTime, int successCount) {
        if (successCount == 0) {
            log.error("=== {} 성능 테스트 결과: 모든 요청 실패 ===", methodName);
            return;
        }

        double avgTime = (double) totalTime / successCount;
        double median = calculateMedian(responseTimes);
        double percentile95 = calculatePercentile(responseTimes, 95);
        double standardDeviation = calculateStandardDeviation(responseTimes, avgTime);
        double successRate = (double) successCount / TEST_ITERATIONS * 100;
        double tps = successCount * 1000.0 / totalTime;

        log.info("=== {} 성능 테스트 결과 ===", methodName);
        log.info("성공률: {}/{} ({}%)", successCount, TEST_ITERATIONS, String.format("%.1f", successRate));
        log.info("평균 응답시간: {}ms", String.format("%.1f", avgTime));
        log.info("중간값(Median): {}ms", String.format("%.1f", median));
        log.info("최소 응답시간: {}ms", minTime);
        log.info("최대 응답시간: {}ms", maxTime);
        log.info("95% Percentile: {}ms", String.format("%.1f", percentile95));
        log.info("표준편차: {}ms", String.format("%.1f", standardDeviation));
        log.info("총 실행시간: {}ms", totalTime);
        log.info("TPS (초당 처리량): {}", String.format("%.2f", tps));
        log.info("========================================");
    }

    private double calculateMedian(List<Long> values) {
        if (values.isEmpty()) return 0;

        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);

        int size = sorted.size();
        if (size % 2 == 0) {
            return (sorted.get(size / 2 - 1) + sorted.get(size / 2)) / 2.0;
        } else {
            return sorted.get(size / 2);
        }
    }

    private double calculatePercentile(List<Long> values, int percentile) {
        if (values.isEmpty()) return 0;

        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);

        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1)));
    }

    private double calculateStandardDeviation(List<Long> values, double mean) {
        if (values.isEmpty()) return 0;

        double sum = 0;
        for (Long value : values) {
            sum += Math.pow(value - mean, 2);
        }
        return Math.sqrt(sum / values.size());
    }
}