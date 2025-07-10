package com.fourthread.ozang.module.domain.weather.batch;

import com.fourthread.ozang.module.config.batch.BatchJobExecutionListener;
import com.fourthread.ozang.module.domain.weather.service.WeatherCacheUpdater;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 날씨 캐시 업데이트 배치 설정
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WeatherCacheBatchConfig {

    private final WeatherCacheUpdater weatherCacheUpdater;
    private final BatchJobExecutionListener batchJobExecutionListener;

    /**
     * 날씨 캐시 업데이트 Job
     * 주기적으로 실행되어 캐시를 최신 상태로 유지
     */
    @Bean
    public Job weatherCacheUpdateJob(
        JobRepository jobRepository,
        Step weatherCacheUpdateStep
    ) {
        return new JobBuilder("weatherCacheUpdateJob", jobRepository)
            .listener(batchJobExecutionListener)
            .start(weatherCacheUpdateStep)
            .build();
    }

    /**
     * 날씨 캐시 업데이트 Step
     */
    @Bean
    public Step weatherCacheUpdateStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        Tasklet weatherCacheUpdateTasklet
    ) {
        return new StepBuilder("weatherCacheUpdateStep", jobRepository)
            .tasklet(weatherCacheUpdateTasklet, transactionManager)
            .build();
    }

    /**
     * 날씨 캐시 업데이트 Tasklet
     * 활성 지역의 날씨 정보를 미리 조회하여 캐시에 저장
     */
    @Bean
    public Tasklet weatherCacheUpdateTasklet() {
        return (contribution, chunkContext) -> {
            log.info("날씨 캐시 업데이트 배치 작업 시작");

            try {
                int updatedCount = weatherCacheUpdater.updateWeatherCache();

                log.info("날씨 캐시 업데이트 완료 - 업데이트된 지역: {}개", updatedCount);

                // ExecutionContext에 결과 저장
                chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext()
                    .putInt("updatedCacheCount", updatedCount);

                return RepeatStatus.FINISHED;

            } catch (Exception e) {
                log.error("날씨 캐시 업데이트 배치 작업 실패", e);
                throw e;
            }
        };
    }
}