package com.fourthread.ozang.module.domain.weather.service;

import com.fourthread.ozang.module.domain.weather.client.KakaoApiClient;
import com.fourthread.ozang.module.domain.weather.client.WeatherApiClient;
import com.fourthread.ozang.module.domain.weather.dto.HumidityDto;
import com.fourthread.ozang.module.domain.weather.dto.PrecipitationDto;
import com.fourthread.ozang.module.domain.weather.dto.TemperatureDto;
import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.module.domain.weather.dto.WindSpeedDto;
import com.fourthread.ozang.module.domain.weather.dto.external.WeatherApiResponse;
import com.fourthread.ozang.module.domain.weather.dto.type.PrecipitationType;
import com.fourthread.ozang.module.domain.weather.dto.type.SkyStatus;
import com.fourthread.ozang.module.domain.weather.dto.type.WindStrength;
import com.fourthread.ozang.module.domain.weather.entity.GridCoordinate;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import com.fourthread.ozang.module.domain.weather.exception.InvalidCoordinateException;
import com.fourthread.ozang.module.domain.weather.exception.WeatherApiException;
import com.fourthread.ozang.module.domain.weather.exception.WeatherDataFetchException;
import com.fourthread.ozang.module.domain.weather.mapper.WeatherMapper;
import com.fourthread.ozang.module.domain.weather.repository.WeatherRepository;
import com.fourthread.ozang.module.domain.weather.util.CoordinateConverter;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class WeatherServiceImpl implements WeatherService {

    private final WeatherRepository weatherRepository;
    private final WeatherMapper weatherMapper;
    private final WeatherApiClient weatherApiClient;
    private final KakaoApiClient kakaoApiClient;
    private final CoordinateConverter coordinateConverter;
    private final Executor apiCallExecutor;

    private final WeatherCacheService cacheService;

    @Value("${batch.weather.retention-days:30}")
    private int defaultRetentionDays;

    public WeatherServiceImpl(
        WeatherRepository weatherRepository,
        WeatherMapper weatherMapper,
        WeatherApiClient weatherApiClient,
        KakaoApiClient kakaoApiClient,
        CoordinateConverter coordinateConverter,
        @Qualifier("apiCallExecutor") Executor apiCallExecutor,
        WeatherCacheService cacheService) {

        this.weatherRepository = weatherRepository;
        this.weatherMapper = weatherMapper;
        this.weatherApiClient = weatherApiClient;
        this.kakaoApiClient = kakaoApiClient;
        this.coordinateConverter = coordinateConverter;
        this.apiCallExecutor = apiCallExecutor;
        this.cacheService = cacheService;
    }

    @Override
    @Transactional(readOnly = true)
    public WeatherDto getWeatherForecast(Double longitude, Double latitude) {
        log.info("날씨 정보 조회 시작 - 위도: {}, 경도: {}", latitude, longitude);

        validateCoordinates(longitude, latitude);

        // 1단계: Redis 캐시 확인
        WeatherDto cachedWeather = cacheService.getCurrentWeatherFromCache(latitude, longitude);
        if (cachedWeather != null) {
            log.info("Redis 캐시에서 날씨 정보 반환 - 응답시간: <100ms");
            return cachedWeather;
        }

        // 격자 좌표 변환
        GridCoordinate gridCoordinate = coordinateConverter.convertToGrid(latitude, longitude);
        log.debug("격자 좌표 변환 완료 - X: {}, Y: {}", gridCoordinate.getX(), gridCoordinate.getY());

        // 2단계: DB 캐시 확인
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Optional<Weather> dbWeather = weatherRepository.findLatestByGridCoordinate(
            gridCoordinate.getX(),
            gridCoordinate.getY()
        );

        if (dbWeather.isPresent() && dbWeather.get().getForecastedAt().isAfter(oneHourAgo)) {
            log.info("DB 캐시에서 날씨 정보 반환");
            WeatherDto weatherDto = weatherMapper.toDto(dbWeather.get());

            // Redis에 다시 캐싱
            cacheService.cacheCurrentWeather(latitude, longitude, weatherDto);

            return weatherDto;
        }

        // 3단계: 외부 API 호출
        LocalDateTime now = LocalDateTime.now();
        String baseDate = calculateBaseDate(now);
        String baseTime = calculateBaseTime(now);
        log.debug("기상청 API 요청 시간 - base_date: {}, base_time: {}", baseDate, baseTime);

        Weather weather = fetchAndSaveWeatherData(latitude, longitude, gridCoordinate);
        WeatherDto weatherDto = weatherMapper.toDto(weather);

        // Redis에 캐싱
        cacheService.cacheCurrentWeather(latitude, longitude, weatherDto);

        log.info("날씨 정보 조회 완료");
        return weatherDto;
    }

    @Override
    @Transactional
    public List<WeatherDto> getFiveDayForecast(Double longitude, Double latitude) {
        validateCoordinates(longitude, latitude);

        // 1단계: Redis 캐시 확인
        LocalDateTime nowKst = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        String baseTime = calculateBaseTime(nowKst);

        List<WeatherDto> cachedForecast = cacheService.getForecastFromCache(latitude, longitude, baseTime);
        if (cachedForecast != null && !cachedForecast.isEmpty()) {
            log.info("Redis 캐시에서 5일 예보 반환 - 응답시간: <100ms");
            return cachedForecast;
        }


        // 3단계: 외부 API 호출
        GridCoordinate grid = coordinateConverter.convertToGrid(latitude, longitude);
        String baseDate = calculateBaseDate(nowKst);
        log.debug("기상청 단기예보 호출 기준시각 - date: {}, time: {}", baseDate, baseTime);

        log.info("5일 예보 병렬 호출 시작");
        long startTime = System.currentTimeMillis();

        CompletableFuture<WeatherApiResponse> weatherApiFuture = CompletableFuture
            .supplyAsync(() -> {
                long apiStartTime = System.currentTimeMillis();
                log.debug("기상청 단기예보 API 호출 시작");
                WeatherApiResponse response = weatherApiClient.callVilageFcst(grid, baseDate, baseTime);
                long apiEndTime = System.currentTimeMillis();
                log.debug("기상청 단기예보 API 호출 완료 - 소요시간: {}ms", apiEndTime - apiStartTime);
                return response;
            }, apiCallExecutor)
            .orTimeout(15, TimeUnit.SECONDS);

        CompletableFuture<List<String>> locationFuture = CompletableFuture
            .supplyAsync(() -> {
                // 위치 정보도 캐시 확인
                WeatherAPILocation cachedLocation = cacheService.getLocationFromCache(latitude, longitude);
                if (cachedLocation != null) {
                    return cachedLocation.locationNames();
                }

                List<String> locationNames = kakaoApiClient.getLocationNames(latitude, longitude);

                // 위치 정보 캐싱
                WeatherAPILocation location = weatherMapper.toWeatherAPILocation(
                    latitude, longitude, grid.getX(), grid.getY(), locationNames
                );
                cacheService.cacheLocation(latitude, longitude, location);

                return locationNames;
            }, apiCallExecutor)
            .orTimeout(5, TimeUnit.SECONDS);

        try {
            CompletableFuture.allOf(weatherApiFuture, locationFuture).join();

            WeatherApiResponse apiResponse = weatherApiFuture.get();
            List<String> locationNames = locationFuture.get();

            long endTime = System.currentTimeMillis();
            log.info("외부 API 병렬 호출 완료 - 소요시간: {}ms", endTime - startTime);

            validateApiResponse(apiResponse);

            List<WeatherDto> forecast = processFiveDayForecast(apiResponse, latitude, longitude, grid, locationNames);

            // Redis에 예보 캐싱
            cacheService.cacheForecast(latitude, longitude, baseTime, forecast);

            return forecast;

        } catch (Exception e) {
            log.error("5일 예보 조회 실패", e);
            throw new WeatherDataFetchException("5일 예보 조회 중 오류 발생", e);
        }
    }

    @Override
    public WeatherAPILocation getWeatherLocation(Double longitude, Double latitude) {
        log.info("위치 정보 조회 시작 - 위도: {}, 경도: {}", latitude, longitude);

        validateCoordinates(longitude, latitude);

        //  Redis 캐시 확인
        WeatherAPILocation cachedLocation = cacheService.getLocationFromCache(latitude, longitude);
        if (cachedLocation != null) {
            log.info("Redis 캐시에서 위치 정보 반환");
            return cachedLocation;
        }

        GridCoordinate gridCoordinate = coordinateConverter.convertToGrid(latitude, longitude);
        List<String> locationNames = kakaoApiClient.getLocationNames(latitude, longitude);

        WeatherAPILocation location = weatherMapper.toWeatherAPILocation(
            latitude, longitude,
            gridCoordinate.getX(), gridCoordinate.getY(),
            locationNames
        );

        //  Redis에 캐싱
        cacheService.cacheLocation(latitude, longitude, location);

        return location;
    }

    // 기존 메서드들은 동일하게 유지
    @Transactional
    protected Weather fetchAndSaveWeatherData(Double latitude, Double longitude,
        GridCoordinate gridCoordinate) {
        try {
            log.info("외부 API 병렬 호출 시작");
            long startTime = System.currentTimeMillis();

            CompletableFuture<WeatherApiResponse> weatherApiFuture = CompletableFuture
                .supplyAsync(() -> {
                    log.debug("기상청 API 호출 시작");
                    return weatherApiClient.getWeatherForecast(gridCoordinate);
                }, apiCallExecutor)
                .orTimeout(10, TimeUnit.SECONDS);

            CompletableFuture<List<String>> locationFuture = CompletableFuture
                .supplyAsync(() -> {
                    log.debug("카카오 지역명 API 호출 시작");
                    return kakaoApiClient.getLocationNames(latitude, longitude);
                }, apiCallExecutor)
                .orTimeout(5, TimeUnit.SECONDS);

            try {
                CompletableFuture.allOf(weatherApiFuture, locationFuture).join();

                WeatherApiResponse apiResponse = weatherApiFuture.get();
                List<String> locationNames = locationFuture.get();

                long endTime = System.currentTimeMillis();
                log.info("외부 API 병렬 호출 완료 - 소요시간: {}ms", endTime - startTime);

                validateApiResponse(apiResponse);

                WeatherAPILocation location = weatherMapper.toWeatherAPILocation(
                    latitude, longitude,
                    gridCoordinate.getX(), gridCoordinate.getY(),
                    locationNames
                );

                List<WeatherApiResponse.Item> items = apiResponse.response().body().items().item();
                Weather weather = weatherMapper.fromApiResponse(items, location);

                String responseHash = generateResponseHash(apiResponse);
                weather.setApiResponseHash(responseHash);

                Weather savedWeather = weatherRepository.save(weather);
                log.info("날씨 데이터 DB 저장 완료 - ID: {}", savedWeather.getId());

                return savedWeather;

            } catch (CompletionException e) {
                log.error("외부 API 호출 중 오류 발생", e);
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new WeatherDataFetchException("외부 API 호출 실패", e);
            }

        } catch (WeatherApiException | WeatherDataFetchException | InvalidCoordinateException e) {
            log.error("날씨 데이터 조회 실패", e);
            throw e;
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생", e);
            throw new WeatherDataFetchException("날씨 데이터 조회 중 오류 발생", e);
        }
    }

    @Override
    @Transactional
    public int cleanupOldWeatherData() {
        return cleanupOldWeatherData(defaultRetentionDays);
    }

    @Override
    @Transactional
    public int cleanupOldWeatherData(int retentionDays) {
        log.info("날씨 데이터 정리 시작 - 보관 기간: {}일", retentionDays);

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        long countBefore = weatherRepository.countOldWeatherData(cutoffDate);

        if (countBefore == 0) {
            log.info("삭제할 날씨 데이터가 없습니다.");
            return 0;
        }

        weatherRepository.deleteOldWeatherData(cutoffDate);
        log.info("날씨 데이터 정리 완료 - 삭제된 데이터: {}건", countBefore);

        return (int) countBefore;
    }

    private void validateCoordinates(Double longitude, Double latitude) {
        if (longitude == null || latitude == null) {
            throw new InvalidCoordinateException("위도와 경도는 필수입니다.");
        }

        if (latitude < 33.0 || latitude > 43.0 || longitude < 124.0 || longitude > 132.0) {
            throw new InvalidCoordinateException("한국 영토 범위를 벗어난 좌표입니다.");
        }
    }

    private void validateApiResponse(WeatherApiResponse response) {
        if (response == null || response.response() == null) {
            throw new WeatherDataFetchException("API 응답이 null입니다.");
        }

        WeatherApiResponse.Header header = response.response().header();
        if (header == null || !"00".equals(header.resultCode())) {
            String errorMsg = header != null ? header.resultMsg() : "Unknown error";
            String resultCode = header != null ? header.resultCode() : "UNKNOWN";

            switch (resultCode) {
                case "01" ->
                    throw new WeatherApiException("어플리케이션 에러 - base_date/base_time 파라미터 오류",
                        resultCode);
                case "02" -> throw new WeatherApiException("데이터베이스 에러", resultCode);
                case "03" -> throw new WeatherDataFetchException("해당 조건의 데이터가 없습니다 - nx/ny 좌표 오류");
                case "04" -> throw new WeatherApiException("HTTP 에러 - 기상청 서버 연결 오류", resultCode);
                case "05" -> throw new WeatherApiException("서비스 연결 실패", resultCode);
                case "10" -> throw new InvalidCoordinateException("잘못된 요청 파라미터입니다");
                case "11" -> throw new WeatherApiException("필수 요청 파라미터가 누락되었습니다", resultCode);
                case "12" -> throw new WeatherApiException("해당 오픈API 서비스가 없거나 폐기되었습니다", resultCode);
                case "20" -> throw new WeatherApiException("서비스 접근 거부", resultCode);
                case "21" -> throw new WeatherApiException("일시적으로 사용할 수 없는 서비스키", resultCode);
                case "22" -> throw new WeatherApiException("서비스 요청 제한 횟수 초과", resultCode);
                case "30" -> throw new WeatherApiException("등록되지 않은 서비스키", resultCode);
                case "31" -> throw new WeatherApiException("기한 만료된 서비스키", resultCode);
                case "32" -> throw new WeatherApiException("등록되지 않은 IP", resultCode);
                case "33" -> throw new WeatherApiException("서명되지 않은 호출", resultCode);
                case "99" -> throw new WeatherApiException("기타 에러", resultCode);
                default -> throw new WeatherApiException("API 호출 실패: " + errorMsg, resultCode);
            }
        }

        WeatherApiResponse.Body body = response.response().body();
        if (body == null || body.items() == null || body.items().item() == null || body.items()
            .item().isEmpty()) {
            throw new WeatherDataFetchException("날씨 데이터가 없습니다.");
        }
    }

    private String generateResponseHash(WeatherApiResponse response) {
        try {
            String dataString = response.toString();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(dataString.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hashBytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("해시 생성 실패", e);
            return UUID.randomUUID().toString();
        }
    }

    private String calculateBaseDate(LocalDateTime dateTime) {
        return dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    private String calculateBaseTime(LocalDateTime dateTime) {
        int hour = dateTime.getHour();
        int[] baseHours = {2, 5, 8, 11, 14, 17, 20, 23};

        for (int i = baseHours.length - 1; i >= 0; i--) {
            if (hour >= baseHours[i]) {
                return String.format("%02d00", baseHours[i]);
            }
        }

        LocalDateTime yesterday = dateTime.minusDays(1);
        return yesterday.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "2300";
    }

    private List<WeatherDto> processFiveDayForecast(WeatherApiResponse response,
        Double latitude, Double longitude,
        GridCoordinate grid, List<String> locationNames) {

        WeatherAPILocation location = weatherMapper.toWeatherAPILocation(
            latitude, longitude, grid.getX(), grid.getY(), locationNames
        );

        List<WeatherApiResponse.Item> items = response.response().body().items().item();

        Map<LocalDate, List<WeatherApiResponse.Item>> groupedByDate = items.stream()
            .collect(Collectors.groupingBy(item -> {
                String fcstDate = item.fcstDate();
                return LocalDate.parse(fcstDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
            }, LinkedHashMap::new, Collectors.toList()));

        List<WeatherDto> result = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 0; i < 5; i++) {
            LocalDate targetDate = today.plusDays(i);
            List<WeatherApiResponse.Item> dayItems = groupedByDate.get(targetDate);

            if (dayItems != null && !dayItems.isEmpty()) {
                WeatherDto dayWeather = createDayWeatherDto(dayItems, location, targetDate);
                result.add(dayWeather);
            }
        }

        return result;
    }

    private WeatherDto createDayWeatherDto(List<WeatherApiResponse.Item> dayItems,
        WeatherAPILocation location, LocalDate date) {

        DoubleSummaryStatistics tempStats = dayItems.stream()
            .filter(item -> "TMP".equals(item.category()))
            .mapToDouble(item -> Double.parseDouble(item.fcstValue()))
            .summaryStatistics();

        double avgHumidity = dayItems.stream()
            .filter(item -> "REH".equals(item.category()))
            .mapToDouble(item -> Double.parseDouble(item.fcstValue()))
            .average()
            .orElse(0.0);

        SkyStatus dominantSky = determineDominantSkyStatus(dayItems);
        PrecipitationDto precipitation = calculatePrecipitation(dayItems);
        double avgWindSpeed = calculateAverageWindSpeed(dayItems);

        return new WeatherDto(
            UUID.randomUUID(),
            LocalDateTime.now(),
            date.atTime(LocalTime.NOON),
            location,
            dominantSky,
            precipitation,
            new HumidityDto(avgHumidity, 0.0),
            new TemperatureDto(
                tempStats.getAverage(),
                tempStats.getMin(),
                tempStats.getMax(),
                0.0
            ),
            new WindSpeedDto(avgWindSpeed, determineWindStrength(avgWindSpeed))
        );
    }

    private SkyStatus determineDominantSkyStatus(List<WeatherApiResponse.Item> items) {
        Map<String, Long> skyCount = items.stream()
            .filter(item -> "SKY".equals(item.category()))
            .collect(Collectors.groupingBy(
                WeatherApiResponse.Item::fcstValue,
                Collectors.counting()
            ));

        return skyCount.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(entry -> mapSkyStatus(entry.getKey()))
            .orElse(SkyStatus.CLEAR);
    }

    private PrecipitationDto calculatePrecipitation(List<WeatherApiResponse.Item> items) {
        Set<String> ptyValues = items.stream()
            .filter(item -> "PTY".equals(item.category()))
            .map(WeatherApiResponse.Item::fcstValue)
            .filter(value -> !"0".equals(value))
            .collect(Collectors.toSet());

        PrecipitationType type = ptyValues.isEmpty() ? PrecipitationType.NONE :
            mapPrecipitationType(ptyValues.iterator().next());

        double maxPop = items.stream()
            .filter(item -> "POP".equals(item.category()))
            .mapToDouble(item -> Double.parseDouble(item.fcstValue()))
            .max()
            .orElse(0.0);

        double totalPcp = items.stream()
            .filter(item -> "PCP".equals(item.category()))
            .map(WeatherApiResponse.Item::fcstValue)
            .filter(value -> !"강수없음".equals(value))
            .mapToDouble(value -> parseAmount(value))
            .sum();

        return new PrecipitationDto(type, totalPcp, maxPop);
    }

    private double calculateAverageWindSpeed(List<WeatherApiResponse.Item> items) {
        return items.stream()
            .filter(item -> "WSD".equals(item.category()))
            .mapToDouble(item -> Double.parseDouble(item.fcstValue()))
            .average()
            .orElse(0.0);
    }

    private WindStrength determineWindStrength(double speed) {
        if (speed < 4.0) return WindStrength.WEAK;
        if (speed < 9.0) return WindStrength.MODERATE;
        return WindStrength.STRONG;
    }

    private SkyStatus mapSkyStatus(String value) {
        return switch (value) {
            case "1" -> SkyStatus.CLEAR;
            case "3" -> SkyStatus.MOSTLY_CLOUDY;
            case "4" -> SkyStatus.CLOUDY;
            default -> SkyStatus.CLEAR;
        };
    }

    private PrecipitationType mapPrecipitationType(String value) {
        return switch (value) {
            case "1", "5" -> PrecipitationType.RAIN;
            case "2", "6" -> PrecipitationType.RAIN_SNOW;
            case "3", "7" -> PrecipitationType.SNOW;
            default -> PrecipitationType.NONE;
        };
    }

    private double parseAmount(String value) {
        if (value.equals("강수없음")) return 0.0;
        if (value.contains("mm")) {
            return Double.parseDouble(value.replace("mm", "").trim());
        }
        return Double.parseDouble(value);
    }
}