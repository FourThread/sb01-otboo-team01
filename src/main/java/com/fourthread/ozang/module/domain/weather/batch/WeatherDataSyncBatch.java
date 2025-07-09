package com.fourthread.ozang.module.domain.weather.batch;

import com.fourthread.ozang.module.config.batch.BatchJobExecutionListener;
import com.fourthread.ozang.module.domain.weather.client.WeatherApiClient;
import com.fourthread.ozang.module.domain.weather.dto.external.WeatherApiResponse;
import com.fourthread.ozang.module.domain.weather.entity.GridCoordinate;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import com.fourthread.ozang.module.domain.weather.exception.WeatherApiException;
import com.fourthread.ozang.module.domain.weather.mapper.WeatherMapper;
import com.fourthread.ozang.module.domain.weather.repository.WeatherRepository;
import com.fourthread.ozang.module.domain.weather.util.CoordinateConverter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 날씨 데이터 동기화 배치
 * - 주요 도시의 날씨 데이터를 주기적으로 동기화
 * - 외부 API 호출을 최적화하여 효율적으로 처리
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WeatherDataSyncBatch {

    private final WeatherApiClient weatherApiClient;
    private final WeatherRepository weatherRepository;
    private final WeatherMapper weatherMapper;
    private final CoordinateConverter coordinateConverter;
    private final BatchJobExecutionListener batchJobExecutionListener;

    @Value("${batch.weather.sync.chunk-size:10}")
    private int chunkSize;

    private static final List<CityCoordinate> MAJOR_CITIES = Arrays.asList(
        new CityCoordinate("서울", 126.9780, 37.5665),
        new CityCoordinate("부산", 129.0756, 35.1796),
        new CityCoordinate("대구", 128.6014, 35.8714),
        new CityCoordinate("인천", 126.7052, 37.4563),
        new CityCoordinate("광주", 126.8526, 35.1595),
        new CityCoordinate("대전", 127.3845, 36.3504),
        new CityCoordinate("울산", 129.3114, 35.5384),
        new CityCoordinate("세종", 127.2898, 36.4804),
        new CityCoordinate("수원", 127.0286, 37.2636),
        new CityCoordinate("성남", 127.1260, 37.4201),
        new CityCoordinate("고양", 126.8320, 37.6584),
        new CityCoordinate("용인", 127.1775, 37.2411),
        new CityCoordinate("창원", 128.6811, 35.2280),
        new CityCoordinate("청주", 127.4897, 36.6424),
        new CityCoordinate("전주", 127.1480, 35.8242),
        new CityCoordinate("천안", 127.1521, 36.8151),
        new CityCoordinate("안산", 126.8309, 37.3219),
        new CityCoordinate("안양", 126.9568, 37.3943),
        new CityCoordinate("남양주", 127.2166, 37.6360),
        new CityCoordinate("제주", 126.5312, 33.4996)
    );

    /**
     *  날씨 데이터 동기화 Job
     */
    @Bean
    public Job weatherDataSyncJob(
        JobRepository jobRepository,
        Step weatherDataSyncStep,
        Step weatherDataValidationStep,
        Step weatherDataAggregationStep
    ) {
        return new JobBuilder("weatherDataSyncJob", jobRepository)
            .listener(batchJobExecutionListener)
            .start(weatherDataSyncStep)
            .next(weatherDataValidationStep)
            .next(weatherDataAggregationStep)
            .build();
    }

    @Bean
    public Step weatherDataSyncStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("weatherDataSyncStep", jobRepository)
            .<CityCoordinate, WeatherSyncResult>chunk(chunkSize, transactionManager)
            .reader(cityCoordinateReader())
            .processor(weatherDataProcessor())
            .writer(weatherDataWriter())
            .faultTolerant()
            .retryLimit(3)
            .retry(WeatherApiException.class)
            .skipLimit(5)
            .skip(WeatherApiException.class)
            .listener(new WeatherSyncStepListener())
            .build();
    }

    /**
     * 동기화된 데이터 검증
     */
    @Bean
    public Step weatherDataValidationStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("weatherDataValidationStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                log.info("날씨 데이터 검증 시작");

                Map<String, Object> jobContext = chunkContext.getStepContext()
                    .getJobExecutionContext();

                int totalProcessed = (int) jobContext.getOrDefault("totalProcessed", 0);
                int totalSuccess = (int) jobContext.getOrDefault("totalSuccess", 0);
                int totalFailed = (int) jobContext.getOrDefault("totalFailed", 0);

                double successRate = totalProcessed > 0
                    ? (double) totalSuccess / totalProcessed * 100
                    : 0;

                log.info("=============== 동기화 결과 ===============");
                log.info("전체 처리: {}건", totalProcessed);
                log.info("성공: {}건", totalSuccess);
                log.info("실패: {}건", totalFailed);
                log.info("성공률: {:.2f}%", successRate);

                if (successRate < 50) {
                    throw new RuntimeException("동기화 성공률이 50% 미만입니다.");
                }

                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }

    @Bean
    public Step weatherDataAggregationStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("weatherDataAggregationStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                log.info("날씨 통계 집계 시작");

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime startDate = now.minusDays(7);

                List<Weather> recentWeathers = weatherRepository
                    .findWeatherDataBetweenDates(startDate, now);

                // 지역별 평균 온도 계산
                Map<String, Double> avgTempByLocation = new HashMap<>();
                Map<String, Integer> countByLocation = new HashMap<>();

                for (Weather weather : recentWeathers) {
                    String location = weather.getLocation().locationNames().get(0);
                    double temp = weather.getTemperature().current();

                    avgTempByLocation.merge(location, temp, Double::sum);
                    countByLocation.merge(location, 1, Integer::sum);
                }

                // 평균 계산
                avgTempByLocation.replaceAll((location, sum) ->
                    sum / countByLocation.get(location));

                log.info("=============== 지역별 평균 온도 (최근 7일) ===============");
                avgTempByLocation.forEach((location, avgTemp) ->
                    log.info("{}: {:.1f}°C", location, avgTemp));

                // 결과를 컨텍스트에 저장
                chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext()
                    .put("weatherStatistics", avgTempByLocation);

                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }


    @Bean
    @StepScope
    public ItemReader<CityCoordinate> cityCoordinateReader() {
        return new ListItemReader<>(MAJOR_CITIES);
    }

    @Bean
    @StepScope
    public ItemProcessor<CityCoordinate, WeatherSyncResult> weatherDataProcessor() {
        return new ItemProcessor<CityCoordinate, WeatherSyncResult>() {
            @Override
            public WeatherSyncResult process(CityCoordinate city) throws Exception {
                log.info("{}의 날씨 데이터 동기화 시작", city.getName());

                WeatherSyncResult result = new WeatherSyncResult();
                result.setCityName(city.getName());
                result.setLongitude(city.getLongitude());
                result.setLatitude(city.getLatitude());

                try {
                    // 격자 좌표 변환
                    GridCoordinate grid = coordinateConverter.convertToGrid(
                        city.getLatitude(), city.getLongitude()
                    );

                    // 현재 시간 기준으로 API 호출
                    LocalDateTime now = LocalDateTime.now();
                    String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                    String baseTime = getValidBaseTime(now);

                    // API 호출
                    WeatherApiResponse response = weatherApiClient.getVillageForecast(
                        baseDate, baseTime, grid.getX(), grid.getY()
                    );

                    // 응답 파싱 및 Weather 엔티티 생성
                    List<Weather> weathers = parseApiResponse(response, city);

                    result.setWeathers(weathers);
                    result.setSuccess(true);
                    result.setProcessedAt(LocalDateTime.now());

                    log.info("{} 날씨 데이터 동기화 성공: {}건", city.getName(), weathers.size());

                } catch (Exception e) {
                    log.error("{} 날씨 데이터 동기화 실패", city.getName(), e);
                    result.setSuccess(false);
                    result.setErrorMessage(e.getMessage());
                }

                return result;
            }

            private String getValidBaseTime(LocalDateTime now) {
                int hour = now.getHour();
                int minute = now.getMinute();

                // API 제공 시간: 02, 05, 08, 11, 14, 17, 20, 23시
                int[] baseTimes = {2, 5, 8, 11, 14, 17, 20, 23};

                for (int i = baseTimes.length - 1; i >= 0; i--) {
                    if (hour > baseTimes[i] || (hour == baseTimes[i] && minute >= 10)) {
                        return String.format("%02d00", baseTimes[i]);
                    }
                }

                // 전날 23시 데이터 사용
                return "2300";
            }

            private List<Weather> parseApiResponse(WeatherApiResponse response, CityCoordinate city) {
                // WeatherMapper를 활용하여 API 응답을 Weather 엔티티로 변환
                // 실제 구현은 기존 WeatherService의 로직을 참고
                List<Weather> weathers = new ArrayList<>();

                // TODO: 실제 파싱 로직 구현
                // weatherMapper를 사용하여 변환

                return weathers;
            }
        };
    }

    @Bean
    @StepScope
    public ItemWriter<WeatherSyncResult> weatherDataWriter() {
        return chunk -> {
            for (WeatherSyncResult result : chunk) {
                if (result.isSuccess() && result.getWeathers() != null) {
                    try {
                        // 중복 체크 및 저장
                        for (Weather weather : result.getWeathers()) {
                            if (!weatherRepository.existsByApiResponseHash(weather.getApiResponseHash())) {
                                weatherRepository.save(weather);
                            }
                        }
                        log.info("{} 날씨 데이터 저장 완료", result.getCityName());
                    } catch (Exception e) {
                        log.error("{} 날씨 데이터 저장 실패", result.getCityName(), e);
                    }
                }
            }
        };
    }

    /**
     * 동기화 통계 수집
     */
    private static class WeatherSyncStepListener implements StepExecutionListener {
        private final Map<String, Integer> syncStats = new ConcurrentHashMap<>();

        @Override
        public void beforeStep(StepExecution stepExecution) {
            log.info("날씨 데이터 동기화 Step 시작");
            syncStats.clear();
        }

        @Override
        public ExitStatus afterStep(StepExecution stepExecution) {
            log.info("날씨 데이터 동기화 Step 완료");

            // 통계를 Job 컨텍스트에 저장
            ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();
            jobContext.putInt("totalProcessed", stepExecution.getReadCount());
            jobContext.putInt("totalSuccess", stepExecution.getWriteCount());
            jobContext.putInt("totalFailed", stepExecution.getProcessSkipCount());

            return stepExecution.getExitStatus();
        }
    }

    private static class CityCoordinate {
        private final String name;
        private final double longitude;
        private final double latitude;

        public CityCoordinate(String name, double longitude, double latitude) {
            this.name = name;
            this.longitude = longitude;
            this.latitude = latitude;
        }

        // Getters
        public String getName() { return name; }
        public double getLongitude() { return longitude; }
        public double getLatitude() { return latitude; }
    }

    private static class WeatherSyncResult {
        private String cityName;
        private double longitude;
        private double latitude;
        private List<Weather> weathers;
        private boolean success;
        private String errorMessage;
        private LocalDateTime processedAt;

        // Getters and Setters
        public String getCityName() { return cityName; }
        public void setCityName(String cityName) { this.cityName = cityName; }

        public double getLongitude() { return longitude; }
        public void setLongitude(double longitude) { this.longitude = longitude; }

        public double getLatitude() { return latitude; }
        public void setLatitude(double latitude) { this.latitude = latitude; }

        public List<Weather> getWeathers() { return weathers; }
        public void setWeathers(List<Weather> weathers) { this.weathers = weathers; }

        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public LocalDateTime getProcessedAt() { return processedAt; }
        public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
    }
}