package com.fourthread.ozang.core.domain.weather.service;

import com.fourthread.ozang.core.domain.weather.client.KakaoApiClient;
import com.fourthread.ozang.core.domain.weather.client.WeatherApiClient;
import com.fourthread.ozang.core.domain.weather.dto.HumidityDto;
import com.fourthread.ozang.core.domain.weather.dto.PrecipitationDto;
import com.fourthread.ozang.core.domain.weather.dto.TemperatureDto;
import com.fourthread.ozang.core.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.core.domain.weather.dto.WeatherChangeDto;
import com.fourthread.ozang.core.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.core.domain.weather.dto.WindSpeedDto;
import com.fourthread.ozang.core.domain.weather.dto.external.WeatherApiResponse;
import com.fourthread.ozang.core.domain.weather.dto.external.WeatherApiResponse.Item;
import com.fourthread.ozang.core.domain.weather.dto.type.PrecipitationType;
import com.fourthread.ozang.core.domain.weather.dto.type.SkyStatus;
import com.fourthread.ozang.core.domain.weather.dto.type.WeatherChangeType;
import com.fourthread.ozang.core.domain.weather.dto.type.WindStrength;
import com.fourthread.ozang.core.domain.weather.entity.GridCoordinate;
import com.fourthread.ozang.core.domain.weather.entity.Weather;
import com.fourthread.ozang.core.domain.weather.exception.InvalidCoordinateException;
import com.fourthread.ozang.core.domain.weather.exception.WeatherApiException;
import com.fourthread.ozang.core.domain.weather.exception.WeatherDataFetchException;
import com.fourthread.ozang.core.domain.weather.mapper.WeatherMapper;
import com.fourthread.ozang.core.domain.weather.repository.WeatherRepository;
import com.fourthread.ozang.core.domain.weather.util.CoordinateConverter;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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
        log.info("초단기예보 조회 시작 - 위도: {}, 경도: {}", latitude, longitude);

        validateCoordinates(longitude, latitude);

        try {
            GridCoordinate gridCoordinate = coordinateConverter.convertToGrid(latitude, longitude);

            // 초단기예보 API 호출
            WeatherApiResponse apiResponse = weatherApiClient.getUltraShortTermForecast(gridCoordinate);
            validateApiResponse(apiResponse);

            // 위치 정보 조회 (캐시 먼저 확인)
            WeatherAPILocation location = getWeatherLocation(longitude, latitude);

            // 현재 시간 기준 가장 가까운 예보 시간의 데이터 추출
            List<Item> currentHourItems = extractCurrentHourItems(apiResponse);

            if (currentHourItems.isEmpty()) {
                throw new WeatherDataFetchException("현재 시간 초단기예보 데이터가 없습니다.");
            }

            // WeatherDto 생성
            WeatherDto weatherDto = createWeatherDtoFromItems(currentHourItems, location);

            log.info("초단기예보 조회 완료 - 위도: {}, 경도: {}", latitude, longitude);
            return weatherDto;

        } catch (Exception e) {
            log.error("초단기예보 조회 실패", e);
            throw new WeatherDataFetchException("초단기예보 조회 중 오류 발생", e);
        }
    }

    @Override
    public List<WeatherChangeDto> detectWeatherChanges(Double latitude, Double longitude) {
        log.info("날씨 변화 감지 시작 - 위도: {}, 경도: {}", latitude, longitude);

        List<WeatherChangeDto> changes = new ArrayList<>();

        try {
            // 현재 초단기예보 조회
            WeatherDto currentWeather = getWeatherForecast(longitude, latitude);

            // 1시간 전 데이터 조회 (캐시)
            WeatherDto previousWeather = getPreviousHourWeather(latitude, longitude);

            if (previousWeather == null) {
                log.debug("이전 시간 데이터가 없어 변화 감지를 건너뜁니다.");
                return changes;
            }

            // 온도 변화 감지
            changes.addAll(detectTemperatureChanges(currentWeather, previousWeather));

            // 강수 변화 감지
            changes.addAll(detectPrecipitationChanges(currentWeather, previousWeather));

            // 풍속 변화 감지
            changes.addAll(detectWindChanges(currentWeather, previousWeather));

            // 하늘 상태 변화 감지
            changes.addAll(detectSkyChanges(currentWeather, previousWeather));

            if (!changes.isEmpty()) {
                log.info("날씨 변화 감지됨 - 위도: {}, 경도: {}, 변화 수: {}",
                    latitude, longitude, changes.size());
            }

        } catch (Exception e) {
            log.error("날씨 변화 감지 실패 - 위도: {}, 경도: {}", latitude, longitude, e);
        }

        return changes;
    }

    @Override
    @Transactional
    public List<WeatherDto> getFiveDayForecast(Double longitude, Double latitude) {
        log.info("5일 예보 조회 시작 - 위도: {}, 경도: {}", latitude, longitude);

        validateCoordinates(longitude, latitude);

        String baseTime = calculateBaseTime();

        List<WeatherDto> cachedForecast = cacheService.getForecastFromCache(latitude, longitude, baseTime);
        if (cachedForecast != null && !cachedForecast.isEmpty()) {
            log.info("Redis 캐시에서 5일 예보 데이터 반환 - {}건", cachedForecast.size());
            cacheService.recordActiveRegion(latitude, longitude);
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
                gridCoordinate.getX(), gridCoordinate.getY(), LocalDateTime.now().minusDays(1)
            );


            List<WeatherDto> result = processFiveDayForecast(apiResponse, latitude, longitude,
                gridCoordinate, locationNames, yesterdayWeather);


            cacheService.cacheForecast(latitude, longitude, baseTime, result);

            cacheService.recordActiveRegion(latitude, longitude);

            return result;

        } catch (Exception e) {
            log.error("5일 예보 조회 실패", e);
            throw new WeatherDataFetchException("5일 예보 조회 중 오류 발생", e);
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

    /**
     * 현재 시간 기준 가장 가까운 예보 시간의 데이터 추출
     */
    private List<Item> extractCurrentHourItems(WeatherApiResponse apiResponse) {
        LocalDateTime now = LocalDateTime.now();
        String targetDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String targetTime = now.format(DateTimeFormatter.ofPattern("HH00"));

        return apiResponse.response().body().items().item().stream()
            .filter(item -> targetDate.equals(item.fcstDate()) && targetTime.equals(item.fcstTime()))
            .toList();
    }

    /**
     * API 응답 아이템들로부터 WeatherDto 생성
     */
    private WeatherDto createWeatherDtoFromItems(List<Item> items, WeatherAPILocation location) {
        Map<String, String> dataMap = items.stream()
            .collect(Collectors.toMap(Item::category, Item::fcstValue, (old, new_) -> new_));

        // 온도 정보
        double temperature = parseDouble(dataMap.get("T1H"), 0.0);
        TemperatureDto temperatureDto = new TemperatureDto(temperature, 0.0, temperature, temperature);

        // 습도 정보
        double humidity = parseDouble(dataMap.get("REH"), 50.0);
        HumidityDto humidityDto = new HumidityDto(humidity, 0.0);

        // 강수 정보
        PrecipitationType precipitationType = mapPrecipitationType(dataMap.get("PTY"));
        double precipitationAmount = parseAmount(dataMap.get("RN1"));
        PrecipitationDto precipitationDto = new PrecipitationDto(precipitationType, precipitationAmount, 0.0);

        // 풍속 정보
        double windSpeed = parseDouble(dataMap.get("WSD"), 0.0);
        WindStrength windStrength = WindStrength.fromSpeed(windSpeed);
        WindSpeedDto windSpeedDto = new WindSpeedDto(windSpeed, windStrength);

        // 하늘 상태
        SkyStatus skyStatus = mapSkyStatus(dataMap.get("SKY"));

        return new WeatherDto(
            UUID.randomUUID(),
            LocalDateTime.now(),
            LocalDateTime.now(),
            location,
            skyStatus,
            precipitationDto,
            humidityDto,
            temperatureDto,
            windSpeedDto
        );
    }

    /**
     * 1시간 전 날씨 데이터 조회
     */
    private WeatherDto getPreviousHourWeather(Double latitude, Double longitude) {
        try {
            // 캐시에서 1시간 전 데이터 조회 시도
            String previousHourKey = LocalDateTime.now().minusHours(1)
                .format(DateTimeFormatter.ofPattern("yyyyMMddHH"));

            // DB에서 조회
            GridCoordinate gridCoordinate = coordinateConverter.convertToGrid(latitude, longitude);
            Optional<Weather> previousWeather = weatherRepository.findLatestByGridCoordinateAndDate(
                gridCoordinate.getX(), gridCoordinate.getY(), LocalDateTime.now().minusHours(1)
            );

            return previousWeather.map(weatherMapper::toDto).orElse(null);
        } catch (Exception e) {
            log.warn("이전 시간 날씨 데이터 조회 실패", e);
            return null;
        }
    }

    //  날씨 변화 감지 헬퍼 메서드
    private List<WeatherChangeDto> detectTemperatureChanges(WeatherDto current, WeatherDto previous) {
        List<WeatherChangeDto> changes = new ArrayList<>();

        double currentTemp = current.temperature().current();
        double previousTemp = previous.temperature().current();
        double tempDiff = currentTemp - previousTemp;

        // 3도 이상 급상승
        if (tempDiff >= 3.0) {
            changes.add(new WeatherChangeDto(
                WeatherChangeType.TEMPERATURE_RISE,
                String.format("기온이 %.1f도 급상승했습니다", tempDiff),
                previousTemp,
                currentTemp,
                "°C",
                LocalDateTime.now(),
                current.location()
            ));
        }
        // 3도 이상 급하강
        else if (tempDiff <= -3.0) {
            changes.add(new WeatherChangeDto(
                WeatherChangeType.TEMPERATURE_DROP,
                String.format("기온이 %.1f도 급하강했습니다", Math.abs(tempDiff)),
                previousTemp,
                currentTemp,
                "°C",
                LocalDateTime.now(),
                current.location()
            ));
        }

        return changes;
    }

    private List<WeatherChangeDto> detectPrecipitationChanges(WeatherDto current, WeatherDto previous) {
        List<WeatherChangeDto> changes = new ArrayList<>();

        PrecipitationType currentType = current.precipitation().type();
        PrecipitationType previousType = previous.precipitation().type();

        // 강수 시작
        if (previousType == PrecipitationType.NONE && currentType != PrecipitationType.NONE) {
            changes.add(new WeatherChangeDto(
                WeatherChangeType.PRECIPITATION_START,
                String.format("%s가 시작되었습니다", currentType.getDescription()),
                0.0,
                current.precipitation().amount(),
                "mm",
                LocalDateTime.now(),
                current.location()
            ));
        }
        // 강수 종료
        else if (previousType != PrecipitationType.NONE && currentType == PrecipitationType.NONE) {
            changes.add(new WeatherChangeDto(
                WeatherChangeType.PRECIPITATION_END,
                String.format("%s가 끝났습니다", previousType.getDescription()),
                previous.precipitation().amount(),
                0.0,
                "mm",
                LocalDateTime.now(),
                current.location()
            ));
        }
        // 강수 형태 변화
        else if (previousType != PrecipitationType.NONE && currentType != PrecipitationType.NONE
            && previousType != currentType) {
            changes.add(new WeatherChangeDto(
                WeatherChangeType.PRECIPITATION_TYPE_CHANGE,
                String.format("%s에서 %s로 변경되었습니다",
                    previousType.getDescription(), currentType.getDescription()),
                previous.precipitation().amount(),
                current.precipitation().amount(),
                "mm",
                LocalDateTime.now(),
                current.location()
            ));
        }

        return changes;
    }

    private List<WeatherChangeDto> detectWindChanges(WeatherDto current, WeatherDto previous) {
        List<WeatherChangeDto> changes = new ArrayList<>();

        double currentWind = current.windSpeed().speed();
        double previousWind = previous.windSpeed().speed();

        // 풍속이 2배 이상 증가하고 9m/s 이상일 때
        if (currentWind >= previousWind * 2 && currentWind >= 9.0) {
            changes.add(new WeatherChangeDto(
                WeatherChangeType.WIND_INCREASE,
                String.format("강풍이 발생했습니다 (%.1fm/s)", currentWind),
                previousWind,
                currentWind,
                "m/s",
                LocalDateTime.now(),
                current.location()
            ));
        }

        return changes;
    }

    private List<WeatherChangeDto> detectSkyChanges(WeatherDto current, WeatherDto previous) {
        List<WeatherChangeDto> changes = new ArrayList<>();

        SkyStatus currentSky = current.skyStatus();
        SkyStatus previousSky = previous.skyStatus();

        // 맑음 ↔ 흐림 등 급격한 변화
        if ((previousSky == SkyStatus.CLEAR && currentSky == SkyStatus.CLOUDY) ||
            (previousSky == SkyStatus.CLOUDY && currentSky == SkyStatus.CLEAR)) {
            changes.add(new WeatherChangeDto(
                WeatherChangeType.SKY_CHANGE,
                String.format("하늘 상태가 %s에서 %s로 급변했습니다",
                    previousSky.getDescription(), currentSky.getDescription()),
                0.0,
                0.0,
                "",
                LocalDateTime.now(),
                current.location()
            ));
        }

        return changes;
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
                    throw new WeatherApiException("어플리케이션 에러",
                        resultCode);
                case "02" -> throw new WeatherApiException("데이터베이스 에러", resultCode);
                case "03" -> throw new WeatherDataFetchException("데이터없음 에러");
                case "04" -> throw new WeatherApiException("HTTP 에러", resultCode);
                case "05" -> throw new WeatherApiException("서비스 연결 실패", resultCode);
                case "10" -> throw new InvalidCoordinateException("잘못된 요청 파라메터 에러");
                case "11" -> throw new WeatherApiException("필수 요청 파라미터가 없음", resultCode);
                case "12" -> throw new WeatherApiException("해당 오픈API서비스가 없거나 폐기됨", resultCode);
                case "20" -> throw new WeatherApiException("서비스 접근거부", resultCode);
                case "21" -> throw new WeatherApiException("일시적으로 사용할 수 없는 서비스키", resultCode);
                case "22" -> throw new WeatherApiException("서비스 요청제한횟수 초과에러", resultCode);
                case "30" -> throw new WeatherApiException("등록되지 않은 서비스키", resultCode);
                case "31" -> throw new WeatherApiException("기한만료된 서비스키", resultCode);
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

    /**
     * 날짜별 고유 API 응답 해시 생성
     * API 응답 + 예보 날짜를 조합하여 각 날씨 데이터마다 고유한 해시 생성
     *
     * @param response API 응답
     * @param targetDate 예보 대상 날짜
     * @return 날짜별 고유 해시값
     */
    private String generateDateSpecificResponseHash(WeatherApiResponse response, LocalDate targetDate) {
        try {
            // API 응답 + 예보 날짜를 조합하여 고유한 데이터 생성
            String combinedData = response.toString() + "_" + targetDate.toString();

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(combinedData.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String resultHash = hexString.toString();
            log.debug("날짜별 해시 생성 완료 - 날짜: {}, Hash: {}", targetDate, resultHash);
            return resultHash;

        } catch (Exception e) {
            log.error("날짜별 해시 생성 실패 - 날짜: {}", targetDate, e);
            // 실패 시 UUID + 날짜 조합으로 대체
            return UUID.randomUUID().toString().replace("-", "") + "_" + targetDate.toString().replace("-", "");
        }
    }

    private String calculateBaseTime() {
        LocalDateTime now = LocalDateTime.now();
        String[] baseTimes = {"0200", "0500", "0800", "1100", "1400", "1700", "2000", "2300"};

        for (int i = baseTimes.length - 1; i >= 0; i--) {
            LocalTime baseTime = LocalTime.parse(baseTimes[i], DateTimeFormatter.ofPattern("HHmm"));
            LocalDateTime baseDateTime = now.toLocalDate().atTime(baseTime);

            if (now.isAfter(baseDateTime.plusMinutes(10))) {
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

        // 날짜별로 그룹화
        Map<LocalDate, List<WeatherApiResponse.Item>> groupedByDate = items.stream()
            .collect(Collectors.groupingBy(item -> {
                String fcstDate = item.fcstDate();
                return LocalDate.parse(fcstDate, DateTimeFormatter.ofPattern("yyyyMMdd"));
            }, LinkedHashMap::new, Collectors.toList()));

        List<Weather> savedWeathers = new ArrayList<>();
        LocalDate today = LocalDate.now();

        log.info("5일치 날씨 데이터 DB 저장 시작");
        for (int i = 0; i < 5; i++) {
            LocalDate targetDate = today.plusDays(i);
            List<WeatherApiResponse.Item> dayItems = groupedByDate.get(targetDate);

            if (dayItems != null && !dayItems.isEmpty()) {
                try {
                    // Weather 엔티티 생성 및 저장
                    Weather weather = createAndSaveWeatherEntity(dayItems, location, response, targetDate);
                    savedWeathers.add(weather);
                    log.debug("날씨 데이터 저장 완료 - 날짜: {}, ID: {}", targetDate, weather.getId());
                } catch (Exception e) {
                    log.error("날씨 데이터 저장 실패 - 날짜: {}", targetDate, e);
                    // 저장 실패 시에도 계속 진행
                }
            }
        }

        log.info("5일치 날씨 데이터 DB 저장 완료 - 저장된 데이터: {}건", savedWeathers.size());

        // 저장된 Weather 엔티티를 사용해서 WeatherDto 생성 (전일 대비 계산 포함)
        List<WeatherDto> result = new ArrayList<>();
        for (int i = 0; i < savedWeathers.size(); i++) {
            Weather currentWeather = savedWeathers.get(i);

            // 전일 대비 계산을 위한 이전 날 데이터 결정
            Optional<Weather> previousDayWeather;
            if (i == 0) {
                // 오늘: DB에서 조회한 전날 데이터 사용
                previousDayWeather = yesterdayWeather;
            } else {
                // 나머지 날들: 바로 전날의 예보 데이터 사용
                previousDayWeather = Optional.of(savedWeathers.get(i - 1));
            }

            WeatherDto weatherDto = createWeatherDtoFromEntity(currentWeather, previousDayWeather);
            result.add(weatherDto);

            log.debug("WeatherDto 생성 완료 - 날짜: {}, UUID: {}",
                currentWeather.getForecastAt().toLocalDate(), weatherDto.id());
        }

        log.info("5일치 WeatherDto 생성 완료 - 총 {}건", result.size());
        return result;
    }

    private Weather createAndSaveWeatherEntity(List<WeatherApiResponse.Item> dayItems,
        WeatherAPILocation location, WeatherApiResponse apiResponse, LocalDate targetDate) {

        Weather weather = weatherMapper.fromApiResponse(dayItems, location);

        List<Double> temperatureValues = dayItems.stream()
            .filter(item -> "TMP".equals(item.category()))
            .map(item -> {
                try {
                    return Double.parseDouble(item.fcstValue());
                } catch (NumberFormatException e) {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .toList();

        weather.calculateAndSetTemperatureStats(temperatureValues);

        String responseHash = generateDateSpecificResponseHash(apiResponse, targetDate);
        weather.setApiResponseHash(responseHash);

        Weather savedWeather = weatherRepository.save(weather);
        log.debug("Weather 엔티티 저장 완료 - 날짜: {}, ID: {}", targetDate, savedWeather.getId());

        return savedWeather;
    }

    private WeatherDto createWeatherDtoFromEntity(Weather weather, Optional<Weather> previousDayWeather) {

        // 전일 대비 온도 및 습도 계산
        double tempComparedToDayBefore = 0.0;
        double humidityComparedToDayBefore = 0.0;

        if (previousDayWeather.isPresent()) {
            Weather previousWeather = previousDayWeather.get();
            double previousTemp = previousWeather.getTemperature().current();
            double previousHumidity = previousWeather.getHumidity().current();

            tempComparedToDayBefore = weather.getTemperature().current() - previousTemp;
            humidityComparedToDayBefore = weather.getHumidity().current() - previousHumidity;

            log.debug("전일 대비 계산 완료 - 현재기온: {}, 전날기온: {}, 차이: {}",
                weather.getTemperature().current(), previousTemp, tempComparedToDayBefore);
        }

        // 전일 대비가 반영된 DTO 생성
        TemperatureDto temperatureDto = new TemperatureDto(
            weather.getTemperature().current(),
            tempComparedToDayBefore, // 전일 대비 반영
            weather.getTemperature().min(),
            weather.getTemperature().max()
        );

        HumidityDto humidityDto = new HumidityDto(
            weather.getHumidity().current(),
            humidityComparedToDayBefore // 전일 대비 반영
        );

        return new WeatherDto(
            weather.getId(),
            weather.getForecastedAt(),
            weather.getForecastAt(),
            weather.getLocation(),
            weather.getSkyStatus(),
            weather.getPrecipitation(),
            humidityDto,
            temperatureDto,
            weather.getWindSpeed()
        );
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

    private double parseDouble(String value, double defaultValue) {
        try {
            return value != null ? Double.parseDouble(value) : defaultValue;
        } catch (NumberFormatException e) {
            return defaultValue;
        }
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