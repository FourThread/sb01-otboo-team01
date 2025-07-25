package com.ozang.batch.batch.controller;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
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
@RequestMapping("/api/admin/batch/weathers")
@PreAuthorize("hasRole('ADMIN')")
@Profile("batch")
@ConditionalOnProperty(name = "batch.enabled", havingValue = "true", matchIfMissing = true)
public class WeatherBatchAdminController {

    private final JobLauncher jobLauncher;

    @Qualifier("asyncJobLauncher")
    private final JobLauncher asyncJobLauncher;

    @Qualifier("weatherDataCleanupJob")
    private final Job weatherDataCleanupJob;

    @Qualifier("weatherCacheWarmupJob")
    private final Job weatherCacheWarmupJob;

    @Qualifier("weatherChangeDetectionJob")
    private final Job weatherChangeDetectionJob;

    private final JobExplorer jobExplorer;

    public WeatherBatchAdminController(
        JobLauncher jobLauncher,
        @Qualifier("asyncJobLauncher") JobLauncher asyncJobLauncher,
        @Qualifier("weatherDataCleanupJob") Job weatherDataCleanupJob,
        @Qualifier("weatherCacheWarmupJob") Job weatherCacheWarmupJob,
        @Qualifier("weatherChangeDetectionJob") Job weatherChangeDetectionJob,
        JobExplorer jobExplorer
    ) {
        this.jobLauncher = jobLauncher;
        this.asyncJobLauncher = asyncJobLauncher;
        this.weatherDataCleanupJob = weatherDataCleanupJob;
        this.weatherCacheWarmupJob = weatherCacheWarmupJob;
        this.weatherChangeDetectionJob = weatherChangeDetectionJob;
        this.jobExplorer = jobExplorer;
    }


    /**
     * 날씨 데이터 정리 배치 수동 실행
     */
    @PostMapping("/cleanup")
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
     * 날씨 캐시 워밍업 배치 수동 실행
     */
    @PostMapping("/cache-warmup")
    public ResponseEntity<Map<String, Object>> runWeatherCacheWarmup(
        @Parameter(description = "비동기 실행 여부", example = "true")
        @RequestParam(defaultValue = "true") boolean async
    ) {
        log.info("[Admin] 날씨 캐시 워밍업 배치 수동 실행 요청 - async: {}", async);

        return executeJob(
            async ? asyncJobLauncher : jobLauncher,
            weatherCacheWarmupJob,
            "manual_weather_cache_warmup",
            "날씨 캐시 워밍업 배치"
        );
    }

    /**
     * 날씨 변화 감지 배치 수동 실행
     */
    @PostMapping("/change-detection")
    public ResponseEntity<Map<String, Object>> runWeatherChangeDetection(
        @Parameter(description = "비동기 실행 여부", example = "true")
        @RequestParam(defaultValue = "true") boolean async
    ) {
        log.info("[Admin] 날씨 변화 감지 배치 수동 실행 요청 - async: {}", async);

        return executeJob(
            async ? asyncJobLauncher : jobLauncher,
            weatherChangeDetectionJob,
            "manual_weather_change_detection",
            "날씨 변화 감지 배치"
        );
    }


