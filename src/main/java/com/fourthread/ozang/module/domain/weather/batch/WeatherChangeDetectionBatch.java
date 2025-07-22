package com.fourthread.ozang.module.domain.weather.batch;

import com.fourthread.ozang.module.config.batch.BatchJobExecutionListener;
import com.fourthread.ozang.module.domain.notification.entity.NotificationLevel;
import com.fourthread.ozang.module.domain.notification.event.WeatherChangeDetectedEvent;
import com.fourthread.ozang.module.domain.notification.service.NotificationService;
import com.fourthread.ozang.module.domain.user.repository.UserRepository;
import com.fourthread.ozang.module.domain.weather.dto.WeatherChangeDto;
import com.fourthread.ozang.module.domain.weather.service.WeatherCacheService;
import com.fourthread.ozang.module.domain.weather.service.WeatherService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 날씨 변화 감지 및 알림 배치 작업
 * 매시 50분에 실행하여 활성 지역의 날씨 변화를 감지하고 사용자에게 알림
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("batch")
@ConditionalOnProperty(name = "batch.enabled", havingValue = "true", matchIfMissing = true)
public class WeatherChangeDetectionBatch {

    private final WeatherService weatherService;
    private final WeatherCacheService cacheService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Executor apiCallExecutor;
    private final BatchJobExecutionListener batchJobExecutionListener;

    @Value("${weather.change-detection.max-regions:30}")
    private int maxDetectionRegions;

    @Bean
    public Job weatherChangeDetectionJob(
        JobRepository jobRepository,
        Step weatherChangeDetectionStep
    ) {
        return new JobBuilder("weatherChangeDetectionJob", jobRepository)
            .listener(batchJobExecutionListener)
            .start(weatherChangeDetectionStep)
            .build();
    }

    @Bean
    public Step weatherChangeDetectionStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("weatherChangeDetectionStep", jobRepository)
            .tasklet(weatherChangeDetectionTasklet(), transactionManager)
            .build();
    }

    @Bean
    public Tasklet weatherChangeDetectionTasklet() {
        return (contribution, chunkContext) -> {
            log.info("[BATCH-JOB] 날씨 변화 감지 배치 작업 시작");

            try {
                // Redis에서 최근 활성 지역 조회
                List<double[]> activeRegions = cacheService.getActiveRegions(maxDetectionRegions);

                if (activeRegions.isEmpty()) {
                    log.info("[BATCH-JOB] 활성 지역이 없어 날씨 변화 감지를 건너뜁니다");
                    return RepeatStatus.FINISHED;
                }

                log.info("[BATCH-JOB] 활성 지역 {}개에서 날씨 변화 감지 시작", activeRegions.size());

                int totalChanges = 0;
                int processedRegions = 0;

                // 병렬 처리로 각 지역의 날씨 변화 감지
                List<CompletableFuture<List<WeatherChangeDto>>> futures = activeRegions.stream()
                    .map(coords -> CompletableFuture.supplyAsync(() ->
                        weatherService.detectWeatherChanges(coords[0], coords[1]), apiCallExecutor))
                    .toList();

                for (CompletableFuture<List<WeatherChangeDto>> future : futures) {
                    try {
                        List<WeatherChangeDto> changes = future.join();
                        processedRegions++;

                        if (!changes.isEmpty()) {
                            totalChanges += changes.size();

                            // 해당 지역 사용자들에게 알림 전송
                            sendWeatherChangeNotifications(changes);

                            // 이벤트 발행
                            eventPublisher.publishEvent(new WeatherChangeDetectedEvent(changes));

                            log.info("[BATCH-JOB] 날씨 변화 감지됨 - 지역: {}, 변화 수: {}",
                                getLocationDescription(changes.get(0)), changes.size());
                        }

                    } catch (Exception e) {
                        log.error("[BATCH-JOB] 지역별 날씨 변화 감지 중 오류 발생", e);
                    }
                }

                log.info("[BATCH-JOB] 날씨 변화 감지 배치 작업 완료 - 처리된 지역: {}개, 총 변화: {}건",
                    processedRegions, totalChanges);

                // ExecutionContext에 결과 저장
                chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext()
                    .putInt("processedRegions", processedRegions);

                chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext()
                    .putInt("totalChanges", totalChanges);

                return RepeatStatus.FINISHED;

            } catch (Exception e) {
                log.error("[BATCH-JOB] 날씨 변화 감지 배치 작업 실패", e);
                throw e;
            }
        };
    }

    /**
     * 날씨 변화 알림 전송
     */
    private void sendWeatherChangeNotifications(List<WeatherChangeDto> changes) {
        try {
            /// (임시)모든 사용자에게 알림 TODO: 해당 지역 사용자만 필터링해야 함
            Set<UUID> allUserIds = userRepository.findAllUserIds();

            for (WeatherChangeDto change : changes) {
                String title = "날씨 급변 알림";
                String content = formatChangeMessage(change);
                NotificationLevel level = determineNotificationLevel(change);

                notificationService.createAll(allUserIds, title, content, level);

                log.debug("날씨 변화 알림 전송 완료 - 변화: {}, 대상 사용자: {}명",
                    change.changeType(), allUserIds.size());
            }

        } catch (Exception e) {
            log.error("날씨 변화 알림 전송 실패", e);
        }
    }

    /**
     * 변화 메시지 포맷팅
     */
    private String formatChangeMessage(WeatherChangeDto change) {
        String locationName = change.location().locationNames().isEmpty() ?
            "현재 지역" : change.location().locationNames().get(0);

        return String.format("%s에 %s", locationName, change.description());
    }

    /**
     * 알림 수준 결정
     */
    private NotificationLevel determineNotificationLevel(WeatherChangeDto change) {
        return switch (change.changeType()) {
            case TEMPERATURE_RISE, TEMPERATURE_DROP, WIND_INCREASE -> NotificationLevel.WARNING;
            case PRECIPITATION_START, PRECIPITATION_TYPE_CHANGE -> NotificationLevel.INFO;
            case PRECIPITATION_END, SKY_CHANGE -> NotificationLevel.INFO;
        };
    }

    /**
     * 위치 설명 추출
     */
    private String getLocationDescription(WeatherChangeDto change) {
        List<String> locationNames = change.location().locationNames();
        return locationNames.isEmpty() ?
            String.format("%.2f,%.2f", change.location().latitude(), change.location().longitude()) :
            locationNames.get(0);
    }
}