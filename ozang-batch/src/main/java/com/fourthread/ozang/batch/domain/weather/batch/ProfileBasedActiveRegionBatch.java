package com.fourthread.ozang.batch.domain.weather.batch;

import com.fourthread.ozang.batch.config.BatchJobExecutionListener;
import com.fourthread.ozang.core.domain.user.entity.Profile;
import com.fourthread.ozang.core.domain.user.repository.ProfileRepository;
import com.fourthread.ozang.core.domain.weather.service.WeatherCacheService;
import java.util.List;
import java.util.Objects;
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

                List<double[]> profileLocations = profiles.stream()
                    .filter(profile -> profile.getLocation() != null)
                    .filter(profile -> profile.getLocation().getLatitude() != null
                        && profile.getLocation().getLongitude() != null)
                    .map(profile -> new double[]{
                        profile.getLocation().getLatitude(),
                        profile.getLocation().getLongitude()
                    })
                    .distinct() // 중복 위치 제거
                    .collect(Collectors.toList());

                log.info("[BATCH-JOB] Profile에서 추출된 위치 정보: {}개", profileLocations.size());

                if (!profileLocations.isEmpty()) {
                    // Redis에 활성 지역으로 등록
                    cacheService.registerActiveRegionsFromProfiles(profileLocations);

                    log.info("[BATCH-JOB] Profile 기반 활성 지역 등록 완료: {}개 지역", profileLocations.size());
                } else {
                    log.info("[BATCH-JOB] 등록할 위치 정보가 없습니다");
                }

                // ExecutionContext에 결과 저장 (모니터링용)
                chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext()
                    .putInt("profileActiveRegionCount", profileLocations.size());

                return RepeatStatus.FINISHED;

            } catch (Exception e) {
                log.error("[BATCH-JOB] Profile 기반 활성 지역 초기화 실패", e);
                throw e;
            }
        };
    }
}
