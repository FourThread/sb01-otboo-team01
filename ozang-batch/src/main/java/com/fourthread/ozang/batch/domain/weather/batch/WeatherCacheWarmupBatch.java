package com.fourthread.ozang.batch.domain.weather.batch;

import com.fourthread.ozang.batch.config.BatchJobExecutionListener;
import com.fourthread.ozang.core.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.core.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.core.domain.weather.service.WeatherCacheService;
import com.fourthread.ozang.core.domain.weather.service.WeatherService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 날씨 5일 예보 배치 작업
 * 활성 지역의 날씨 정보를 미리 캐싱
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WeatherCacheWarmupBatch {

    private final WeatherService weatherService;
    private final WeatherCacheService cacheService;
    private final TaskExecutor batchTaskExecutor;
    private final BatchJobExecutionListener batchJobExecutionListener;

    @Bean
    public Job weatherCacheWarmupJob(
        JobRepository jobRepository,
        Step cacheCleanupStep,
        Step activeRegionsWarmupStep
    ) {
        return new JobBuilder("weatherCacheWarmupJob", jobRepository)
            .listener(batchJobExecutionListener)
            // 배치 기반 캐시 갱신: 이전 캐시 정리 후 새로운 캐시 생성
            .start(cacheCleanupStep)
            .next(activeRegionsWarmupStep)
            .build();
    }

    /**
     * 이전 캐시 데이터 정리 Step
     */
    @Bean
    public Step cacheCleanupStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        Tasklet cacheCleanupTasklet
    ) {
        return new StepBuilder("cacheCleanupStep", jobRepository)
            .tasklet(cacheCleanupTasklet, transactionManager)
            .build();
    }

    /**
     * 이전 캐시 정리 Tasklet
     */
    @Bean
    public Tasklet cacheCleanupTasklet() {
        return (contribution, chunkContext) -> {
            log.info("[BATCH-WARMUP] 이전 캐시 데이터 정리 시작");

            try {
                cacheService.cleanupOldCacheData();
                log.info("[BATCH-WARMUP] 이전 캐시 데이터 정리 완료");

                return RepeatStatus.FINISHED;
            } catch (Exception e) {
                log.error("[BATCH-WARMUP] 캐시 정리 실패", e);
                throw e;
            }
        };
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
            .<double[], CacheWarmupResult>chunk(50, transactionManager)  // API 호출 부하 고려하여 50개씩 처리
            .reader(activeRegionsWarmupReader())
            .processor(weatherCacheWarmupProcessor())
            .writer(cacheWarmupWriter())
            .taskExecutor(batchTaskExecutor)
            .faultTolerant()
            .retryLimit(2)
            .retry(Exception.class)
            .skipLimit(10)
            .skip(Exception.class)
            .build();
    }

    /**
     * 활성 지역 Reader
     */
    @Bean
    public ItemReader<double[]> activeRegionsWarmupReader() {
        return new ItemReader<double[]>() {
            private List<double[]> activeRegions;
            private int currentIndex = 0;

            @Override
            public double[] read() {
                if (activeRegions == null) {
                    activeRegions = cacheService.getAllActiveRegions();
                    log.info("[BATCH-WARMUP] 활성 지역 {}개 캐시 워밍업 시작", activeRegions.size());

                    if (activeRegions.isEmpty()) {
                        log.warn("[BATCH-WARMUP] 활성 지역이 없습니다. Profile 기반 초기화가 필요할 수 있습니다.");
                        return null;
                    }
                }

                if (currentIndex >= activeRegions.size()) {
                    return null;
                }

                return activeRegions.get(currentIndex++);
            }
        };
    }

    /**
     * 캐시 워밍업 Processor
     */
    @Bean
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    public ItemProcessor<double[], CacheWarmupResult> weatherCacheWarmupProcessor() {
        return coordinates -> {
            double latitude = coordinates[0];
            double longitude = coordinates[1];

            try {
                log.debug("[BATCH-WARMUP] 캐시 워밍업 시작 - 위도: {}, 경도: {}", latitude, longitude);

                // 배치 기반 캐시 갱신: 5일 예보와 위치 정보 한 번에 처리
                List<WeatherDto> forecast = weatherService.getFiveDayForecast(longitude, latitude);
                WeatherAPILocation location = weatherService.getWeatherLocation(longitude, latitude);

                cacheService.warmupCache(latitude, longitude, forecast, location);

                log.debug("[BATCH-WARMUP] 캐시 워밍업 성공 - 위도: {}, 경도: {}", latitude, longitude);

                return new CacheWarmupResult(latitude, longitude, true, null);

            } catch (Exception e) {
                log.error("[BATCH-WARMUP] 캐시 워밍업 실패 - 위도: {}, 경도: {}", latitude, longitude, e);
                return new CacheWarmupResult(latitude, longitude, false, e.getMessage());
            }
        };
    }

    /**
     * 캐시 워밍업 Writer
     */
    @Bean
    public ItemWriter<CacheWarmupResult> cacheWarmupWriter() {
        return chunk -> {
            int successCount = 0;
            int failureCount = 0;

            for (CacheWarmupResult result : chunk.getItems()) {
                if (result.success()) {
                    successCount++;
                } else {
                    failureCount++;
                    log.warn("[BATCH-WARMUP] 워밍업 실패 - 위도: {}, 경도: {}, 오류: {}",
                        result.latitude(), result.longitude(), result.errorMessage());
                }
            }

            log.info("[BATCH-WARMUP] 청크 처리 완료 - 성공: {}개, 실패: {}개", successCount, failureCount);
        };
    }

    /**
     * 캐시 워밍업 결과 DTO
     */
    public record CacheWarmupResult(
        double latitude,
        double longitude,
        boolean success,
        String errorMessage
    ) {}
}