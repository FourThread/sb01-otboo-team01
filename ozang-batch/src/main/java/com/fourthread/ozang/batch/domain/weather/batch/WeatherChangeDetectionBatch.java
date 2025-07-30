package com.fourthread.ozang.batch.domain.weather.batch;

import com.fourthread.ozang.batch.config.BatchJobExecutionListener;
import com.fourthread.ozang.core.domain.notification.entity.NotificationLevel;
import com.fourthread.ozang.core.domain.notification.service.NotificationService;
import com.fourthread.ozang.core.domain.weather.dto.WeatherChangeDto;
import com.fourthread.ozang.core.domain.weather.service.WeatherCacheService;
import com.fourthread.ozang.core.domain.weather.service.WeatherService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 날씨 변화 감지 및 알림 배치 작업
 * 매시 50분에 실행하여 활성 지역의 날씨 변화를 감지하고 사용자에게 알림
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WeatherChangeDetectionBatch {

    private final WeatherService weatherService;
    private final WeatherCacheService cacheService;
    private final NotificationService notificationService;
    private final TaskExecutor batchTaskExecutor;
    private final BatchJobExecutionListener batchJobExecutionListener;

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
        PlatformTransactionManager transactionManager) {
        return new StepBuilder("weatherChangeDetectionStep", jobRepository)
            .<double[], List<WeatherChangeDto>>chunk(100, transactionManager) // 100개씩 청크 처리
            .reader(activeRegionsReader())
            .processor(weatherChangeProcessor())
            .writer(weatherChangeWriter())
            .taskExecutor(batchTaskExecutor) // 병렬 처리 활성화
//            .throttleLimit(4)
            .build();
    }

    /**
     * ItemReader: Redis에서 모든 활성 지역 조회
     * 모든 활성 지역 데이터를 청크 단위로 읽기
     * 상태 초기화: 매번 Redis에서 최신 활성 지역 데이터 조회
     */
    @Bean
    @StepScope
    public ItemReader<double[]> activeRegionsReader() {
        return new ItemReader<double[]>() {
            private List<double[]> activeRegions;
            private int currentIndex = 0;
            private boolean initialized = false;

            @Override
            public double[] read()
                throws ParseException, NonTransientResourceException {

                // 매 Step 실행마다 Redis에서 활성 지역을 새로 조회
                if (!initialized) {
                    log.info("[BATCH-READER] 활성 지역 로드 시작 (Step 스코프 초기화)");

                    // Redis에서 활성 지역 조회
                    activeRegions = cacheService.getAllActiveRegions();
                    initialized = true;

                    log.info("[BATCH-READER] 활성 지역 {}개 로드 완료", activeRegions.size());

                    if (activeRegions.isEmpty()) {
                        log.warn("[BATCH-READER] 활성 지역이 없어 처리를 종료합니다");
                        log.info("[BATCH-READER] Redis 키 상태 재확인을 위해 직접 조회 시도...");

                        // Redis 연결 상태와 키 존재 여부를 재확인
                        try {
                            List<double[]> retryRegions = cacheService.getAllActiveRegions();
                            log.info("[BATCH-READER] 재조회 결과: {}개 지역", retryRegions.size());
                        } catch (Exception e) {
                            log.error("[BATCH-READER] Redis 재조회 실패", e);
                        }

                        return null;
                    }

                    for (int i = 0; i < Math.min(activeRegions.size(), 5); i++) {
                        double[] coords = activeRegions.get(i);
                        log.info("[BATCH-READER] 활성 지역 {}: 위도={}, 경도={}", i+1, coords[0], coords[1]);
                    }
                    if (activeRegions.size() > 5) {
                        log.info("[BATCH-READER] ... 외 {}개 지역", activeRegions.size() - 5);
                    }
                }

                if (currentIndex >= activeRegions.size()) {
                    log.info("[BATCH-READER] 모든 활성 지역 처리 완료 - 총 {}개 처리", currentIndex);
                    return null; //읽을 데이터 더 이상 없음
                }

                double[] currentRegion = activeRegions.get(currentIndex++);
                log.debug("[BATCH-READER] 활성 지역 반환 - 인덱스: {}, 위도: {}, 경도: {}",
                    currentIndex-1, currentRegion[0], currentRegion[1]);

                return currentRegion;
            }
        };
    }


    /**
     * ItemProcessor: 각 지역의 날씨 변화 감지
     * 병렬 처리로 성능 최적화
     */
    @Bean
    @StepScope
    public ItemProcessor<double[], List<WeatherChangeDto>> weatherChangeProcessor() {
        return coordinates -> {
            try {
                double latitude = coordinates[0];
                double longitude = coordinates[1];

                log.debug("[BATCH-CHUNK] 날씨 변화 감지 시작 - 위도: {}, 경도: {}", latitude, longitude);

                List<WeatherChangeDto> changes = weatherService.detectWeatherChanges(latitude, longitude);

                if (!changes.isEmpty()) {
                    log.info("[BATCH-CHUNK] 날씨 변화 감지됨 - 위도: {}, 경도: {}, 변화 수: {}",
                        latitude, longitude, changes.size());
                }

                return changes;

            } catch (Exception e) {
                log.error("[BATCH-CHUNK] 날씨 변화 감지 실패 - 좌표: [{}, {}]",
                    coordinates[0], coordinates[1], e);
                // 실패한 항목은 빈 리스트 반환하여 처리 계속
                return List.of();
            }
        };
    }

    /**
     * ItemWriter: 감지된 변화를 이벤트로 발행
     */
    @Bean
    @StepScope
    public ItemWriter<List<WeatherChangeDto>> weatherChangeWriter() {
        return chunk -> {
            int totalChanges = 0;
            int totalNotifications = 0;

            for (List<WeatherChangeDto> changes : chunk.getItems()) {
                if (changes != null && !changes.isEmpty()) {
                    totalChanges += changes.size();

                    // 격자별로 그룹화하여 알림 전송
                    int sentNotifications = sendGridBasedNotifications(changes);
                    totalNotifications += sentNotifications;
                }
            }

            if (totalChanges > 0) {
                log.info("[BATCH-CHUNK] 청크 처리 완료 - 청크 크기: {}, 총 변화: {}건, 전송된 알림: {}건",
                    chunk.size(), totalChanges, totalNotifications);
            }
        };
    }

    /**
     * 격자별 날씨 변화 알림 전송
     */
    private int sendGridBasedNotifications(List<WeatherChangeDto> changes) {
        int notificationCount = 0;

        // 변화별로 해당 격자 사용자들에게 알림 전송
        for (WeatherChangeDto change : changes) {
            try {
                String title = "날씨 급변 알림";
                String content = formatChangeMessage(change);
                NotificationLevel level = determineNotificationLevel(change);

                // 격자별 사용자에게만 알림 전송
                notificationService.sendWeatherAlertToGridUsers(
                    change.gridX(),
                    change.gridY(),
                    title,
                    content,
                    level
                );

                notificationCount++;

                log.debug("[BATCH-NOTIFICATION] 격자({}, {}) 알림 전송 완료: {}",
                    change.gridX(), change.gridY(), change.description());

            } catch (Exception e) {
                log.error("[BATCH-NOTIFICATION] 격자({}, {}) 알림 전송 실패: {}",
                    change.gridX(), change.gridY(), change.description(), e);
            }
        }

        return notificationCount;
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
}