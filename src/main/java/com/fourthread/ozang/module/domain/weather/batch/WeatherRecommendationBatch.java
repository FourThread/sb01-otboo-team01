package com.fourthread.ozang.module.domain.weather.batch;

import com.fourthread.ozang.module.config.batch.BatchJobExecutionListener;
import com.fourthread.ozang.module.domain.clothes.repository.ClothesRepository;
import com.fourthread.ozang.module.domain.feed.entity.Feed;
import com.fourthread.ozang.module.domain.feed.repository.FeedRepository;
import com.fourthread.ozang.module.domain.weather.dto.type.PrecipitationType;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import com.fourthread.ozang.module.domain.weather.repository.WeatherRepository;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.scope.context.StepSynchronizationManager;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * =============== 새로운 배치 작업 추가 ===============
 * 날씨 기반 추천 사전 생성 배치
 * - 날씨 조건별로 인기 있는 의상 조합을 분석하여 추천 데이터 생성
 * - Redis에 캐싱하여 실시간 추천 성능 향상
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class WeatherRecommendationBatch {

    private final FeedRepository feedRepository;
    private final WeatherRepository weatherRepository;
    private final ClothesRepository clothesRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final EntityManagerFactory entityManagerFactory;
    private final BatchJobExecutionListener batchJobExecutionListener;

    @Value("${batch.weather.recommendation.chunk-size:100}")
    private int chunkSize;

    @Value("${batch.weather.recommendation.cache-ttl-hours:24}")
    private int cacheTtlHours;

    private static final String RECOMMENDATION_CACHE_PREFIX = "weather:recommendation:";

    /**
     * =============== 날씨 추천 생성 Job ===============
     */
    @Bean
    public Job weatherRecommendationJob(
        JobRepository jobRepository,
        Step analyzeWeatherPatternsStep,
        Step generateRecommendationsStep,
        Step cacheRecommendationsStep
    ) {
        return new JobBuilder("weatherRecommendationJob", jobRepository)
            .listener(batchJobExecutionListener)
            .start(analyzeWeatherPatternsStep)
            .next(generateRecommendationsStep)
            .next(cacheRecommendationsStep)
            .build();
    }

    /**
     * Step 1: 날씨 패턴 분석
     */
    @Bean
    public Step analyzeWeatherPatternsStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("analyzeWeatherPatternsStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                log.info("날씨 패턴 분석 시작");

                // 최근 30일간의 피드 데이터 분석
                LocalDateTime startDate = LocalDateTime.now().minusDays(30);
                List<Feed> recentFeeds = feedRepository.findByCreatedAtAfter(startDate);

                // 날씨 조건별 의상 착용 빈도 분석
                Map<WeatherCondition, Map<String, Integer>> weatherClothesFrequency = new HashMap<>();

                for (Feed feed : recentFeeds) {
                    Weather weather = feed.getWeather();
                    WeatherCondition condition = categorizeWeather(weather);

                    Map<String, Integer> clothesFreq = weatherClothesFrequency
                        .computeIfAbsent(condition, k -> new HashMap<>());

                    // 피드의 OOTD 의상들 카운팅
                    feed.getOotds().forEach(ootd -> {
                        String clothesType = ootd.getClothes().getType().name();
                        clothesFreq.merge(clothesType, 1, Integer::sum);
                    });
                }

                // 분석 결과를 컨텍스트에 저장
                ExecutionContext executionContext = chunkContext.getStepContext()
                    .getStepExecution()
                    .getJobExecution()
                    .getExecutionContext();

                executionContext.put("weatherClothesFrequency", weatherClothesFrequency);
                executionContext.putInt("analyzedFeedCount", recentFeeds.size());

                log.info("=============== 날씨 패턴 분석 완료 ===============");
                log.info("분석된 피드 수: {}개", recentFeeds.size());
                log.info("발견된 날씨 패턴: {}개", weatherClothesFrequency.size());

                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }

    /**
     * Step 2: 추천 데이터 생성
     */
    @Bean
    public Step generateRecommendationsStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("generateRecommendationsStep", jobRepository)
            .<WeatherCondition, WeatherRecommendation>chunk(chunkSize, transactionManager)
            .reader(weatherConditionReader())
            .processor(recommendationProcessor())
            .writer(recommendationWriter())
            .build();
    }

    /**
     * Step 3: 추천 데이터 캐싱
     */
    @Bean
    public Step cacheRecommendationsStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("cacheRecommendationsStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                log.info("추천 데이터 Redis 캐싱 시작");

                ExecutionContext jobContext = chunkContext.getStepContext()
                    .getJobExecution()
                    .getExecutionContext();

                @SuppressWarnings("unchecked")
                List<WeatherRecommendation> recommendations =
                    (List<WeatherRecommendation>) jobContext.get("generatedRecommendations");

                if (recommendations != null) {
                    int cachedCount = 0;

                    for (WeatherRecommendation recommendation : recommendations) {
                        String cacheKey = generateCacheKey(recommendation.getWeatherCondition());

                        // Redis에 캐싱
                        redisTemplate.opsForValue().set(
                            cacheKey,
                            recommendation,
                            cacheTtlHours,
                            TimeUnit.HOURS
                        );

                        cachedCount++;
                    }

                    log.info("=============== 캐싱 완료 ===============");
                    log.info("캐싱된 추천 데이터: {}개", cachedCount);
                    log.info("캐시 TTL: {}시간", cacheTtlHours);

                    jobContext.putInt("cachedRecommendationCount", cachedCount);
                }

                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }

    /**
     * ItemReader: 날씨 조건 읽기
     */
    @Bean
    @StepScope
    public ItemReader<WeatherCondition> weatherConditionReader() {
        return new ItemReader<WeatherCondition>() {
            private List<WeatherCondition> conditions = generateAllWeatherConditions();
            private int index = 0;

            @Override
            public WeatherCondition read() throws Exception {
                if (index < conditions.size()) {
                    return conditions.get(index++);
                }
                return null;
            }
        };
    }

    /**
     * ItemProcessor: 추천 생성
     */
    @Bean
    @StepScope
    public ItemProcessor<WeatherCondition, WeatherRecommendation> recommendationProcessor(
        @Value("#{jobExecutionContext['weatherClothesFrequency']}") Map<WeatherCondition, Map<String, Integer>> weatherClothesFrequency
    ) {
        return weatherCondition -> {
            log.debug("날씨 조건 {} 에 대한 추천 생성", weatherCondition);

            WeatherRecommendation recommendation = new WeatherRecommendation();
            recommendation.setWeatherCondition(weatherCondition);
            recommendation.setGeneratedAt(LocalDateTime.now());

            // 해당 날씨의 의상 빈도 데이터 조회
            Map<String, Integer> clothesFrequency = weatherClothesFrequency.get(weatherCondition);

            if (clothesFrequency != null && !clothesFrequency.isEmpty()) {
                // 빈도순으로 정렬하여 상위 10개 추천
                List<ClothesRecommendation> topRecommendations = clothesFrequency.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(10)
                    .map(entry -> {
                        ClothesRecommendation cr = new ClothesRecommendation();
                        cr.setClothesType(entry.getKey());
                        cr.setFrequency(entry.getValue());
                        cr.setScore(calculateRecommendationScore(entry.getValue(), clothesFrequency.size()));
                        return cr;
                    })
                    .collect(Collectors.toList());

                recommendation.setRecommendations(topRecommendations);

                // 추가 메타데이터
                recommendation.setTotalSampleSize(clothesFrequency.values().stream()
                    .mapToInt(Integer::intValue).sum());
                recommendation.setConfidenceLevel(calculateConfidenceLevel(recommendation.getTotalSampleSize()));
            } else {
                // 데이터가 없는 경우 기본 추천
                recommendation.setRecommendations(generateDefaultRecommendations(weatherCondition));
                recommendation.setTotalSampleSize(0);
                recommendation.setConfidenceLevel("LOW");
            }

            return recommendation;
        };
    }

    /**
     * ItemWriter: 추천 저장
     */
    @Bean
    @StepScope
    public ItemWriter<WeatherRecommendation> recommendationWriter() {
        return chunk -> {
            List<WeatherRecommendation> recommendations = new ArrayList<>();

            for (WeatherRecommendation recommendation : chunk) {
                recommendations.add(recommendation);

                log.info("=============== 추천 생성 완료 ===============");
                log.info("날씨 조건: {}", recommendation.getWeatherCondition());
                log.info("추천 의상 수: {}개", recommendation.getRecommendations().size());
                log.info("신뢰도: {}", recommendation.getConfidenceLevel());
            }

            // Job 컨텍스트에 저장
            StepExecution stepExecution = StepSynchronizationManager.getContext().getStepExecution();
            ExecutionContext jobContext = stepExecution.getJobExecution().getExecutionContext();

            @SuppressWarnings("unchecked")
            List<WeatherRecommendation> existingRecommendations =
                (List<WeatherRecommendation>) jobContext.get("generatedRecommendations");

            if (existingRecommendations == null) {
                existingRecommendations = new ArrayList<>();
            }
            existingRecommendations.addAll(recommendations);

            jobContext.put("generatedRecommendations", existingRecommendations);
        };
    }

    private WeatherCondition categorizeWeather(Weather weather) {
        WeatherCondition condition = new WeatherCondition();

        // 온도 범위 설정
        double temp = weather.getTemperature().current();
        if (temp < 5) {
            condition.setTemperatureRange("VERY_COLD");
        } else if (temp < 10) {
            condition.setTemperatureRange("COLD");
        } else if (temp < 20) {
            condition.setTemperatureRange("MILD");
        } else if (temp < 28) {
            condition.setTemperatureRange("WARM");
        } else {
            condition.setTemperatureRange("HOT");
        }

        // 날씨 상태
        condition.setSkyStatus(weather.getSkyStatus().name());

        // 강수 여부
        condition.setPrecipitation(weather.getPrecipitation().type() != PrecipitationType.NONE);

        // 계절
        int month = LocalDateTime.now().getMonthValue();
        if (month >= 3 && month <= 5) {
            condition.setSeason("SPRING");
        } else if (month >= 6 && month <= 8) {
            condition.setSeason("SUMMER");
        } else if (month >= 9 && month <= 11) {
            condition.setSeason("FALL");
        } else {
            condition.setSeason("WINTER");
        }

        return condition;
    }

    private List<WeatherCondition> generateAllWeatherConditions() {
        List<WeatherCondition> conditions = new ArrayList<>();

        String[] tempRanges = {"VERY_COLD", "COLD", "MILD", "WARM", "HOT"};
        String[] skyStatuses = {"CLEAR", "MOSTLY_CLOUDY", "CLOUDY"};
        String[] seasons = {"SPRING", "SUMMER", "FALL", "WINTER"};
        boolean[] precipitations = {true, false};

        for (String temp : tempRanges) {
            for (String sky : skyStatuses) {
                for (String season : seasons) {
                    for (boolean precip : precipitations) {
                        WeatherCondition condition = new WeatherCondition();
                        condition.setTemperatureRange(temp);
                        condition.setSkyStatus(sky);
                        condition.setSeason(season);
                        condition.setPrecipitation(precip);
                        conditions.add(condition);
                    }
                }
            }
        }

        return conditions;
    }

    private double calculateRecommendationScore(int frequency, int totalTypes) {
        return (double) frequency / totalTypes * 100;
    }

    private String calculateConfidenceLevel(int sampleSize) {
        if (sampleSize >= 100) return "HIGH";
        if (sampleSize >= 50) return "MEDIUM";
        return "LOW";
    }

    private List<ClothesRecommendation> generateDefaultRecommendations(WeatherCondition condition) {
        List<ClothesRecommendation> defaults = new ArrayList<>();

        // 온도에 따른 기본 추천
        if ("VERY_COLD".equals(condition.getTemperatureRange()) ||
            "COLD".equals(condition.getTemperatureRange())) {
            defaults.add(createRecommendation("OUTER", 90));
            defaults.add(createRecommendation("TOP", 85));
            defaults.add(createRecommendation("BOTTOM", 85));
            defaults.add(createRecommendation("SCARF", 70));
        } else if ("HOT".equals(condition.getTemperatureRange())) {
            defaults.add(createRecommendation("TOP", 90));
            defaults.add(createRecommendation("BOTTOM", 85));
            defaults.add(createRecommendation("HAT", 60));
        }

        // 강수 시 추가
        if (condition.isPrecipitation()) {
            defaults.add(createRecommendation("SHOES", 80)); // 방수 신발
            defaults.add(createRecommendation("BAG", 70)); // 방수 가방
        }

        return defaults;
    }

    private ClothesRecommendation createRecommendation(String type, double score) {
        ClothesRecommendation rec = new ClothesRecommendation();
        rec.setClothesType(type);
        rec.setScore(score);
        rec.setFrequency(0); // 기본값
        return rec;
    }

    private String generateCacheKey(WeatherCondition condition) {
        return RECOMMENDATION_CACHE_PREFIX +
            condition.getTemperatureRange() + ":" +
            condition.getSkyStatus() + ":" +
            condition.getSeason() + ":" +
            condition.isPrecipitation();
    }

    /**
     * =============== DTO 클래스 정의 ===============
     */
    private static class WeatherCondition {
        private String temperatureRange;
        private String skyStatus;
        private String season;
        private boolean precipitation;

        // Getters and Setters
        public String getTemperatureRange() { return temperatureRange; }
        public void setTemperatureRange(String temperatureRange) { this.temperatureRange = temperatureRange; }

        public String getSkyStatus() { return skyStatus; }
        public void setSkyStatus(String skyStatus) { this.skyStatus = skyStatus; }

        public String getSeason() { return season; }
        public void setSeason(String season) { this.season = season; }

        public boolean isPrecipitation() { return precipitation; }
        public void setPrecipitation(boolean precipitation) { this.precipitation = precipitation; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            WeatherCondition that = (WeatherCondition) o;
            return precipitation == that.precipitation &&
                Objects.equals(temperatureRange, that.temperatureRange) &&
                Objects.equals(skyStatus, that.skyStatus) &&
                Objects.equals(season, that.season);
        }

        @Override
        public int hashCode() {
            return Objects.hash(temperatureRange, skyStatus, season, precipitation);
        }

        @Override
        public String toString() {
            return String.format("%s_%s_%s_%s",
                temperatureRange, skyStatus, season, precipitation ? "RAIN" : "CLEAR");
        }
    }

    private static class WeatherRecommendation {
        private WeatherCondition weatherCondition;
        private List<ClothesRecommendation> recommendations;
        private LocalDateTime generatedAt;
        private int totalSampleSize;
        private String confidenceLevel;

        // Getters and Setters
        public WeatherCondition getWeatherCondition() { return weatherCondition; }
        public void setWeatherCondition(WeatherCondition weatherCondition) { this.weatherCondition = weatherCondition; }

        public List<ClothesRecommendation> getRecommendations() { return recommendations; }
        public void setRecommendations(List<ClothesRecommendation> recommendations) { this.recommendations = recommendations; }

        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

        public int getTotalSampleSize() { return totalSampleSize; }
        public void setTotalSampleSize(int totalSampleSize) { this.totalSampleSize = totalSampleSize; }

        public String getConfidenceLevel() { return confidenceLevel; }
        public void setConfidenceLevel(String confidenceLevel) { this.confidenceLevel = confidenceLevel; }
    }

    private static class ClothesRecommendation {
        private String clothesType;
        private int frequency;
        private double score;

        public String getClothesType() { return clothesType; }
        public void setClothesType(String clothesType) { this.clothesType = clothesType; }

        public int getFrequency() { return frequency; }
        public void setFrequency(int frequency) { this.frequency = frequency; }

        public double getScore() { return score; }
        public void setScore(double score) { this.score = score; }
    }
}