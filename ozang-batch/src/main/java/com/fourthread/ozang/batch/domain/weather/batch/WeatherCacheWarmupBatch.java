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
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.transaction.PlatformTransactionManager;

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
    private final TaskExecutor batchTaskExecutor;
    private final BatchJobExecutionListener batchJobExecutionListener;

    @Bean
    public Job weatherCacheWarmupJob(
        JobRepository jobRepository,
        Step activeRegionsWarmupStep
    ) {
        return new JobBuilder("weatherCacheWarmupJob", jobRepository)
            .listener(batchJobExecutionListener)
            .start(activeRegionsWarmupStep)
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
            .<double[], CacheWarmupResult>chunk(100, transactionManager)  // 100개씩 처리 (API 호출 부하 고려)
            .reader(activeRegionsWarmupReader())
            .processor(weatherCacheWarmupProcessor())
            .writer(cacheWarmupWriter())
            .taskExecutor(batchTaskExecutor)
//            .throttleLimit(3)  // API 호출 제한 고려
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

                // 현재 날씨, 5일 예보, 위치 정보를 순차적으로 조회
                List<WeatherDto> forecast = weatherService.getFiveDayForecast(longitude, latitude);
                WeatherAPILocation location = weatherService.getWeatherLocation(longitude, latitude);

                // 명시적으로 캐시에 저장
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