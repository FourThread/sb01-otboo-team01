package com.fourthread.ozang.module.domain.weather.scheduler;

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
 * 날씨 데이터 정리 스케줄러
 * 오래된 날씨 데이터를 주기적으로 정리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherDataCleanupScheduler {

    private final ZoneId zoneId;

    @Qualifier("asyncJobLauncher")
    private final JobLauncher asyncJobLauncher;

    @Qualifier("weatherDataCleanupJob")
    private final Job weatherDataCleanupJob;

    @Value("${batch.scheduler.weather-cleanup.enabled:true}")
    private boolean weatherCleanupEnabled;

    /**
     * 날씨 데이터 정리 작업
     * 매일 자정에 실행
     */
    @Scheduled(cron = "0 0 0 * * ?", zone = "#{@timezoneId}")
    public void runWeatherDataCleanup() {
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
}