package com.fourthread.ozang.batch.domain.weather.batch;

import com.fourthread.ozang.batch.config.BatchJobExecutionListener;
import com.fourthread.ozang.core.domain.user.entity.Profile;
import com.fourthread.ozang.core.domain.user.repository.ProfileRepository;
import com.fourthread.ozang.core.domain.weather.service.WeatherCacheService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
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
 * Profile 기반 활성 지역 초기화 배치 작업
 * DB의 사용자 프로필 위치 정보를 활성 지역으로 등록
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ProfileBasedActiveRegionBatch {
    private final ProfileRepository profileRepository;
    private final WeatherCacheService cacheService;
    private final BatchJobExecutionListener batchJobExecutionListener;

    @Bean
    public Job profileActiveRegionInitJob(
        JobRepository jobRepository,
        Step profileActiveRegionInitStep
    ) {
        return new JobBuilder("profileActiveRegionInitJob", jobRepository)
            .listener(batchJobExecutionListener)
            .start(profileActiveRegionInitStep)
            .build();
    }

    @Bean
    public Step profileActiveRegionInitStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        Tasklet profileActiveRegionInitTasklet
    ) {
        return new StepBuilder("profileActiveRegionInitStep", jobRepository)
            .tasklet(profileActiveRegionInitTasklet, transactionManager)
            .build();
    }

    /**
     * Profile 기반 활성 지역 초기화 Tasklet
     * 모든 사용자 프로필의 위치 정보를 조회하여 활성 지역으로 등록
     */
    @Bean
    public Tasklet profileActiveRegionInitTasklet() {
        return (contribution, chunkContext) -> {
            log.info("[BATCH-JOB] Profile 기반 활성 지역 초기화 시작");

            try {
                // 모든 프로필의 위치 정보 조회
                List<Profile> profiles = profileRepository.findAll();

                Set<String> gridKeys = profiles.stream()
                    .filter(profile -> profile.getLocation() != null)
                    .filter(profile -> profile.getLocation().getX() != null
                        && profile.getLocation().getY() != null)
                    .map(profile -> String.format("%d:%d",
                        profile.getLocation().getX(),
                        profile.getLocation().getY()))
                    .collect(Collectors.toSet()); // Set으로 중복 격자 자동 제거

                log.info("[BATCH-JOB] Profile {}개에서 고유 격자 {}개 추출 완료",
                    profiles.size(), gridKeys.size());

                if (!gridKeys.isEmpty()) {
                    // Redis에 활성 지역으로 등록
                    cacheService.registerActiveRegionsFromGridKeys(gridKeys);

                    log.info("[BATCH-JOB] Profile 기반 활성 지역 등록 완료: {}개 격자", gridKeys.size());
                } else {
                    log.info("[BATCH-JOB] 등록할 격자 정보가 없습니다");
                }

                // ExecutionContext에 결과 저장 (모니터링용)
                chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext()
                    .put("profileActiveRegionCount", gridKeys.size());

                return RepeatStatus.FINISHED;

            } catch (Exception e) {
                log.error("[BATCH-JOB] Profile 기반 활성 지역 초기화 실패", e);
                throw e;
            }
        };
    }
}