    /**
     * 배치 작업 실행 이력 조회 (도메인별)
     */
    @GetMapping("/history")
    public ResponseEntity<Map<String, Object>> getBatchHistory(
        @Parameter(description = "조회할 배치 Job 이름", example = "weatherDataCleanupJob")
        @RequestParam(required = false) String jobName,

        @Parameter(description = "조회할 개수", example = "10")
        @RequestParam(defaultValue = "10") int limit
    ) {
        log.info("[Admin] 배치 작업 실행 이력 조회 요청 - Job Name: {}, Limit: {}", jobName, limit);

        try {
            Map<String, Object> response = new HashMap<>();

            if (jobName != null) {
                // 특정 Job 이력 조회
                List<Map<String, Object>> history = getJobHistory(jobName, limit);
                response.put("history", history);
                response.put("jobName", jobName);
            } else {
                // 모든 Job 이력 조회
                Map<String, List<Map<String, Object>>> allHistory = new HashMap<>();
                allHistory.put("weatherDataCleanupJob",
                    getJobHistory("weatherDataCleanupJob", limit));
                allHistory.put("expiredTokenCleanupJob",
                    getJobHistory("expiredTokenCleanupJob", limit));
                allHistory.put("weatherCacheWarmupJob",
                    getJobHistory("weatherCacheWarmupJob", limit));
                response.put("history", allHistory);
            }

            response.put("success", true);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Admin] 배치 작업 실행 이력 조회 실패", e);
            return ResponseEntity.badRequest().body(createErrorResponse("이력 조회 실패", e));
        }
    }

    /**
     * 특정 배치 작업 상태 조회
     */
    @GetMapping("/status/{jobExecutionId}")
    public ResponseEntity<Map<String, Object>> getBatchStatus(
        @Parameter(description = "Job Execution ID")
        @PathVariable Long jobExecutionId
    ) {
        log.info("[Admin] 배치 작업 상태 조회 요청 - Job Execution ID: {}", jobExecutionId);

        try {
            JobExecution jobExecution = jobExplorer.getJobExecution(jobExecutionId);

            if (jobExecution == null) {
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = jobExecutionToMap(jobExecution);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Admin] 배치 작업 상태 조회 실패", e);
            return ResponseEntity.badRequest().body(createErrorResponse("배치 상태 조회 실패", e));
        }
    }

    /**
     * 실행 중인 배치 작업 조회
     */
    @GetMapping("/running")
    public ResponseEntity<Map<String, Object>> getRunningJobs() {
        log.info("[Admin] 실행 중인 배치 작업 조회 요청");

        try {
            Map<String, Object> response = new HashMap<>();

            // 실행 중인 Job 조회
            List<String> runningJobs = jobExplorer.getJobNames().stream()
                .flatMap(jobName -> jobExplorer.getJobInstances(jobName, 0, 100).stream())
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .filter(execution -> execution.isRunning())
                .map(execution -> execution.getJobInstance().getJobName())
                .distinct()
                .toList();

            response.put("runningJobs", runningJobs);
            response.put("count", runningJobs.size());
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Admin] 실행 중인 배치 작업 조회 실패", e);
            return ResponseEntity.badRequest().body(createErrorResponse("실행 중인 작업 조회 실패", e));
        }
    }

    /**
     * 배치 시스템 상태 요약
     */
    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getBatchSummary() {
        log.info("[Admin] 배치 시스템 상태 요약 조회 요청");

        try {
            Map<String, Object> response = new HashMap<>();

            // 최근 실행 상태
            Map<String, Object> weatherStatus = getLatestJobStatus("weatherDataCleanupJob");
            Map<String, Object> tokenStatus = getLatestJobStatus("expiredTokenCleanupJob");
            Map<String, Object> cacheWarmupStatus = getLatestJobStatus("weatherCacheWarmupJob");

            response.put("weatherCleanup", weatherStatus);
            response.put("tokenCleanup", tokenStatus);
            response.put("weatherCacheWarmup", cacheWarmupStatus);
            response.put("success", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Admin] 배치 시스템 상태 요약 조회 실패", e);
            return ResponseEntity.badRequest().body(createErrorResponse("상태 요약 조회 실패", e));
        }
    }


    /**
     * 배치 작업 실행 공통 메서드
     */
    private ResponseEntity<Map<String, Object>> executeJob(
        JobLauncher launcher,
        Job job,
        String jobType,
        String description
    ) {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .addString("jobType", jobType)
                .addString("triggeredBy", "admin")
                .toJobParameters();

            JobExecution jobExecution = launcher.run(job, jobParameters);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", description + "가 시작되었습니다.");
            response.put("jobId", jobExecution.getId());
            response.put("jobName", jobExecution.getJobInstance().getJobName());
            response.put("status", jobExecution.getStatus().toString());

            log.info("[Admin] {} 시작 완료 - Job ID: {}", description, jobExecution.getId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("[Admin] {} 실행 실패", description, e);
            return ResponseEntity.badRequest().body(createErrorResponse("배치 실행 실패", e));
        }
    }

    /**
     * 특정 Job의 실행 이력 조회
     */
    private List<Map<String, Object>> getJobHistory(String jobName, int limit) {
        try {
            return jobExplorer.getJobInstances(jobName, 0, limit)
                .stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .sorted((a, b) -> b.getStartTime().compareTo(a.getStartTime()))
                .limit(limit)
                .map(this::jobExecutionToMap)
                .toList();
        } catch (Exception e) {
            log.warn("[Admin] Job 이력 조회 실패 - Job Name: {}", jobName, e);
            return List.of();
        }
    }

    /**
     * 최신 Job 상태 조회
     */
    private Map<String, Object> getLatestJobStatus(String jobName) {
        try {
            return jobExplorer.getJobInstances(jobName, 0, 1)
                .stream()
                .flatMap(instance -> jobExplorer.getJobExecutions(instance).stream())
                .findFirst()
                .map(this::jobExecutionToMap)
                .orElse(Map.of("status", "NEVER_RUN"));
        } catch (Exception e) {
            log.warn("[Admin] 최신 Job 상태 조회 실패 - Job Name: {}", jobName, e);
            return Map.of("status", "ERROR", "message", e.getMessage());
        }
    }

    /**
     * 에러 응답 생성 유틸리티
     */
    private Map<String, Object> createErrorResponse(String message, Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", message + (e != null ? ": " + e.getMessage() : ""));
        if (e != null) {
            errorResponse.put("error", e.getClass().getSimpleName());
        }
        return errorResponse;
    }

    /**
     * JobExecution을 Map으로 변환하는 유틸리티 메서드
     */
    private Map<String, Object> jobExecutionToMap(JobExecution jobExecution) {
        Map<String, Object> jobInfo = new HashMap<>();
        jobInfo.put("jobExecutionId", jobExecution.getId());
        jobInfo.put("jobName", jobExecution.getJobInstance().getJobName());
        jobInfo.put("status", jobExecution.getStatus().toString());
        jobInfo.put("startTime", jobExecution.getStartTime());
        jobInfo.put("endTime", jobExecution.getEndTime());
        // 실행 결과 정보
        ExecutionContext executionContext = jobExecution.getExecutionContext();
        Map<String, Object> results = new HashMap<>();

        if (executionContext.containsKey("deletedWeatherCount")) {
            results.put("deletedWeatherCount", executionContext.getInt("deletedWeatherCount"));
        }
        if (executionContext.containsKey("majorCitiesSuccess")) {
            results.put("majorCitiesSuccess", executionContext.getInt("majorCitiesSuccess"));
        }
        if (executionContext.containsKey("deletedTokenCount")) {
            results.put("deletedTokenCount", executionContext.getInt("deletedTokenCount"));
        }

        jobInfo.put("results", results);
        return jobInfo;
    }
}