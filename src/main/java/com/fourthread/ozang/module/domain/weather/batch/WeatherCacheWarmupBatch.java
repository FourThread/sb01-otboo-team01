package com.fourthread.ozang.module.domain.weather.batch;

import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.module.domain.weather.service.WeatherCacheService;
import com.fourthread.ozang.module.domain.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * 날씨 캐시 워밍업 배치 작업
 * 주요 도시 및 활성 지역의 날씨 정보를 미리 캐싱
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WeatherCacheWarmupBatch {

    private final WeatherService weatherService;
    private final WeatherCacheService cacheService;
    private final Executor apiCallExecutor;

    @Value("${weather.cache.warmup.max-regions:50}")
    private int maxWarmupRegions;

    /**
     * 주요 도시 좌표 (서울, 부산, 대구, 인천, 광주, 대전, 울산, 세종, 제주)
     */
    private static final List<double[]> MAJOR_CITIES = Arrays.asList(
        new double[]{37.5665, 126.9780},  // 서울
        new double[]{35.1796, 129.0756},  // 부산
        new double[]{35.8714, 128.6014},  // 대구
        new double[]{37.4563, 126.7052},  // 인천
        new double[]{35.1595, 126.8526},  // 광주
        new double[]{36.3504, 127.3845},  // 대전
        new double[]{35.5384, 129.3114},  // 울산
        new double[]{36.4801, 127.2892},  // 세종
        new double[]{33.4996, 126.5312}   // 제주
    );

    @Bean
    public Job weatherCacheWarmupJob(
        JobRepository jobRepository,
        Step majorCitiesWarmupStep,
        Step activeRegionsWarmupStep
    ) {
        return new JobBuilder("weatherCacheWarmupJob", jobRepository)
            .start(majorCitiesWarmupStep)
            .next(activeRegionsWarmupStep)
            .build();
    }

    /**
     * 주요 도시 캐시 워밍업 Step
     */
    @Bean
    public Step majorCitiesWarmupStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("majorCitiesWarmupStep", jobRepository)
            .tasklet(majorCitiesWarmupTasklet(), transactionManager)
            .build();
    }

    /**
     * 활성 지역 캐시 워밍업 Step
     */
    @Bean
    public Step activeRegionsWarmupStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("activeRegionsWarmupStep", jobRepository)
            .tasklet(activeRegionsWarmupTasklet(), transactionManager)
            .build();
    }

    /**
     * 주요 도시 캐시 워밍업 Tasklet
     */
    @Bean
    public Tasklet majorCitiesWarmupTasklet() {
        return (contribution, chunkContext) -> {
            log.info("주요 도시 날씨 캐시 워밍업 시작");

            int successCount = 0;
            int failureCount = 0;

            // 병렬 처리로 성능 향상
            List<CompletableFuture<Boolean>> futures = MAJOR_CITIES.stream()
                .map(coords -> CompletableFuture.supplyAsync(() ->
                    warmupLocationWeather(coords[0], coords[1]), apiCallExecutor))
                .toList();

            for (CompletableFuture<Boolean> future : futures) {
                try {
                    if (future.join()) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.error("캐시 워밍업 중 오류 발생", e);
                    failureCount++;
                }
            }

            log.info("주요 도시 캐시 워밍업 완료 - 성공: {}개, 실패: {}개",
                successCount, failureCount);

            // ExecutionContext에 결과 저장
            chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .putInt("majorCitiesSuccess", successCount);

            return RepeatStatus.FINISHED;
        };
    }

    /**
     * 활성 지역 캐시 워밍업 Tasklet
     */
    @Bean
    public Tasklet activeRegionsWarmupTasklet() {
        return (contribution, chunkContext) -> {
            log.info("활성 지역 날씨 캐시 워밍업 시작");

            // Redis에서 최근 활성 지역 조회
            List<double[]> activeRegions = cacheService.getActiveRegions(maxWarmupRegions);

            if (activeRegions.isEmpty()) {
                log.info("활성 지역이 없어 워밍업을 건너뜁니다");
                return RepeatStatus.FINISHED;
            }

            log.info("활성 지역 {}개 발견", activeRegions.size());

            int successCount = 0;
            int failureCount = 0;

            // 병렬 처리
            List<CompletableFuture<Boolean>> futures = activeRegions.stream()
                .map(coords -> CompletableFuture.supplyAsync(() ->
                    warmupLocationWeather(coords[0], coords[1]), apiCallExecutor))
                .toList();

            for (CompletableFuture<Boolean> future : futures) {
                try {
                    if (future.join()) {
                        successCount++;
                    } else {
                        failureCount++;
                    }
                } catch (Exception e) {
                    log.error("활성 지역 캐시 워밍업 중 오류 발생", e);
                    failureCount++;
                }
            }

            log.info("활성 지역 캐시 워밍업 완료 - 성공: {}개, 실패: {}개",
                successCount, failureCount);

            // ExecutionContext에 결과 저장
            chunkContext.getStepContext()
                .getStepExecution()
                .getJobExecution()
                .getExecutionContext()
                .putInt("activeRegionsSuccess", successCount);

            return RepeatStatus.FINISHED;
        };
    }

    /**
     * 특정 위치의 날씨 정보를 캐시에 워밍업
     */
    private boolean warmupLocationWeather(double latitude, double longitude) {
        try {
            log.debug("캐시 워밍업 시작 - 위도: {}, 경도: {}", latitude, longitude);

            // 현재 날씨 조회 및 캐싱
//            WeatherDto currentWeather = weatherService.getWeatherForecast(longitude, latitude);

            // 5일 예보 조회 및 캐싱
            List<WeatherDto> forecast = weatherService.getFiveDayForecast(longitude, latitude);

            // 위치 정보 조회 및 캐싱
            WeatherAPILocation location = weatherService.getWeatherLocation(longitude, latitude);

            // 명시적으로 캐시에 저장
            cacheService.warmupCache(latitude, longitude, /*currentWeather,*/ forecast, location);

            log.debug("캐시 워밍업 성공 - 위도: {}, 경도: {}", latitude, longitude);
            return true;

        } catch (Exception e) {
            log.error("캐시 워밍업 실패 - 위도: {}, 경도: {}", latitude, longitude, e);
            return false;
        }
    }
}