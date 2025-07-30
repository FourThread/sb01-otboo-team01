package com.fourthread.ozang.batch.config;


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
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Spring Batch 메인 설정 클래스
 * 배치 작업의 전체적인 설정을 관리
 */
@Slf4j
@Configuration
@EnableBatchProcessing
@EnableScheduling
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
        taskExecutor.setConcurrencyLimit(5); // 동시 실행 가능한 배치 작업 수 제한
        taskExecutor.setThreadNamePrefix("batch-task-");
        return taskExecutor;
    }

    /**
     * Chunk Processing 전용 TaskExecutor
     * 대용량 데이터 처리를 위한 최적화된 스레드 풀
     */
    @Bean(name = "chunkProcessingExecutor")
    public TaskExecutor chunkProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // CPU 집약적 작업과 I/O 작업의 균형을 고려한 설정
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(corePoolSize * 2);

        // 큐 크기를 적절히 설정하여 메모리 사용량 제어
        executor.setQueueCapacity(200);

        // 스레드 이름 및 유지 시간 설정
        executor.setThreadNamePrefix("chunk-proc-");
        executor.setKeepAliveSeconds(120);

        // 종료 시 대기 설정
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);

        executor.initialize();

        log.info("Chunk Processing TaskExecutor 설정 완료 - Core={}, Max={}, Queue={}",
            corePoolSize, corePoolSize * 2, executor.getQueueCapacity());

        return executor;
    }

    /**
     * 외부 API 호출용 TaskExecutor (기존 유지하되 최적화)
     * 날씨 API 호출 시 사용되는 전용 스레드 풀
     */
    @Bean(name = "apiCallExecutor")
    public TaskExecutor apiCallExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // API 호출 제한을 고려한 설정
        executor.setCorePoolSize(3);
        executor.setMaxPoolSize(6);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("api-call-");
        executor.setKeepAliveSeconds(300);

        // API 호출 실패 시 빠른 대응을 위한 설정
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        log.info("API 호출용 TaskExecutor 설정 완료 - Core={}, Max={}, Queue={}",
            executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());

        return executor;
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
