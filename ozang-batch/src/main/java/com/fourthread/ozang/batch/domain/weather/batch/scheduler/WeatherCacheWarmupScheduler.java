package com.fourthread.ozang.batch.domain.weather.batch.scheduler;

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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 날씨 캐시 워밍업 스케줄러
 * 활성 지역 및 주요 도시의 날씨 캐시를 주기적으로 갱신
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherCacheWarmupScheduler {

    private final ZoneId zoneId;

    @Qualifier("asyncJobLauncher")
    private final JobLauncher asyncJobLauncher;

    @Qualifier("weatherCacheWarmupJob")
    private final Job weatherCacheWarmupJob;

    @Qualifier("profileActiveRegionInitJob")
    private final Job profileActiveRegionInitJob;

    @Value("${batch.scheduler.weather-cache-warmup.enabled:true}")
    private boolean weatherCacheWarmupEnabled;

    /**
     * 기상청 단기예보 제공 시각 기준 캐시 워밍업
     * 매일 2:10, 5:10, 8:10, 11:10, 14:10, 17:10, 20:10, 23:10에 실행
     */
    @Scheduled(cron = "0 10 2,5,8,11,14,17,20,23 * * ?", zone = "#{@timezoneId}")
    public void runWeatherCacheWarmupAtForecastTimes() {
        if (!weatherCacheWarmupEnabled) {
            log.debug("날씨 캐시 워밍업이 비활성화되어 있습니다");
            return;
        }

        log.info("[BATCH-SCHEDULER] 기상청 단기예보 제공 시각 기준 캐시 워밍업 시작");
        executeWarmupJob("scheduled_forecast_time", "기상청 단기예보 제공 시각 캐시 워밍업");
    }

    /**
     * Profile 기반 활성 지역 초기화 작업
     * 매일 새벽 1:00에 실행 (단기예보 첫 제공 시각 전)
     */
    @Scheduled(cron = "0 0 1 * * *", zone = "#{@timezoneId}")
    public void runProfileActiveRegionInit() {
        if (!weatherCacheWarmupEnabled) {
            log.debug("Profile 활성 지역 초기화가 비활성화되어 있습니다");
            return;
        }

        log.info("[BATCH-SCHEDULER] Profile 기반 활성 지역 초기화 시작");
        executeProfileActiveRegionJob();
    }

    /**
     * Profile 기반 활성 지역 초기화 배치 실행
     */
    private void executeProfileActiveRegionJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("jobType", "scheduled_profile_active_region")
                .addString("triggeredBy", "scheduler")
                .addString("timezone", zoneId.getId())
                .toJobParameters();

            JobExecution jobExecution = asyncJobLauncher.run(profileActiveRegionInitJob, jobParameters);

            log.info("[BATCH-SCHEDULER] Profile 기반 활성 지역 초기화 시작 - Job ID={}, Status={}",
                jobExecution.getId(), jobExecution.getStatus());

        } catch (Exception e) {
            log.error("[BATCH-SCHEDULER] Profile 기반 활성 지역 초기화 실행 실패", e);
        }
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