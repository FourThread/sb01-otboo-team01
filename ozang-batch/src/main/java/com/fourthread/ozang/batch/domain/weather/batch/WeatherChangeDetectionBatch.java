package com.fourthread.ozang.batch.domain.weather.batch;

import com.fourthread.ozang.batch.config.BatchJobExecutionListener;
import com.fourthread.ozang.core.domain.notification.entity.NotificationLevel;
import com.fourthread.ozang.core.domain.notification.event.WeatherChangeDetectedEvent;
import com.fourthread.ozang.core.domain.weather.dto.WeatherChangeDto;
import com.fourthread.ozang.core.domain.weather.service.WeatherCacheService;
import com.fourthread.ozang.core.domain.weather.service.WeatherService;
import java.util.List;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ApplicationEventPublisher eventPublisher;
    private final Executor apiCallExecutor;
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
        PlatformTransactionManager transactionManager,
        TaskExecutor batchTaskExecutor) {
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
     * @return
     */
    @Bean
    public ItemReader<double[]> activeRegionsReader() {
        return new ItemReader<double[]>() {
            private List<double[]> activeRegions;
            private int currentIndex = 0;

            @Override
            public double[] read()
                throws ParseException, NonTransientResourceException {

                if (activeRegions == null) {
                    activeRegions = cacheService.getAllActiveRegions();
                    log.info("[BATCH-CHUNK] 활성 지역 {}개 로드 완료", activeRegions.size());

                    if (activeRegions.isEmpty()) {
                        log.info("[BATCH-CHUNK] 활성 지역이 없어 처리를 종료합니다");
                        return null;
                    }
                }

                if (currentIndex >= activeRegions.size()) {
                    return null; //읽을 데이터 더 이상 없음
                }

                return activeRegions.get(currentIndex++);
            }
        };
    }


    /**
     * ItemProcessor: 각 지역의 날씨 변화 감지
     * 병렬 처리로 성능 최적화
     */
    @Bean
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
     * 배치 단위로 효율적 처리
     */
    @Bean
    public ItemWriter<List<WeatherChangeDto>> weatherChangeWriter() {
        return chunk -> {
            int totalChanges = 0;

            for (List<WeatherChangeDto> changes : chunk.getItems()) {
                if (changes != null && !changes.isEmpty()) {
                    totalChanges += changes.size();
                    sendWeatherChangeEvents(changes);
                }
            }

            if (totalChanges > 0) {
                log.info("[BATCH-CHUNK] 청크 처리 완료 - 청크 크기: {}, 총 변화: {}건",
                    chunk.size(), totalChanges);
            }
        };
    }


    /**
     * 날씨 변화 이벤트 발행
     */
    private void sendWeatherChangeEvents(List<WeatherChangeDto> changes) {
        for (WeatherChangeDto change : changes) {
            String title = "날씨 급변 알림";
            String content = formatChangeMessage(change);
            NotificationLevel level = determineNotificationLevel(change);

            WeatherChangeDetectedEvent event = new WeatherChangeDetectedEvent(
                    getLocationDescription(change),
                    title,
                    content,
                    level
            );

            eventPublisher.publishEvent(event);
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