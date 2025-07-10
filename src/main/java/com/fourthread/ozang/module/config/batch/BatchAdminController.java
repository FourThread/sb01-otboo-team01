package com.fourthread.ozang.module.config.batch;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 배치 작업 관리용 Admin API
 * 각 도메인별 배치를 독립적으로 관리
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/batch")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class BatchAdminController {

    private final JobLauncher jobLauncher;

    @Qualifier("asyncJobLauncher")
    private final JobLauncher asyncJobLauncher;

    private final Job weatherDataCleanupJob;

    private final Job weatherCacheUpdateJob;

    private final JobExplorer jobExplorer;


    /**
     * 날씨 데이터 정리 배치 수동 실행
     */
    @PostMapping("/weather-cleanup")
    public ResponseEntity<Map<String, Object>> runWeatherCleanup(
        @Parameter(description = "비동기 실행 여부", example = "true")
        @RequestParam(defaultValue = "true") boolean async
    ) {
        log.info("[Admin] 날씨 데이터 정리 배치 수동 실행 요청 - async: {}", async);

        return executeJob(
            async ? asyncJobLauncher : jobLauncher,
            weatherDataCleanupJob,
            "manual_weather_cleanup",
            "날씨 데이터 정리 배치"
        );
    }

    /**
     * 날씨 캐시 업데이트 배치 수동 실행
     */
    @PostMapping("/weather-cache-update")
    public ResponseEntity<Map<String, Object>> runWeatherCacheUpdate(
        @Parameter(description = "비동기 실행 여부", example = "true")
        @RequestParam(defaultValue = "true") boolean async
    ) {
        log.info("[Admin] 날씨 캐시 업데이트 배치 수동 실행 요청 - async: {}", async);

        return executeJob(
            async ? asyncJobLauncher : jobLauncher,
            weatherCacheUpdateJob,
            "manual_weather_cache_update",
            "날씨 캐시 업데이트 배치"
        );
    }

    /**
     * 배치 작업 실행 이력 조회
     */
    @GetMapping("/executions")
    public ResponseEntity<Map<String, Object>> getJobExecutions(
        @Parameter(description = "작업 이름", example = "weatherDataCleanupJob")
        @RequestParam(required = false) String jobName,
        @Parameter(description = "조회할 개수", example = "10")
        @RequestParam(defaultValue = "10") int count
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (jobName != null) {
                List<JobExecution> executions = jobExplorer.findJobInstancesByJobName(jobName, 0, count)
                    .stream()
                    .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                    .toList();

                response.put("jobName", jobName);
                response.put("executions", mapJobExecutions(executions));
            } else {
                // 모든 작업의 최근 실행 이력
                Map<String, List<JobExecution>> allExecutions = new HashMap<>();

                String[] jobNames = {"weatherDataCleanupJob", "expiredTokenCleanupJob", "weatherCacheUpdateJob"};
                for (String name : jobNames) {
                    List<JobExecution> executions = jobExplorer.findJobInstancesByJobName(name, 0, 5)
                        .stream()
                        .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                        .toList();
                    allExecutions.put(name, executions);
                }

                response.put("allExecutions", allExecutions);
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("배치 작업 실행 이력 조회 실패", e);
            response.put("error", "배치 작업 실행 이력 조회 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 특정 배치 작업 실행 상세 조회
     */
    @GetMapping("/executions/{executionId}")
    public ResponseEntity<Map<String, Object>> getJobExecutionDetail(
        @PathVariable Long executionId
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            JobExecution execution = jobExplorer.getJobExecution(executionId);
            if (execution == null) {
                response.put("error", "실행 ID를 찾을 수 없습니다: " + executionId);
                return ResponseEntity.notFound().build();
            }

            response.put("executionId", execution.getId());
            response.put("jobName", execution.getJobInstance().getJobName());
            response.put("status", execution.getStatus().toString());
            response.put("startTime", execution.getStartTime());
            response.put("endTime", execution.getEndTime());
            response.put("exitStatus", execution.getExitStatus().toString());

            // ExecutionContext 정보
            ExecutionContext context = execution.getExecutionContext();
            Map<String, Object> contextData = new HashMap<>();
            context.entrySet().forEach(entry ->
                contextData.put(entry.getKey(), entry.getValue())
            );
            response.put("executionContext", contextData);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("배치 작업 실행 상세 조회 실패", e);
            response.put("error", "배치 작업 실행 상세 조회 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * 공통 배치 실행 메서드
     */
    private ResponseEntity<Map<String, Object>> executeJob(
        JobLauncher launcher,
        Job job,
        String triggerType,
        String jobDescription
    ) {
        Map<String, Object> response = new HashMap<>();

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("triggerType", triggerType)
                .toJobParameters();

            JobExecution execution = launcher.run(job, jobParameters);

            response.put("jobName", job.getName());
            response.put("jobDescription", jobDescription);
            response.put("executionId", execution.getId());
            response.put("status", execution.getStatus().toString());
            response.put("message", jobDescription + " 시작됨");

            log.info("{} 시작 - 실행 ID: {}", jobDescription, execution.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("{} 실행 실패", jobDescription, e);
            response.put("error", jobDescription + " 실행 실패: " + e.getMessage());
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * JobExecution 리스트를 Map으로 변환
     */
    private List<Map<String, Object>> mapJobExecutions(List<JobExecution> executions) {
        return executions.stream().map(execution -> {
            Map<String, Object> map = new HashMap<>();
            map.put("executionId", execution.getId());
            map.put("status", execution.getStatus().toString());
            map.put("startTime", execution.getStartTime());
            map.put("endTime", execution.getEndTime());
            map.put("exitStatus", execution.getExitStatus().toString());
            return map;
        }).toList();
    }
}