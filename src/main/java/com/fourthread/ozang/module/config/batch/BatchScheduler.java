package com.fourthread.ozang.module.config.batch;

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
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * 배치 작업 스케줄링 설정
 * 주기적으로 배치 작업을 실행
 */
@Slf4j
@Configuration
@EnableScheduling
public class BatchScheduler {

    private final ZoneId zoneId;

    @Qualifier("asyncJobLauncher")
    private final JobLauncher asyncJobLauncher;

    @Qualifier("weatherDataCleanupJob")
    private final Job weatherDataCleanupJob;

    @Qualifier("weatherChangeDetectionJob")
    private final Job weatherChangeDetectionJob;

    @Qualifier("weatherCacheWarmupJob")
    private final Job weatherCacheWarmupJob;

    @Value("${batch.scheduler.weather-cleanup.enabled:true}")
    private boolean weatherCleanupEnabled;

    @Value("${batch.scheduler.weather-change-detection.enabled:true}")
    private boolean weatherChangeDetectionEnabled;

    @Value("${batch.scheduler.weather-cache-warmup.enabled:true}")
    private boolean weatherCacheWarmupEnabled;

    public BatchScheduler(
        ZoneId zoneId,
        @Qualifier("asyncJobLauncher") JobLauncher asyncJobLauncher,
        @Qualifier("weatherDataCleanupJob") Job weatherDataCleanupJob,
        @Qualifier("weatherChangeDetectionJob") Job weatherChangeDetectionJob,
        @Qualifier("weatherCacheWarmupJob") Job weatherCacheWarmupJob
    ) {
        this.zoneId = zoneId;
        this.asyncJobLauncher = asyncJobLauncher;
        this.weatherDataCleanupJob = weatherDataCleanupJob;
        this.weatherChangeDetectionJob = weatherChangeDetectionJob;
        this.weatherCacheWarmupJob = weatherCacheWarmupJob;
    }

    /**
     * 날씨 데이터 정리 작업
     */
    @Scheduled(cron = "0 0 0 * * ?", zone = "#{@timezoneId}")
    public void runWeatherDataCleanup() {
        log.info("(Scheduled) 날씨 데이터 정리 작업 시작");

        if (!weatherCleanupEnabled) {
            log.debug("날씨 데이터 정리 작업이 비활성화되어 있습니다");
            return;
        }

        log.info("[Scheduled] 날씨 데이터 정리 작업 시작");

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("jobType", "scheduled_weather_cleanup")
                .addString("triggeredBy", "scheduler")
                .addString("timezone", zoneId.getId())
                .toJobParameters();

            JobExecution jobExecution = asyncJobLauncher.run(weatherDataCleanupJob, jobParameters);

            log.info("[Scheduled] 날씨 데이터 정리 작업 시작 - Job ID={}, Status={}",
                jobExecution.getId(), jobExecution.getStatus());

        } catch (Exception e) {
            log.error("[Scheduled] 날씨 데이터 정리 작업 실행 실패", e);
        }
    }

    /**
     * 날씨 변화 감지 작업
     * 매시 50분에 실행 (초단기예보 제공 후 5분 뒤)
     */
    @Scheduled(cron = "0 50 * * * ?", zone = "#{@timezoneId}")
    public void runWeatherChangeDetection() {
        if (!weatherChangeDetectionEnabled) {
            log.debug("날씨 변화 감지 작업이 비활성화되어 있습니다");
            return;
        }

        log.info("[Scheduled] 날씨 변화 감지 작업 시작");

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("jobType", "scheduled_weather_change_detection")
                .addString("triggeredBy", "scheduler")
                .addString("timezone", zoneId.getId())
                .toJobParameters();

            JobExecution jobExecution = asyncJobLauncher.run(weatherChangeDetectionJob, jobParameters);

            log.info("[Scheduled] 날씨 변화 감지 작업 시작 - Job ID={}, Status={}",
                jobExecution.getId(), jobExecution.getStatus());

        } catch (Exception e) {
            log.error("[Scheduled] 날씨 변화 감지 작업 실행 실패", e);
        }
    }

    /**
     * 활성 지역 캐시 갱신 스케줄러
     * 매시간 정각에 활성 지역 캐시 갱신
     */
    @Scheduled(cron = "0 0 * * * *", zone = "#{@timezoneId}")
    public void scheduledActiveRegionsCacheRefresh() {
        if (!weatherCacheWarmupEnabled) {
            log.debug("날씨 캐시 워밍업이 비활성화되어 있습니다");
            return;
        }

        log.info("[Scheduled] 활성 지역 캐시 갱신 시작");

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("jobType", "scheduled-active-regions")
                .addString("triggeredBy", "scheduler")
                .addString("timezone", zoneId.getId())
                .toJobParameters();

            JobExecution jobExecution = asyncJobLauncher.run(weatherCacheWarmupJob, jobParameters);

            log.info("[Scheduled] 활성 지역 캐시 갱신 시작 - Job ID={}, Status={}",
                jobExecution.getId(), jobExecution.getStatus());

        } catch (Exception e) {
            log.error("[Scheduled] 활성 지역 캐시 갱신 실행 실패", e);
        }
    }
}
