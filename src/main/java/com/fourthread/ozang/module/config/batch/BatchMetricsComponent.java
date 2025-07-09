package com.fourthread.ozang.module.config.batch;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/**
 * 배치 작업 성능 메트릭 수집 컴포넌트
 * Micrometer를 사용하여 배치 작업 메트릭 수집
 */
@Component
@Slf4j
public class BatchMetricsComponent implements JobExecutionListener, StepExecutionListener {

    private final MeterRegistry meterRegistry;
    private final ConcurrentHashMap<Long, Long> jobStartTimes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Long> stepStartTimes = new ConcurrentHashMap<>();

    public BatchMetricsComponent(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    // Job 실행 메트릭
    @Override
    public void beforeJob(JobExecution jobExecution) {
        jobStartTimes.put(jobExecution.getId(), System.currentTimeMillis());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        Long startTime = jobStartTimes.remove(jobExecution.getId());
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            String jobName = jobExecution.getJobInstance().getJobName();
            String status = jobExecution.getStatus().toString();

            // Job 실행 시간 기록
            Timer.Sample sample = Timer.start(meterRegistry);
            sample.stop(Timer.builder("batch.job.duration")
                .tag("job", jobName)
                .tag("status", status)
                .register(meterRegistry));

            // Job 실행 횟수 카운터
            Counter.builder("batch.job.executions")
                .tag("job", jobName)
                .tag("status", status)
                .register(meterRegistry)
                .increment();

            log.debug("배치 작업 메트릭 기록 - Job: {}, Status: {}, Duration: {}ms",
                jobName, status, duration);
        }
    }

    // Step 실행 메트릭
    @Override
    public void beforeStep(StepExecution stepExecution) {
        stepStartTimes.put(stepExecution.getId(), System.currentTimeMillis());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        Long startTime = stepStartTimes.remove(stepExecution.getId());
        if (startTime != null) {
            long duration = System.currentTimeMillis() - startTime;
            String stepName = stepExecution.getStepName();
            String status = stepExecution.getStatus().toString();

            // Step 실행 시간 기록
            meterRegistry.timer("batch.step.duration",
                "step", stepName,
                "status", status
            ).record(duration, TimeUnit.MILLISECONDS);

            // Step별 처리 건수 기록
            meterRegistry.gauge("batch.step.read_count",
                stepExecution, StepExecution::getReadCount);
            meterRegistry.gauge("batch.step.write_count",
                stepExecution, StepExecution::getWriteCount);
            meterRegistry.gauge("batch.step.skip_count",
                stepExecution, StepExecution::getSkipCount);
        }

        return stepExecution.getExitStatus();
    }
}