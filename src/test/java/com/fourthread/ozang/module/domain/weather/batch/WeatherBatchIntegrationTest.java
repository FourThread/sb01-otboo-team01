package com.fourthread.ozang.module.domain.weather.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fourthread.ozang.module.domain.weather.entity.Weather;
import com.fourthread.ozang.module.domain.weather.repository.WeatherRepository;
import com.fourthread.ozang.module.domain.weather.service.WeatherService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * =============== 날씨 배치 통합 테스트 ===============
 */
@Slf4j
@SpringBootTest
@SpringBatchTest
@ActiveProfiles("test")
@DisplayName("날씨 배치 작업 통합 테스트")
class WeatherBatchIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private JobRepositoryTestUtils jobRepositoryTestUtils;

    @Autowired
    private WeatherRepository weatherRepository;

    @MockitoBean
    private WeatherService weatherService;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private Job weatherDataCleanupJob;

    @Autowired
    private Job weatherDataSyncJob;

    @Autowired
    private Job weatherRecommendationJob;

    @Autowired
    private Job weatherAlertJob;

    @BeforeEach
    void setUp() {
        jobRepositoryTestUtils.removeJobExecutions();
        weatherRepository.deleteAll();
    }

    /**
     * =============== 날씨 데이터 정리 배치 테스트 ===============
     */
    @Test
    @DisplayName("오래된 날씨 데이터 정리 배치가 정상적으로 실행된다")
    void testWeatherDataCleanupJob() throws Exception {
        // Given
        int oldDataCount = 10;
        when(weatherService.cleanupOldWeatherData()).thenReturn(oldDataCount);

        jobLauncherTestUtils.setJob(weatherDataCleanupJob);

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(jobExecution.getExitStatus().getExitCode()).isEqualTo("COMPLETED");

        // ExecutionContext 검증
        ExecutionContext executionContext = jobExecution.getExecutionContext();
        assertThat(executionContext.getInt("deletedWeatherCount")).isEqualTo(oldDataCount);

        verify(weatherService, times(1)).cleanupOldWeatherData();
    }

    /**
     * =============== 날씨 데이터 동기화 배치 테스트 ===============
     */
    @Test
    @DisplayName("날씨 데이터 동기화 배치가 정상적으로 실행된다")
    @Transactional
    void testWeatherDataSyncJob() throws Exception {
        // Given
        List<Weather> mockWeathers = createMockWeatherList(5);
        when(weatherService.syncWeatherData(anyDouble(), anyDouble())).thenReturn(mockWeathers);

        jobLauncherTestUtils.setJob(weatherDataSyncJob);

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Step 실행 검증
        Collection<StepExecution> stepExecutions = jobExecution.getStepExecutions();
        assertThat(stepExecutions).hasSize(3); // sync, validation, aggregation

        // 각 Step 상태 검증
        stepExecutions.forEach(stepExecution -> {
            assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        });
    }

    /**
     * =============== 날씨 추천 생성 배치 테스트 ===============
     */
    @Test
    @DisplayName("날씨 추천 생성 배치가 정상적으로 실행된다")
    void testWeatherRecommendationJob() throws Exception {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(mock(org.springframework.data.redis.core.ValueOperations.class));

        jobLauncherTestUtils.setJob(weatherRecommendationJob);

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Step 실행 검증
        StepExecution analyzeStep = getStepExecution(jobExecution, "analyzeWeatherPatternsStep");
        assertThat(analyzeStep).isNotNull();
        assertThat(analyzeStep.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    /**
     * =============== 날씨 알림 발송 배치 테스트 ===============
     */
    @Test
    @DisplayName("날씨 알림 발송 배치가 정상적으로 실행된다")
    void testWeatherAlertJob() throws Exception {
        // Given
        List<WeatherService.ExtremeWeatherInfo> extremeWeathers = createMockExtremeWeathers();
        when(weatherService.detectExtremeWeatherConditions(any(), any())).thenReturn(extremeWeathers);

        jobLauncherTestUtils.setJob(weatherAlertJob);

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // ExecutionContext 검증
        ExecutionContext executionContext = jobExecution.getExecutionContext();
        assertThat(executionContext.containsKey("alertCount")).isTrue();
    }

    /**
     *  Step 단위 테스트
     */
    @Test
    @DisplayName("날씨 데이터 동기화 Step만 실행한다")
    void testWeatherDataSyncStep() throws Exception {
        // Given
        jobLauncherTestUtils.setJob(weatherDataSyncJob);

        // When
        JobExecution jobExecution = jobLauncherTestUtils.launchStep("weatherDataSyncStep");

        // Then
        StepExecution stepExecution = jobExecution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(stepExecution.getReadCount()).isGreaterThan(0);
    }

    /**
     *  배치 재시작 테스트
     */
    @Test
    @DisplayName("실패한 배치 작업을 재시작할 수 있다")
    void testJobRestart() throws Exception {
        // Given
        jobLauncherTestUtils.setJob(weatherDataSyncJob);

        // 첫 번째 실행 시 실패하도록 설정
        when(weatherService.syncWeatherData(anyDouble(), anyDouble()))
            .thenThrow(new RuntimeException("API 호출 실패"))
            .thenReturn(createMockWeatherList(3));

        // When - 첫 번째 실행 (실패)
        JobParameters jobParameters = new JobParametersBuilder()
            .addLong("timestamp", System.currentTimeMillis())
            .toJobParameters();

        JobExecution firstExecution = jobLauncherTestUtils.launchJob(jobParameters);
        assertThat(firstExecution.getStatus()).isEqualTo(BatchStatus.FAILED);

        // When - 재시작
        JobExecution restartExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // Then
        assertThat(restartExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    /**
     * =============== 성능 테스트 ===============
     */
    @Test
    @DisplayName("대량 데이터 처리 성능을 측정한다")
    void testBatchPerformance() throws Exception {
        // Given
        int dataSize = 1000;
        List<Weather> largeDataSet = createMockWeatherList(dataSize);
        when(weatherService.syncWeatherData(anyDouble(), anyDouble())).thenReturn(largeDataSet);

        jobLauncherTestUtils.setJob(weatherDataSyncJob);

        // When
        long startTime = System.currentTimeMillis();
        JobExecution jobExecution = jobLauncherTestUtils.launchJob();
        long endTime = System.currentTimeMillis();

        // Then
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        long executionTime = endTime - startTime;
        log.info("배치 실행 시간: {}ms", executionTime);

        // 성능 기준: 1000건 처리에 10초 이내
        assertThat(executionTime).isLessThan(10000);
    }

    /**
     * =============== 헬퍼 메서드 ===============
     */
    private List<Weather> createMockWeatherList(int count) {
        List<Weather> weathers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Weather weather = Weather.builder()
                .forecastedAt(LocalDateTime.now())
                .forecastAt(LocalDateTime.now().plusHours(i))
                .build();
            weathers.add(weather);
        }
        return weathers;
    }

    private List<WeatherService.ExtremeWeatherInfo> createMockExtremeWeathers() {
        List<WeatherService.ExtremeWeatherInfo> extremeWeathers = new ArrayList<>();

        WeatherService.ExtremeWeatherInfo heatWave = new WeatherService.ExtremeWeatherInfo();
        heatWave.setAlertType("HEAT_WAVE");
        heatWave.setSeverity("WARNING");
        heatWave.setLocation("서울");
        heatWave.setAlertTime(LocalDateTime.now().plusDays(1));
        extremeWeathers.add(heatWave);

        return extremeWeathers;
    }

    private StepExecution getStepExecution(JobExecution jobExecution, String stepName) {
        return jobExecution.getStepExecutions().stream()
            .filter(step -> step.getStepName().equals(stepName))
            .findFirst()
            .orElse(null);
    }
}