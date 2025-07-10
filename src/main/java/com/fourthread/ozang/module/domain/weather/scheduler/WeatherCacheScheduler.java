package com.fourthread.ozang.module.domain.weather.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 날씨 캐시 배치 스케줄러
 * 주기적으로 날씨 캐시를 업데이트
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "batch.scheduler.weather-cache.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class WeatherCacheScheduler {

    @Qualifier("asyncJobLauncher")
    private final JobLauncher asyncJobLauncher;

    private final Job weatherCacheUpdateJob;

    @Value("${batch.scheduler.weather-cache.enabled:false}")
    private boolean enabled;

    /**
     * 날씨 캐시 업데이트 스케줄
     * 매 시간 정각에 실행 (기상청 데이터 업데이트 주기 고려)
     */
    @Scheduled(cron = "${batch.scheduler.weather-cache.cron:0 0 * * * ?}")
    public void updateWeatherCache() {
        if (!enabled) {
            log.debug("날씨 캐시 업데이트 스케줄러가 비활성화되어 있습니다");
            return;
        }

        log.info("날씨 캐시 업데이트 배치 작업 시작 (스케줄러)");

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("scheduledTime", System.currentTimeMillis())
                .addString("triggerType", "scheduled")
                .toJobParameters();

            asyncJobLauncher.run(weatherCacheUpdateJob, jobParameters);
            log.info("날씨 캐시 업데이트 배치 작업이 비동기로 시작되었습니다");

        } catch (Exception e) {
            log.error("날씨 캐시 업데이트 배치 작업 실행 실패", e);
        }
    }

    /**
     * 날씨 캐시 워밍업 스케줄
     * 새벽 시간대에 주요 도시 캐시 미리 준비
     */
    @Scheduled(cron = "${batch.scheduler.weather-cache.warmup-cron:0 30 5 * * ?}")
    public void warmupWeatherCache() {
        if (!enabled) {
            return;
        }

        log.info("날씨 캐시 워밍업 배치 작업 시작");

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("warmupTime", System.currentTimeMillis())
                .addString("triggerType", "warmup")
                .toJobParameters();

            asyncJobLauncher.run(weatherCacheUpdateJob, jobParameters);
            log.info("날씨 캐시 워밍업 배치 작업이 비동기로 시작되었습니다");

        } catch (Exception e) {
            log.error("날씨 캐시 워밍업 배치 작업 실행 실패", e);
        }
    }
}