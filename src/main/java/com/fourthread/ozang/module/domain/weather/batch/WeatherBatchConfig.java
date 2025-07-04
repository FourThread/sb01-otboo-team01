package com.fourthread.ozang.module.domain.weather.batch;

import com.fourthread.ozang.module.config.batch.BatchJobExecutionListener;
import com.fourthread.ozang.module.config.batch.BatchService;
import com.fourthread.ozang.module.domain.weather.service.WeatherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class WeatherBatchConfig {

    private final WeatherService weatherService;
    private final BatchJobExecutionListener batchJobExecutionListener;

    @Value("${batch.weather.retention-days:30}")
    private int weatherRetentionDays;

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
                log.error("날씨 데이터 정리 배치 작업 실패, e");
                throw e;
            }
        };
    }
}