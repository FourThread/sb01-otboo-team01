package com.fourthread.ozang.module.config.batch;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * 배치 작업 실행 모니터링 리스너
 * 배치 작업의 시작, 완료, 실패 등을 로깅하고 모니터링
 */
@Slf4j
@RequiredArgsConstructor
public class BatchJobExecutionListener implements JobExecutionListener {

    private final ZoneId zoneId;

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        LocalDateTime startTime = LocalDateTime.ofInstant(
            jobExecution.getStartTime().atZone(zoneId).toInstant(),
            zoneId
        );

        log.info("배치 작업 시작");
        log.info("Job Name: {}", jobName);
        log.info("Job ID: {}", jobExecution.getId());
        log.info("시작 시간: {}", startTime);
        log.info("작업 파라미터: {}", jobExecution.getJobParameters().toString());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        BatchStatus status = jobExecution.getStatus();
        LocalDateTime startTime = LocalDateTime.ofInstant(
            jobExecution.getStartTime().atZone(zoneId).toInstant(),
            zoneId
        );
        LocalDateTime endTime = LocalDateTime.ofInstant(
            jobExecution.getEndTime().atZone(zoneId).toInstant(),
            zoneId
        );

        Duration duration = Duration.between(startTime, endTime);

        log.info("배치 작업 완료");
        log.info("Job Name: {}", jobName);
        log.info("Job ID: {}", jobExecution.getId());
        log.info("실행 상태: {}", status);
        log.info("시작 시간: {}", startTime);
        log.info("종료 시간: {}", endTime);
        log.info("실행 시간: {}초", duration.toSeconds());

        // 실행 결과 상세 정보 출력
        if (status == BatchStatus.COMPLETED) {
            logJobResults(jobExecution);
            log.info("[Success] 배치 작업이 성공적으로 완료되었습니다.");
        } else if (status == BatchStatus.FAILED) {
            log.error("[Failed] 배치 작업이 실패하였습니다.");
            logJobFailureDetails(jobExecution);
        } else {
            log.warn("[Error] 배치 작업이 비정상적으로 종료되었습니다. (상태: {})", status);
        }
    }

    /**
     * 배치 작업 결과 상세 정보 로깅
     */
    private void logJobResults(JobExecution jobExecution) {
        try {
            var executionContext = jobExecution.getExecutionContext();

            // 삭제된 날씨 데이터 개수
            if (executionContext.containsKey("deletedWeatherCount")) {
                int deletedWeatherCount = executionContext.getInt("deletedWeatherCount");
                log.info("삭제된 날씨 데이터: {}건", deletedWeatherCount);
            }

            // 삭제된 토큰 개수
            if (executionContext.containsKey("deletedTokenCount")) {
                int deletedTokenCount = executionContext.getInt("deletedTokenCount");
                log.info("삭제된 만료 토큰: {}건", deletedTokenCount);
            }

            // Step 실행 정보
            jobExecution.getStepExecutions().forEach(stepExecution -> {
                log.info("Step '{}': {} (읽기: {}, 생략: {}, 쓰기: {})",
                    stepExecution.getStepName(),
                    stepExecution.getStatus(),
                    stepExecution.getReadCount(),
                    stepExecution.getProcessSkipCount(),
                    stepExecution.getWriteCount()
                );
            });

        } catch (Exception e) {
            log.warn("[Error] 배치 작업 결과 정보 로깅 중 오류 발생: {}", e.getMessage());
        }
    }

    /**
     * 배치 작업 실패 시 상세 정보 로깅
     */
    private void logJobFailureDetails(JobExecution jobExecution) {
        try {
            // 실패한 Step과 예외 정보 출력
            jobExecution.getStepExecutions().forEach(stepExecution -> {
                if (stepExecution.getStatus() == BatchStatus.FAILED) {
                    log.error("[Failed] 실패한 Step: {}", stepExecution.getStepName());

                    stepExecution.getFailureExceptions().forEach(exception -> {
                        log.error("[Failed] 실패 원인: {}", exception.getMessage());
                        log.error("[Failed] 예외 타입: {}", exception.getClass().getSimpleName());
                    });
                }
            });

            // Job 레벨 실패 예외 정보
            jobExecution.getFailureExceptions().forEach(exception -> {
                log.error("[Failed] Job 실패 원인: {}", exception.getMessage());
            });

        } catch (Exception e) {
            log.error("[Error] 배치 작업 실패 정보 로깅 중 오류 발생: {}", e.getMessage());
        }
    }
}