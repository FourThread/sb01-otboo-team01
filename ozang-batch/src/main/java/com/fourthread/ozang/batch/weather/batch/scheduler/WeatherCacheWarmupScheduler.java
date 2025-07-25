package com.fourthread.ozang.domain.weather.batch.scheduler;

import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 날씨 캐시 워밍업 스케줄러
 * 활성 지역 및 주요 도시의 날씨 캐시를 주기적으로 갱신
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("batch")
@ConditionalOnProperty(name = "batch.scheduler.weather-cache-warmup.enabled", havingValue = "true")
public class WeatherCacheWarmupScheduler {

    private final ZoneId zoneId;

    @Qualifier("asyncJobLauncher")
    private final JobLauncher asyncJobLauncher;

    @Qualifier("weatherCacheWarmupJob")
    private final Job weatherCacheWarmupJob;

    @Value("${batch.scheduler.weather-cache-warmup.enabled:true}")
    private boolean weatherCacheWarmupEnabled;

    /**
     * 주요 도시 캐시 워밍업 작업
     * 매일 새벽 5:30에 실행
     */
    @Scheduled(cron = "0 30 5 * * *", zone = "#{@timezoneId}")
    public void runMajorCitiesCacheWarmup() {
        if (!weatherCacheWarmupEnabled) {
            log.debug("날씨 캐시 워밍업이 비활성화되어 있습니다");
            return;
        }

        log.info("[BATCH-SCHEDULER] 주요 도시 캐시 워밍업 시작");
        executeWarmupJob("scheduled_major_cities", "주요 도시 캐시 워밍업");
    }

    /**
     * 활성 지역 캐시 갱신 작업
     * 매시간 정각에 실행
     */
    @Scheduled(cron = "0 0 * * * *", zone = "#{@timezoneId}")
    public void runActiveRegionsCacheRefresh() {
        if (!weatherCacheWarmupEnabled) {
            log.debug("날씨 캐시 워밍업이 비활성화되어 있습니다");
            return;
        }

        log.info("[BATCH-SCHEDULER] 활성 지역 캐시 갱신 시작");
        executeWarmupJob("scheduled_active_regions", "활성 지역 캐시 갱신");
    }

    /**
     * 캐시 워밍업 배치 실행 공통 메서드
     */
    private void executeWarmupJob(String jobType, String description) {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("jobType", jobType)
                .addString("triggeredBy", "scheduler")
                .addString("timezone", zoneId.getId())
                .toJobParameters();

            JobExecution jobExecution = asyncJobLauncher.run(weatherCacheWarmupJob, jobParameters);

            log.info("[BATCH-SCHEDULER] {} 시작 - Job ID={}, Status={}",
                description, jobExecution.getId(), jobExecution.getStatus());

        } catch (Exception e) {
            log.error("[BATCH-SCHEDULER] {} 실행 실패", description, e);
        }
    }
}