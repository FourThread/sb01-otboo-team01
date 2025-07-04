package com.fourthread.ozang.module.domain.security.jwt.batch;

import com.fourthread.ozang.module.config.batch.BatchJobExecutionListener;
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
 * JWT 토큰 도메인 배치 작업 설정
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class JwtBatchConfig {

    private final JwtBatchService jwtBatchService;
    private final BatchJobExecutionListener batchJobExecutionListener;

    /**
     * 만료된 JWT 토큰 정리 Job
     */
    @Bean
    public Job expiredTokenCleanupJob(JobRepository jobRepository, Step expiredTokenCleanupStep) {
        return new JobBuilder("expiredTokenCleanupJob", jobRepository)
            .listener(batchJobExecutionListener)
            .start(expiredTokenCleanupStep)
            .build();
    }

    /**
     * 만료된 토큰 정리 Step
     */
    @Bean
    public Step expiredTokenCleanupStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        Tasklet expiredTokenCleanupTasklet
    ) {
        return new StepBuilder("expiredTokenCleanupStep", jobRepository)
            .tasklet(expiredTokenCleanupTasklet, transactionManager)
            .build();
    }

    /**
     * 만료된 토큰 정리 Tasklet
     * 만료된 JWT 토큰들을 정리
     */
    @Bean
    public Tasklet expiredTokenCleanupTasklet() {
        return (contribution, chunkContext) -> {
            log.info("만료된 JWT 토큰 정리 배치 작업 시작");

            try {
                int deletedCount = jwtBatchService.cleanupExpiredTokens();

                log.info("JWT 토큰 정리 완료 - 삭제된 토큰: {}건", deletedCount);

                // ExecutionContext에 결과 저장 (모니터링용)
                chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext()
                    .putInt("deletedTokenCount", deletedCount);

                return RepeatStatus.FINISHED;

            } catch (Exception e) {
                log.error("만료된 토큰 정리 배치 작업 실패", e);
                throw e;
            }
        };
    }
}