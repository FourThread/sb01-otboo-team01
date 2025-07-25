package com.fourthread.ozang.batch.weather.batch.scheduler;

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
 * 날씨 변화 감지 스케줄러
 * 활성 지역의 날씨 변화를 감지하고 알림 전송
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Profile("batch")
@ConditionalOnProperty(name = "batch.scheduler.weather-change-detection.enabled", havingValue = "true")
public class WeatherChangeDetectionScheduler {

    private final ZoneId zoneId;

    @Qualifier("asyncJobLauncher")
    private final JobLauncher asyncJobLauncher;

    @Qualifier("weatherChangeDetectionJob")
    private final Job weatherChangeDetectionJob;

    @Value("${batch.scheduler.weather-change-detection.enabled:true}")
    private boolean weatherChangeDetectionEnabled;

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

        log.info("[BATCH-SCHEDULER] 날씨 변화 감지 작업 시작");

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("jobType", "scheduled_weather_change_detection")
                .addString("triggeredBy", "scheduler")
                .addString("timezone", zoneId.getId())
                .toJobParameters();

            JobExecution jobExecution = asyncJobLauncher.run(weatherChangeDetectionJob, jobParameters);

            log.info("[BATCH-SCHEDULER] 날씨 변화 감지 작업 시작 - Job ID={}, Status={}",
                jobExecution.getId(), jobExecution.getStatus());

        } catch (Exception e) {
            log.error("[BATCH-SCHEDULER] 날씨 변화 감지 작업 실행 실패", e);
        }
    }
}