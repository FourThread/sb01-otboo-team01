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
import com.fourthread.ozang.module.domain.weather.dto.external.WeatherApiResponse.Item;
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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
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
    public WeatherDto getWeatherForecast(Double longitude, Double latitude) {
        log.info("날씨 예보 조회 시작 - 위도: {}, 경도: {}", latitude, longitude);

        validateCoordinates(longitude, latitude);

        // 1단계: Redis 캐시 확인
        WeatherDto cachedWeather = cacheService.getCurrentWeatherFromCache(latitude, longitude);
        if (cachedWeather != null) {
            log.info("Redis 캐시에서 날씨 데이터 반환");
            return cachedWeather;
        }

        GridCoordinate gridCoordinate = coordinateConverter.convertToGrid(latitude, longitude);

        // 2단계: DB 캐시 확인 (1시간 이내 데이터)
        Optional<Weather> recentWeather = weatherRepository.findLatestByGridCoordinate(
            gridCoordinate.getX(), gridCoordinate.getY());

        if (recentWeather.isPresent()) {
            Weather weather = recentWeather.get();
            if (weather.getForecastedAt().isAfter(LocalDateTime.now().minusHours(1))) {
                log.info("DB 캐시에서 날씨 데이터 반환");
                WeatherDto weatherDto = weatherMapper.toDto(weather);

                cacheService.cacheCurrentWeather(latitude, longitude, weatherDto);
                return weatherDto;
            }
        }

        Weather freshWeather = fetchAndSaveWeatherData(latitude, longitude, gridCoordinate);
        WeatherDto result = weatherMapper.toDto(freshWeather);

        cacheService.cacheCurrentWeather(latitude, longitude, result);

        return result;
    }

    @Override
    @Transactional
    public List<WeatherDto> getFiveDayForecast(Double longitude, Double latitude) {
        log.info("5일 예보 조회 시작 - 위도: {}, 경도: {}", latitude, longitude);

        validateCoordinates(longitude, latitude);

        String baseTime = calculateBaseTime();

        List<WeatherDto> cachedForecast = cacheService.getForecastFromCache(latitude, longitude,
            baseTime);
        if (cachedForecast != null && !cachedForecast.isEmpty()) {
            log.info("Redis 캐시에서 5일 예보 데이터 반환");
            return cachedForecast;
        }

        try {
            GridCoordinate gridCoordinate = coordinateConverter.convertToGrid(latitude, longitude);

            log.info("외부 API 병렬 호출 시작");
            long startTime = System.currentTimeMillis();

            CompletableFuture<WeatherApiResponse> weatherApiFuture = CompletableFuture
                .supplyAsync(() -> {
                    log.debug("기상청 단기예보 API 호출 시작");
                    long apiStartTime = System.currentTimeMillis();
                    WeatherApiResponse response = weatherApiClient.getWeatherForecast(
                        gridCoordinate);
                    long apiEndTime = System.currentTimeMillis();
                    log.debug("기상청 단기예보 API 호출 완료 - 소요시간: {}ms", apiEndTime - apiStartTime);
                    return response;
                }, apiCallExecutor)
                .orTimeout(10, TimeUnit.SECONDS);

            CompletableFuture<List<String>> locationFuture = CompletableFuture
                .supplyAsync(() -> {
                    log.debug("카카오 지역명 API 호출 시작");
                    return kakaoApiClient.getLocationNames(latitude, longitude);
                }, apiCallExecutor)
                .orTimeout(5, TimeUnit.SECONDS);

            CompletableFuture.allOf(weatherApiFuture, locationFuture).join();

            WeatherApiResponse apiResponse = weatherApiFuture.get();
            List<String> locationNames = locationFuture.get();

            long endTime = System.currentTimeMillis();
            log.info("외부 API 병렬 호출 완료 - 소요시간: {}ms", endTime - startTime);

            validateApiResponse(apiResponse);

            Optional<Weather> yesterdayWeather = weatherRepository.findLatestByGridCoordinateAndDate(
                gridCoordinate.getX(), gridCoordinate.getY(), LocalDate.now().minusDays(1)
            );


            List<WeatherDto> result = processFiveDayForecast(apiResponse, latitude, longitude,
                gridCoordinate, locationNames, yesterdayWeather);

            if (!result.isEmpty()) {
                WeatherDto todayWeather = result.get(0);
                saveTodayWeatherToDatabase(todayWeather, apiResponse, gridCoordinate);
            }

            cacheService.cacheForecast(latitude, longitude, baseTime, result);

            return result;

        } catch (Exception e) {
            log.error("5일 예보 조회 실패", e);
            throw new WeatherDataFetchException("5일 예보 조회 중 오류 발생", e);
        }
    }

    /**
     * 오늘 날씨 데이터를 DB에 저장
     */
    private void saveTodayWeatherToDatabase(WeatherDto todayWeather, WeatherApiResponse apiResponse,
        GridCoordinate gridCoordinate) {

        try {
            // 오늘 날짜의 기존 데이터가 있는지 확인
            Optional<Weather> existingWeather = weatherRepository.findLatestByGridCoordinateAndDate(
                gridCoordinate.getX(), gridCoordinate.getY(), LocalDate.now()
            );

            if (existingWeather.isPresent() &&
                existingWeather.get().getForecastedAt()
                    .isAfter(LocalDateTime.now().minusHours(1))) {
                log.debug("1시간 이내 데이터가 이미 존재하여 DB 저장을 생략합니다.");
                return;
            }

            List<Item> todayItems = apiResponse.response().body().items().item()
                .stream()
                .filter(item -> {
                    String fcstDate = item.fcstDate();
                    LocalDate itemDate = LocalDate.parse(fcstDate,
                        DateTimeFormatter.ofPattern("yyyyMMdd"));

                    return itemDate.equals(LocalDate.now());
                })
                .collect(Collectors.toList());

            if (!todayItems.isEmpty()) {
                Weather weather = weatherMapper.fromApiResponse(todayItems,
                    todayWeather.location());
                String responseHash = generateResponseHash(apiResponse);
                weather.setApiResponseHash(responseHash);

                Weather savedWeather = weatherRepository.save(weather);
                log.info("오늘 날씨 데이터 DB 저장 완료 - ID: {}", savedWeather.getId());
            }
        } catch (Exception e) {
            log.error("오늘 날씨 데이터 DB 저장 실패, e");
            // DB 저장 실패해도 예외를 던지지 않음 (캐시는 정상 동작해야 됨)
        }
    }

    @Override
    public WeatherAPILocation getWeatherLocation(Double longitude, Double latitude) {
        log.info("위치 정보 조회 시작 - 위도: {}, 경도: {}", latitude, longitude);

        validateCoordinates(longitude, latitude);

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

        cacheService.cacheLocation(latitude, longitude, location);

        return location;

    }

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
            String responseStr = response.toString();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(responseStr.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            return UUID.randomUUID().toString();
        }
    }

    private String calculateBaseTime() {
        LocalDateTime now = LocalDateTime.now();
        String[] baseTimes = {"0200", "0500", "0800", "1100", "1400", "1700", "2000", "2300"};

        for (int i = baseTimes.length - 1; i >= 0; i--) {
            LocalTime baseTime = LocalTime.parse(baseTimes[i], DateTimeFormatter.ofPattern("HHmm"));
            LocalDateTime baseDateTime = now.toLocalDate().atTime(baseTime);

            if (now.isAfter(baseDateTime.plusMinutes(10))) { // API 제공 시간 고려
                return baseTimes[i];
            }
        }

        // 전날 마지막 발표시각
        return "2300";
    }

    private List<WeatherDto> processFiveDayForecast(WeatherApiResponse response,
        Double latitude, Double longitude,
        GridCoordinate grid, List<String> locationNames,
        Optional<Weather> yesterdayWeather) {

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
                WeatherDto dayWeather = createDayWeatherDto(dayItems, location, targetDate,
                    i == 0 ? yesterdayWeather : Optional.empty()); //첫 번째 날만 전날 대비 계산
                result.add(dayWeather);
            }
        }

        return result;
    }

    private WeatherDto createDayWeatherDto(List<WeatherApiResponse.Item> dayItems,
        WeatherAPILocation location, LocalDate date, Optional<Weather> yesterdayWeather) {

        DoubleSummaryStatistics tempStats = dayItems.stream()
            .filter(item -> "TMP".equals(item.category()))
            .mapToDouble(item -> Double.parseDouble(item.fcstValue()))
            .summaryStatistics();

        double avgHumidity = dayItems.stream()
            .filter(item -> "REH".equals(item.category()))
            .mapToDouble(item -> Double.parseDouble(item.fcstValue()))
            .average()
            .orElse(0.0);

        // 전날 대비 온도 변화 계산
        double tempComparedToDayBefore = 0.0;
        double humidityComparedToDayBefore = 0.0;

        if (yesterdayWeather.isPresent()) {
            double yesterdayAvgTemp = yesterdayWeather.get().getTemperature().current();
            double yesterdayAvgHumidity = yesterdayWeather.get().getHumidity().current();

            tempComparedToDayBefore = tempStats.getAverage() - yesterdayAvgTemp;
            humidityComparedToDayBefore = avgHumidity - yesterdayAvgHumidity;

            log.debug("전날 대비 계산 - 오늘 평균기온: {}, 전날 평균기온: {}, 차이: {}",
                tempStats.getAverage(), yesterdayAvgTemp, tempComparedToDayBefore);
        }

        TemperatureDto temperature = new TemperatureDto(
            tempStats.getAverage(),
            tempComparedToDayBefore, // 전날 대비 온도 차이
            tempStats.getMin(),
            tempStats.getMax()
        );

        HumidityDto humidity = new HumidityDto(avgHumidity, humidityComparedToDayBefore);

        PrecipitationDto precipitation = calculatePrecipitation(dayItems);

        double avgWindSpeed = calculateAverageWindSpeed(dayItems);
        WindStrength windStrength = determineWindStrength(avgWindSpeed);
        WindSpeedDto windSpeed = new WindSpeedDto(avgWindSpeed, windStrength);

        SkyStatus skyStatus = determineDominantSkyStatus(dayItems);

        return new WeatherDto(
            UUID.randomUUID(),
            LocalDateTime.now(),
            date.atStartOfDay(),
            location,
            skyStatus,
            precipitation,
            humidity,
            temperature,
            windSpeed
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
            .mapToDouble(this::parseAmount)
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
        if (speed < 4.0) {
            return WindStrength.WEAK;
        }
        if (speed < 9.0) {
            return WindStrength.MODERATE;
        }
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
            case "1", "4" -> PrecipitationType.RAIN;
            case "2" -> PrecipitationType.RAIN_SNOW;
            case "3" -> PrecipitationType.SNOW;
            default -> PrecipitationType.NONE;
        };
    }

    /**
     * 강수량 문자열을 Double로 파싱
     * 기상청 API의 다양한 강수량 표현 방식을 처리
     */
    private double parseAmount(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0.0;
        }

        String cleanValue = value.trim();

        // "강수없음" 처리
        if (cleanValue.equals("강수없음") || cleanValue.equals("-") || cleanValue.equals("0")) {
            return 0.0;
        }

        // "미만" 처리 (다양한 패턴)
        if (cleanValue.contains("미만")) {
            log.info("[강수량 파싱]: 미만");
            if (cleanValue.contains("mm 미만") || cleanValue.contains("미만")) {
                return 0.5;
            }
        }

        // "이상" 처리
        if (cleanValue.contains("이상")) {
            // "50.0mm 이상", "50 이상" 등
            try {
                String numberPart = cleanValue.replaceAll("[^0-9.]", "");
                if (!numberPart.isEmpty()) {
                    return Double.parseDouble(numberPart);
                }
            } catch (NumberFormatException e) {
                log.warn("강수량 파싱 실패 (이상): {}", value);
                return 50.0; // 기본값
            }
        }

        // 범위 표현 처리 ("30.0~50.0mm", "30~50" 등)
        if (cleanValue.contains("~")) {
            try {
                log.info("[PCP]: ~");
                String[] parts = cleanValue.replace("mm", "").split("~");
                log.info("[강수량 파싱]: mm 제거 및 ~ 기준으로 분리");
                if (parts.length >= 2) {
                    double pcp1 = Double.parseDouble(parts[0].trim());
                    double pcp2 = Double.parseDouble(parts[1].trim());
                    log.info("[PCP]: {}mm ~ {}mm", pcp1, pcp2);
                    return Double.sum(pcp1 / 2.0, pcp2 / 2.0);
                }
            } catch (NumberFormatException e) {
                log.warn("강수량 파싱 실패 (범위): {}", value);
                return 0.0;
            }
        }

        // 일반 숫자 처리 (mm 단위 포함/미포함)
        try {
            String numberPart = cleanValue.replace("mm", "").trim();
            return Double.parseDouble(numberPart);
        } catch (NumberFormatException e) {
            log.warn("강수량 파싱 실패 (일반): {} - 0.0으로 설정", value);
            return 0.0;
        }
    }
}