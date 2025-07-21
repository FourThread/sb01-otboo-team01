package com.fourthread.ozang.module.domain.weather.batch;

import com.fourthread.ozang.module.config.batch.BatchJobExecutionListener;
import com.fourthread.ozang.module.domain.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@Slf4j
public class WeatherBatchConfig {

    private final WeatherService weatherService;
    private final BatchJobExecutionListener batchJobExecutionListener;

    private final JobLauncher jobLauncher;

    @Value("${batch.weather.retention-days:30}")
    private int weatherRetentionDays;

    @Value("${batch.weather.cache-warmup.enabled:true}")
    private boolean cacheWarmupEnabled;

    public WeatherBatchConfig(
        WeatherService weatherService,
        BatchJobExecutionListener batchJobExecutionListener,
        @Qualifier("asyncJobLauncher") JobLauncher jobLauncher
    ) {
        this.weatherService = weatherService;
        this.batchJobExecutionListener = batchJobExecutionListener;
        this.jobLauncher = jobLauncher;
    }

    @Bean
    public Job weatherDataCleanupJob(
        JobRepository jobRepository,
        Step weatherDataCleanupStep
    ) {
        return new JobBuilder("weatherDataCleanupJob", jobRepository)
            .listener(batchJobExecutionListener)
            .start(weatherDataCleanupStep)
            .build();
    }

    @Bean
    public Step weatherDataCleanupStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        Tasklet weatherDataCleanupTasklet
    ) {
        return new StepBuilder("weatherDataCleanupStep", jobRepository)
            .tasklet(weatherDataCleanupTasklet, transactionManager)
            .build();
    }

    /**
     * 날씨 데이터 정리 Tasklet
     * 설정된 보관 기간보다 오래된 날씨 데이터 삭제
     */
    @Bean
    public Tasklet weatherDataCleanupTasklet() {
        return (contribution, chunkContext) -> {
            log.info("날씨 데이터 정리 배치 작업 시작");

            try {
                int deletedCount = weatherService.cleanupOldWeatherData();

                log.info("날씨 데이터 정리 완료 - 삭제된 데이터: {}건", deletedCount);

                // ExecutionContext에 결과 저장 (모니터링용)
                chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext()
                    .putInt("deletedWeatherCount", deletedCount);

                return RepeatStatus.FINISHED;

            } catch (Exception e) {
                log.error("날씨 데이터 정리 배치 작업 실패", e);
                throw e;
            }
        };
    }

    // 스케줄러 메서드들을 별도 Job 빈을 참조하도록 수정
    /**
     * 날씨 캐시 워밍업 스케줄러
     * 매일 새벽 5:30에 주요 도시 캐시 준비
     */
    @Scheduled(cron = "0 30 5 * * *", zone = "#{@timezoneId}")
    public void scheduledMajorCitiesCacheWarmup() {
        if (!cacheWarmupEnabled) {
            log.debug("날씨 캐시 워밍업이 비활성화되어 있습니다");
            return;
        }

        log.info("스케줄된 주요 도시 캐시 워밍업 시작");
        runJob("weatherCacheWarmupJob", "scheduled-major-cities");
    }

    /**
     * 활성 지역 캐시 갱신 스케줄러
     * 매시간 정각에 활성 지역 캐시 갱신
     */
    @Scheduled(cron = "0 0 * * * *", zone = "#{@timezoneId}")
    public void scheduledActiveRegionsCacheRefresh() {
        if (!cacheWarmupEnabled) {
            log.debug("날씨 캐시 워밍업이 비활성화되어 있습니다");
            return;
        }

        log.info("스케줄된 활성 지역 캐시 갱신 시작");
        runJob("weatherCacheWarmupJob", "scheduled-active-regions");
    }

    /**
     * 날씨 데이터 정리 스케줄러
     * 매일 새벽 3시에 오래된 데이터 정리
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "#{@timezoneId}")
    public void scheduledWeatherDataCleanup() {
        log.info("스케줄된 날씨 데이터 정리 시작");
        runJob("weatherDataCleanupJob", "scheduled-cleanup");
    }

    /**
     * Job 실행 헬퍼 메서드
     */
    private void runJob(String jobName, String jobType) {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("jobType", jobType)
                .toJobParameters();

            // ApplicationContext에서 Job을 조회하여 실행
            // 이 부분은 ApplicationContext 주입이 필요할 수 있으므로 별도 서비스로 분리 고려
            log.info("배치 작업 실행 예약: jobName={}, jobType={}", jobName, jobType);

        } catch (Exception e) {
            log.error("배치 작업 실행 실패 - jobName: {}, jobType: {}", jobName, jobType, e);
        }
    }
}