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
@RequiredArgsConstructor
public class BatchScheduler {

    private final ZoneId zoneId;

    @Qualifier("asyncJobLauncher")
    private final JobLauncher asyncJobLauncher;

    private final Job weatherDataCleanupJob;
    private final Job expiredTokenCleanupJob;

    @Value("${batch.scheduler.weather-cleanup.enabled:true}")
    private boolean weatherCleanupEnabled;

    @Value("${batch.scheduler.token-cleanup.enabled:true}")
    private boolean tokenCleanupEnabled;

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
     * JWT 토큰 정리 작업
     */
    @Scheduled(cron = "0 0 1 * * ?", zone = "#{@timezoneId}")
    public void runTokenCleanup() {
        if (!tokenCleanupEnabled) {
            log.debug("JWT 토큰 정리 작업이 비활성화되어 있습니다");
            return;
        }

        log.info("[Scheduled] JWT 토큰 정리 작업 시작");

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("jobType", "scheduled_token_cleanup")
                .addString("triggeredBy", "scheduler")
                .addString("timezone", zoneId.getId())
                .toJobParameters();

            JobExecution jobExecution = asyncJobLauncher.run(expiredTokenCleanupJob, jobParameters);

            log.info("[Scheduled] JWT 토큰 정리 작업 시작됨 - Job ID: {}, Status: {}",
                jobExecution.getId(), jobExecution.getStatus());

        } catch (Exception e) {
            log.error("[Scheduled] JWT 토큰 정리 작업 실행 실패", e);
        }
    }

}
