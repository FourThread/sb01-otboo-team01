package com.fourthread.ozang.module.config.batch;


import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

/**
 * Spring Batch 메인 설정 클래스
 * 배치 작업의 전체적인 설정을 관리
 */
@Slf4j
@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class BatchConfig {

    private final ZoneId zoneId;
    /**
     * 비동기 배치 작업용 TaskExecutor
     * 배치 작업이 메인 스레드를 블로킹하지 않도록 비동기로 실행
     */
    @Bean
    public TaskExecutor batchTaskExecutor() {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor();
        taskExecutor.setConcurrencyLimit(2); // 동시 실행 가능한 배치 작업 수 제한
        taskExecutor.setThreadNamePrefix("batch-task-");
        return taskExecutor;
    }

    /**
     * 비동기 JobLauncher 설정
     * 스케줄된 배치 작업이 비동기로 실행되도록 설정
     */
    @Bean
    public JobLauncher asyncJobLauncher(JobRepository jobRepository) throws Exception {
        TaskExecutorJobLauncher jobLauncher = new TaskExecutorJobLauncher();
        jobLauncher.setJobRepository(jobRepository);
        jobLauncher.setTaskExecutor(batchTaskExecutor());
        jobLauncher.afterPropertiesSet();
        return jobLauncher;
    }

    /**
     * 배치 작업 실행 상태 모니터링을 위한 리스너
     */
    @Bean
    public BatchJobExecutionListener batchJobExecutionListener() {
        return new BatchJobExecutionListener(zoneId);
    }
}
