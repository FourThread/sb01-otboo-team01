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
import java.util.HashMap;
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

    @Value("${batch.weather.retention-days:30}")
    private int defaultRetentionDays;

    /**
     * =============== 새로운 설정 추가 ===============
     */
    @Value("${batch.weather.alert.heat-threshold:33}")
    private double heatThreshold;

    @Value("${batch.weather.alert.cold-threshold:-12}")
    private double coldThreshold;

    @Value("${batch.weather.alert.rain-threshold:30}")
    private double rainThreshold;

    @Value("${batch.weather.alert.wind-threshold:14}")
    private double windThreshold;

    public WeatherServiceImpl(
        WeatherRepository weatherRepository,
        WeatherMapper weatherMapper,
        WeatherApiClient weatherApiClient,
        KakaoApiClient kakaoApiClient,
        CoordinateConverter coordinateConverter,
        @Qualifier("apiCallExecutor") Executor apiCallExecutor) {

        this.weatherRepository = weatherRepository;
        this.weatherMapper = weatherMapper;
        this.weatherApiClient = weatherApiClient;
        this.kakaoApiClient = kakaoApiClient;
        this.coordinateConverter = coordinateConverter;
        this.apiCallExecutor = apiCallExecutor;
    }

    @Override
    @Transactional(readOnly = true)
    public WeatherDto getWeatherForecast(Double longitude, Double latitude) {
        log.info("날씨 정보 조회 시작 - 위도: {}, 경도: {}", latitude, longitude);

        validateCoordinates(longitude, latitude);

        GridCoordinate gridCoordinate = coordinateConverter.convertToGrid(latitude, longitude);
        log.debug("격자 좌표 변환 완료 - X: {}, Y: {}", gridCoordinate.getX(), gridCoordinate.getY());

        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Optional<Weather> cachedWeather = weatherRepository.findLatestByGridCoordinate(
            gridCoordinate.getX(),
            gridCoordinate.getY()
        );

        if (cachedWeather.isPresent() && cachedWeather.get().getForecastedAt()
            .isAfter(oneHourAgo)) {
            log.info("캐시된 날씨 데이터 사용");
            return weatherMapper.toDto(cachedWeather.get());
        }

        LocalDateTime now = LocalDateTime.now();
        String baseDate = calculateBaseDate(now);
        String baseTime = calculateBaseTime(now);
        log.debug("기상청 API 요청 시간 - base_date: {}, base_time: {}", baseDate, baseTime);

        Weather weather = fetchAndSaveWeatherData(latitude, longitude, gridCoordinate);

        //  날씨 변화 감지 및 알림 처리

        log.info("날씨 정보 조회 완료");
        return weatherMapper.toDto(weather);
    }

    @Override
    public WeatherAPILocation getWeatherLocation(Double longitude, Double latitude) {
        log.info("위치 정보 조회 시작 - 위도: {}, 경도: {}", latitude, longitude);

        validateCoordinates(longitude, latitude);

        GridCoordinate gridCoordinate = coordinateConverter.convertToGrid(latitude, longitude);

        List<String> locationNames = kakaoApiClient.getLocationNames(latitude, longitude);

        return weatherMapper.toWeatherAPILocation(
            latitude, longitude,
            gridCoordinate.getX(), gridCoordinate.getY(),
            locationNames
        );
    }

    // 비동기 처리
    @Transactional
    protected Weather fetchAndSaveWeatherData(Double latitude, Double longitude,
        GridCoordinate gridCoordinate) {
        try {
            log.info("외부 API 병렬 호출 시작");
            long startTime = System.currentTimeMillis();

            CompletableFuture<WeatherApiResponse> weatherAPiFuture = CompletableFuture
                .supplyAsync(() -> {
                    long apiStartTime = System.currentTimeMillis();
                    log.debug("기상청 API 호출 시작");
                    WeatherApiResponse response = weatherApiClient.getWeatherForecast(
                        gridCoordinate);
                    long apiEndTime = System.currentTimeMillis();
                    log.debug("기상청 API 호출 완료 - 소요시간={}ms", apiEndTime - apiStartTime);
                    return response;
                }, apiCallExecutor)
                .orTimeout(10, TimeUnit.SECONDS);

            CompletableFuture<List<String>> locationFuture = CompletableFuture
                .supplyAsync(() -> {
                    long apiStartTime = System.currentTimeMillis();
                    log.debug("카카오 API 호출 시작");
                    List<String> locations = kakaoApiClient.getLocationNames(latitude, longitude);
                    long apiEndTime = System.currentTimeMillis();
                    log.debug("카카오 API 호출 완료 - 소요시간={}ms", apiEndTime - apiStartTime);
                    return locations;
                }, apiCallExecutor)
                .orTimeout(5, TimeUnit.SECONDS);

            // 두 API 호출이 모두 완료될 때까지 대기
            CompletableFuture<Void> combinedFuture = CompletableFuture.allOf(
                weatherAPiFuture, locationFuture);

            WeatherApiResponse apiResponse;
            List<String> locationNames;

            try {
                combinedFuture.join(); // 모든 비동기 작업 완료 대기
                apiResponse = weatherAPiFuture.join();
                locationNames = locationFuture.join();

                long endTime = System.currentTimeMillis();
                log.info("외부 API 병렬 호출 완료 - 소요시간: {}ms", endTime - startTime);
            } catch (CompletionException e) {
                log.error("외부 API 호출 중 오류 발생", e);
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                }
                throw new WeatherDataFetchException("외부 API 호출 실패", e);
            } catch (Exception e) {
                log.error("외부 API 호출 중 오류 발생", e);
                throw new WeatherDataFetchException("외부 API 호출 실패", e);
            }

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
            log.info("날씨 데이터 저장 완료 - ID: {}", savedWeather.getId());

            return savedWeather;

        } catch (WeatherApiException | WeatherDataFetchException | InvalidCoordinateException e) {
            log.error("날씨 데이터 조회 실패", e);
            throw e;
        } catch (Exception e) {
            log.error("예상치 못한 오류 발생", e);
            throw new WeatherDataFetchException("날씨 데이터 조회 중 오류 발생", e);
        }
    }

    @Transactional
    protected Weather fetchAndSaveWeatherDataV1(Double latitude, Double longitude,
        GridCoordinate gridCoordinate) {
        try {
            //  1. 기상청 API 호출
            log.info(" 기상청 API 호출 시작");
            WeatherApiResponse apiResponse = weatherApiClient.getWeatherForecast(gridCoordinate);

            //  2. 응답 검증
            validateApiResponse(apiResponse);

            //  3. 카카오 API로 지역명 조회
            List<String> locationNames = kakaoApiClient.getLocationNames(latitude, longitude);

            //  4. WeatherAPILocation 생성
            WeatherAPILocation location = weatherMapper.toWeatherAPILocation(
                latitude, longitude,
                gridCoordinate.getX(), gridCoordinate.getY(),
                locationNames
            );

            //  5. Weather 엔티티 생성
            List<WeatherApiResponse.Item> items = apiResponse.response().body().items().item();
            Weather weather = weatherMapper.fromApiResponse(items, location);

            //  6. API 응답 해시 생성 (중복 방지)
            String responseHash = generateResponseHash(apiResponse);
            weather.setApiResponseHash(responseHash);

            //  7. 저장
            Weather savedWeather = weatherRepository.save(weather);
            log.info("날씨 데이터 저장 완료 - ID: {}", savedWeather.getId());

            return savedWeather;

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
    public List<WeatherDto> getFiveDayForecast(Double longitude, Double latitude) {
        validateCoordinates(longitude, latitude);

        GridCoordinate grid = coordinateConverter.convertToGrid(latitude, longitude);

        LocalDateTime nowKst = LocalDateTime.now(ZoneId.of("Asia/Seoul"));
        String baseDate = calculateBaseDate(nowKst);
        String baseTime = calculateBaseTime(nowKst);
        log.debug("기상청 단기예보 호출 기준시각 - date: {}, time: {}", baseDate, baseTime);

        log.info("5일 예보 병렬 호출 시작");
        long startTime = System.currentTimeMillis();

        CompletableFuture<WeatherApiResponse> weatherApiFuture = CompletableFuture
            .supplyAsync(() -> {
                long apiStartTime = System.currentTimeMillis();
                log.debug("기상청 단기예보 API 호출 시작");
                WeatherApiResponse response = weatherApiClient.callVilageFcst(grid, baseDate,
                    baseTime);
                long apiEndTime = System.currentTimeMillis();
                log.debug("기상청 단기예보 API 호출 완료 - 소요시간: {}ms", apiEndTime - apiStartTime);
                return response;
            }, apiCallExecutor)
            .orTimeout(15, TimeUnit.SECONDS);

        CompletableFuture<List<String>> locationFuture = CompletableFuture
            .supplyAsync(() -> {
                long apiStartTime = System.currentTimeMillis();
                log.debug("카카오 API 호출 시작");
                List<String> locations = kakaoApiClient.getLocationNames(latitude, longitude);
                long apiEndTime = System.currentTimeMillis();
                log.debug("카카오 API 호출 완료 - 소요시간: {}ms", apiEndTime - apiStartTime);
                return locations;
            }, apiCallExecutor) // 커스텀 Executor 사용
            .orTimeout(5, TimeUnit.SECONDS);

        WeatherApiResponse resp;
        List<String> locationNames;

        try {
            CompletableFuture.allOf(weatherApiFuture, locationFuture).join();
            resp = weatherApiFuture.join();
            locationNames = locationFuture.join();

            long endTime = System.currentTimeMillis();
            log.info("5일 예보 API 병렬 호출 완료 - 소요시간: {}ms", endTime - startTime);

        } catch (Exception e) {
            log.error("5일 예보 API 호출 중 오류 발생", e);
            throw new WeatherDataFetchException("5일 예보 API 호출 실패", e);
        }

        validateApiResponse(resp);

        return ensureFiveDayForecast(
            resp.response().body().items().item(),
            baseDate, baseTime, grid,
            LocalDate.now(ZoneId.of("Asia/Seoul")),
            new WeatherAPILocation(latitude, longitude, grid.getX(), grid.getY(), locationNames)
        );
    }

    private List<WeatherDto> filterFiveDay(
        List<WeatherApiResponse.Item> items,
        String baseTime,
        LocalDate today,
        WeatherAPILocation loc
    ) {
        int baseHour = Integer.parseInt(baseTime.substring(0, 2));
        boolean morning = Set.of(2, 5, 8, 11, 14).contains(baseHour);
        boolean evening = Set.of(17, 20, 23).contains(baseHour);

        log.debug("기준시각: {}시, morning: {}, evening: {}", baseHour, morning, evening);

        Map<LocalDate, List<WeatherApiResponse.Item>> byDate = items.stream()
            .filter(it -> includeCurrentApiItem(it, today, morning, evening))
            .collect(Collectors.groupingBy(
                it -> LocalDate.parse(it.fcstDate(), DateTimeFormatter.BASIC_ISO_DATE),
                LinkedHashMap::new,
                Collectors.toList()
            ));

        log.debug("현재 API에서 추출된 날짜: {}", byDate.keySet());

        List<WeatherDto> result = new ArrayList<>();
        WeatherDto previousDay = null;

        for (var entry : byDate.entrySet()) {
            LocalDate date = entry.getKey();
            List<WeatherApiResponse.Item> day = entry.getValue();

            int dayOffset = (int) ChronoUnit.DAYS.between(today, date);
            if (dayOffset > 5) {
                continue;
            }

            log.debug("처리 중인 날짜: {}, 데이터 개수: {}", date, day.size());

            WeatherDto weatherDto = createWeatherDtoFromItems(day, day.get(0).baseDate(),
                day.get(0).baseTime(), loc, previousDay);
            if (weatherDto != null) {
                result.add(weatherDto);
                previousDay = weatherDto;
            }
        }

        log.debug("현재 API에서 생성된 예보: {}일치", result.size());
        return result;
    }

    private boolean includeCurrentApiItem(WeatherApiResponse.Item it, LocalDate today,
        boolean morning, boolean evening) {
        LocalDate d = LocalDate.parse(it.fcstDate(), DateTimeFormatter.BASIC_ISO_DATE);
        int offset = (int) ChronoUnit.DAYS.between(today, d);
        String t = it.fcstTime();

        if (offset < 0 || offset > 5) {
            return false;
        }

        if (offset <= 3) {
            return t.endsWith("00");
        }

        if (offset == 4) {
            return morning
                ? Set.of("0200", "0500", "0800", "1100", "1400").contains(t)
                : t.endsWith("00");
        }

        if (offset == 5) {
            if (!evening) {
                return false;
            }
            Set<String> fiveDayTimes = Set.of("0000", "0300", "0600", "0900", "1200", "1500",
                "1800", "2100");
            return fiveDayTimes.contains(t);
        }

        return false;
    }

    private List<WeatherDto> ensureFiveDayForecast(
        List<WeatherApiResponse.Item> items,
        String baseDate, String baseTime, GridCoordinate grid,
        LocalDate today,
        WeatherAPILocation loc
    ) {
        List<WeatherDto> forecast = filterFiveDay(items, baseTime, today, loc);

        LocalDate fifthDay = today.plusDays(5);
        boolean hasFifthDay = forecast.stream()
            .anyMatch(dto -> dto.forecastAt().toLocalDate().equals(fifthDay));

        log.debug("현재 예보 일수: {}, 5일째 데이터 존재: {}", forecast.size(), hasFifthDay);

        if (!hasFifthDay) {
            log.info("5일째 데이터 부족으로 이전 발표시각에서 조회 시도");
            WeatherDto lastForecast = forecast.isEmpty() ? null : forecast.get(forecast.size() - 1);
            WeatherDto fifthDayForecast = getFifthDayFromPreviousTime(grid, baseDate, baseTime,
                today, loc, lastForecast);
            if (fifthDayForecast != null) {
                forecast.add(fifthDayForecast);
                log.info("5일째 데이터 추가 완료");
            }
        }

        List<WeatherDto> result = forecast.stream()
            .sorted(Comparator.comparing(WeatherDto::forecastAt))
            .limit(5)
            .collect(Collectors.toList());

        log.info("최종 5일 예보 완료 - 총 {}일치", result.size());
        return result;
    }

    private WeatherDto getFifthDayFromPreviousTime(
        GridCoordinate grid, String currentBaseDate, String currentBaseTime,
        LocalDate today, WeatherAPILocation loc, WeatherDto previousDay
    ) {
        try {
            String[] previousTimes = {"2300", "2000", "1700"};
            String currentTime = currentBaseTime.substring(0, 2) + "00";

            for (String prevTime : previousTimes) {
                if (prevTime.compareTo(currentTime) < 0 || !currentBaseDate.equals(
                    calculateBaseDate(LocalDateTime.now()))) {

                    String targetDate = currentBaseDate;
                    if (prevTime.compareTo(currentTime) >= 0) {
                        LocalDate prevDay = LocalDate.parse(currentBaseDate,
                            DateTimeFormatter.ofPattern("yyyyMMdd")).minusDays(1);
                        targetDate = prevDay.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
                    }

                    log.debug("5일째 데이터 조회 시도 - date: {}, time: {}", targetDate, prevTime);

                    WeatherApiResponse prevResp = weatherApiClient.callVilageFcst(grid, targetDate,
                        prevTime);

                    if (prevResp != null && prevResp.response() != null &&
                        prevResp.response().body() != null &&
                        prevResp.response().body().items() != null) {

                        LocalDate fifthDay = today.plusDays(5);
                        List<WeatherApiResponse.Item> fifthDayItems = prevResp.response().body()
                            .items().item().stream()
                            .filter(item -> {
                                LocalDate itemDate = LocalDate.parse(item.fcstDate(),
                                    DateTimeFormatter.BASIC_ISO_DATE);
                                return itemDate.equals(fifthDay);
                            })
                            .collect(Collectors.toList());

                        if (!fifthDayItems.isEmpty()) {
                            log.info("5일째 데이터 발견 - {}개 항목", fifthDayItems.size());
                            return createWeatherDtoFromItems(fifthDayItems, targetDate, prevTime,
                                loc,
                                previousDay);
                        }
                    }
                }
            }

            log.warn("5일째 데이터를 찾을 수 없습니다");
            return null;

        } catch (Exception e) {
            log.error("5일째 데이터 조회 중 오류 발생", e);
            return null;
        }
    }

    private WeatherDto createWeatherDtoFromItems(
        List<WeatherApiResponse.Item> dayItems,
        String baseDate, String baseTime,
        WeatherAPILocation loc,
        WeatherDto previousDay
    ) {
        if (dayItems.isEmpty()) {
            return null;
        }

        LocalDate date = LocalDate.parse(dayItems.get(0).fcstDate(),
            DateTimeFormatter.BASIC_ISO_DATE);

        // 기온(TMP) 처리
        DoubleSummaryStatistics tStat = dayItems.stream()
            .filter(i -> "TMP".equals(i.category()))
            .mapToDouble(i -> parseDouble(i.fcstValue()))
            .summaryStatistics();

        double curTemp = tStat.getCount() > 0 ? tStat.getAverage() : 22.0;
        double minTemp = tStat.getCount() > 0 ? tStat.getMin() : 20.0;
        double maxTemp = tStat.getCount() > 0 ? tStat.getMax() : 25.0;

        // 습도(REH)
        double avgHum = dayItems.stream()
            .filter(i -> "REH".equals(i.category()))
            .mapToDouble(i -> parseDouble(i.fcstValue()))
            .average().orElse(50.0);

        // 풍속(WSD)
        double avgWsd = dayItems.stream()
            .filter(i -> "WSD".equals(i.category()))
            .mapToDouble(i -> parseDouble(i.fcstValue()))
            .average().orElse(1.0);

        // 강수확률(POP)
        double avgPop = dayItems.stream()
            .filter(i -> "POP".equals(i.category()))
            .mapToDouble(i -> parseDouble(i.fcstValue()) / 100.0)
            .average().orElse(0.0);

        // 하늘상태(SKY)
        String skyCode = dayItems.stream()
            .filter(i -> "SKY".equals(i.category()))
            .collect(
                Collectors.groupingBy(WeatherApiResponse.Item::fcstValue, Collectors.counting()))
            .entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey).orElse("1");

        // 강수형태 및 강수량
        PrecipitationType precipitationType = dayItems.stream()
            .filter(i -> "PTY".equals(i.category()))
            .map(i -> {
                String value = i.fcstValue();
                return switch (value) {
                    case "1" -> PrecipitationType.RAIN;
                    case "2" -> PrecipitationType.RAIN_SNOW;
                    case "3" -> PrecipitationType.SNOW;
                    case "4" -> PrecipitationType.SHOWER;
                    default -> PrecipitationType.NONE;
                };
            })
            .findFirst()
            .orElse(PrecipitationType.NONE);

        double precipitation = dayItems.stream()
            .filter(i -> "PCP".equals(i.category()))
            .mapToDouble(i -> {
                String value = i.fcstValue();
                if ("강수없음".equals(value) || value.isEmpty()) {
                    return 0.0;
                }
                try {
                    return Double.parseDouble(value.replaceAll("[^0-9.]", ""));
                } catch (NumberFormatException e) {
                    return 0.0;
                }
            })
            .sum();

        // 전날 대비 변화량 계산
        double temperatureCompared =
            previousDay != null ? curTemp - previousDay.temperature().current() : 0.0;
        double humidityCompared =
            previousDay != null ? avgHum - previousDay.humidity().current() : 0.0;

        return new WeatherDto(
            UUID.randomUUID(),
            parseDateTime(baseDate, baseTime),
            LocalDateTime.of(date, LocalTime.NOON),
            loc,
            SkyStatus.fromCode(skyCode),
            new PrecipitationDto(precipitationType, precipitation, avgPop),
            new HumidityDto(avgHum, humidityCompared),
            new TemperatureDto(curTemp, temperatureCompared, minTemp, maxTemp),
            new WindSpeedDto(avgWsd, WindStrength.fromSpeed(avgWsd))
        );
    }


    @Override
    @Transactional
    public int cleanupOldWeatherData() {
        return cleanupOldWeatherData(defaultRetentionDays);
    }

    @Override
    @Transactional
    public int cleanupOldWeatherData(int retentionDays) {
        log.info("오래된 날씨 데이터 정리 시작 - 보관 기간: {}일", retentionDays);

        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
            log.debug("삭제 기준 날짜: {}", cutoffDate);

            long deleteCount = weatherRepository.countOldWeatherData(cutoffDate);

            if (deleteCount > 0) {
                log.info("삭제 예정 날씨 데이터: {}건", deleteCount);

                // 삭제(배치)
                weatherRepository.deleteOldWeatherData(cutoffDate);
                log.info("{}일 이전 날씨 데이터 {}건 삭제 완료", retentionDays, deleteCount);
            } else {
                log.info("삭제할 오래된 날씨 데이터가 없습니다");
            }

            return (int) deleteCount;

        } catch (Exception e) {
            log.error("날씨 데이터 정리 중 오류 발생", e);
            throw new RuntimeException("날씨 데이터 정리 실패", e);
        }
    }

    @Override
    @Transactional
    public List<Weather> syncWeatherData(Double longitude, Double latitude) {
        log.info("날씨 데이터 동기화 시작 - 위도: {}, 경도: {}", latitude, longitude);

        validateCoordinates(latitude, longitude);

        try {
            GridCoordinate grid = coordinateConverter.convertToGrid(latitude, longitude);

            // 현재 시간 기준으로 API 호출
            LocalDateTime now = LocalDateTime.now();
            String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String baseTime = getValidBaseTime(now);

            // API 호출
            WeatherApiResponse response = weatherApiClient.getVillageForecast(
                baseDate, baseTime, grid.getX(), grid.getY()
            );

            // 응답 파싱 및 저장
            List<Weather> weathers = parseAndSaveWeatherData(response, grid, longitude, latitude);

            log.info("날씨 데이터 동기화 완료 - {}건 저장", weathers.size());
            return weathers;

        } catch (Exception e) {
            log.error("날씨 데이터 동기화 실패", e);
            throw new WeatherDataFetchException("날씨 데이터 동기화에 실패했습니다.", e);
        }
    }

    @Override
    @Transactional
    public Map<String, List<Weather>> syncMultipleLocations(List<Map<String, Double>> locations) {
        log.info("다중 위치 날씨 데이터 동기화 시작 - {}개 위치", locations.size());

        Map<String, List<Weather>> results = new HashMap<>();

        for (Map<String, Double> location : locations) {
            Double longitude = location.get("longitude");
            Double latitude = location.get("latitude");
            String locationKey = String.format("%.4f,%.4f", latitude, longitude);

            try {
                List<Weather> weathers = syncWeatherData(longitude, latitude);
                results.put(locationKey, weathers);
            } catch (Exception e) {
                log.error("위치 {} 동기화 실패", locationKey, e);
                results.put(locationKey, new ArrayList<>());
            }
        }

        return results;
    }

    @Override
    @Transactional(readOnly = true)
    public WeatherStatistics getWeatherStatistics(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("날씨 통계 조회 - {} ~ {}", startDate, endDate);

        WeatherStatistics statistics = new WeatherStatistics();

        // 날씨 데이터 조회
        List<Weather> weathers = weatherRepository.findWeatherDataBetweenDates(startDate, endDate);

        // 지역별 통계 계산
        Map<String, List<Weather>> weatherByLocation = weathers.stream()
            .collect(Collectors.groupingBy(w ->
                w.getLocation().locationNames().isEmpty() ? "Unknown" : w.getLocation().locationNames().get(0)
            ));

        Map<String, Double> avgTempByLocation = new HashMap<>();
        Map<String, Integer> precipDaysByLocation = new HashMap<>();
        Map<String, Integer> clearDaysByLocation = new HashMap<>();

        weatherByLocation.forEach((location, locationWeathers) -> {
            // 평균 온도
            double avgTemp = locationWeathers.stream()
                .mapToDouble(w -> w.getTemperature().current())
                .average()
                .orElse(0.0);
            avgTempByLocation.put(location, avgTemp);

            // 강수일수
            int precipDays = (int) locationWeathers.stream()
                .filter(w -> w.getPrecipitation().type() != PrecipitationType.NONE)
                .map(w -> w.getForecastAt().toLocalDate())
                .distinct()
                .count();
            precipDaysByLocation.put(location, precipDays);

            // 맑은 날 수
            int clearDays = (int) locationWeathers.stream()
                .filter(w -> w.getSkyStatus() == SkyStatus.CLEAR)
                .map(w -> w.getForecastAt().toLocalDate())
                .distinct()
                .count();
            clearDaysByLocation.put(location, clearDays);
        });

        // 전체 평균 온도
        double overallAvgTemp = weathers.stream()
            .mapToDouble(w -> w.getTemperature().current())
            .average()
            .orElse(0.0);

        statistics.setAverageTemperatureByLocation(avgTempByLocation);
        statistics.setPrecipitationDaysByLocation(precipDaysByLocation);
        statistics.setClearDaysByLocation(clearDaysByLocation);
        statistics.setOverallAverageTemperature(overallAvgTemp);
        statistics.setTotalDataPoints(weathers.size());

        return statistics;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExtremeWeatherInfo> detectExtremeWeatherConditions(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("극한 날씨 조건 감지 - {} ~ {}", startDate, endDate);

        List<ExtremeWeatherInfo> extremeWeathers = new ArrayList<>();

        // 극한 날씨 조건 조회
        List<Weather> weathers = weatherRepository.findExtremeWeatherConditions(
            heatThreshold, coldThreshold, rainThreshold, windThreshold, startDate, endDate
        );

        for (Weather weather : weathers) {
            ExtremeWeatherInfo info = new ExtremeWeatherInfo();
            info.setWeather(weather);
            info.setLocation(weather.getLocation().locationNames().isEmpty() ?
                "Unknown" : weather.getLocation().locationNames().get(0));
            info.setAlertTime(weather.getForecastAt());

            // 경보 유형 판단
            if (weather.getTemperature().max() >= heatThreshold) {
                info.setAlertType("HEAT_WAVE");
                info.setSeverity("WARNING");
            } else if (weather.getTemperature().min() <= coldThreshold) {
                info.setAlertType("COLD_WAVE");
                info.setSeverity("WARNING");
            } else if (weather.getPrecipitation().amount() >= rainThreshold) {
                info.setAlertType("HEAVY_RAIN");
                info.setSeverity("WARNING");
            } else if (weather.getWindSpeed().speed() >= windThreshold) {
                info.setAlertType("STRONG_WIND");
                info.setSeverity("CAUTION");
            }

            extremeWeathers.add(info);
        }

        return extremeWeathers;
    }

    @Override
    public WeatherRecommendationData generateRecommendationData(WeatherCondition weatherCondition) {
        log.info("날씨 추천 데이터 생성 - 조건: {}", weatherCondition);

        WeatherRecommendationData data = new WeatherRecommendationData();
        data.setCondition(weatherCondition);
        data.setGeneratedAt(LocalDateTime.now());

        List<String> recommendedTypes = new ArrayList<>();
        Map<String, Double> scores = new HashMap<>();

        // 온도별 추천
        switch (weatherCondition.getTemperatureRange()) {
            case "VERY_COLD":
                recommendedTypes.addAll(List.of("OUTER", "SCARF", "HAT"));
                scores.put("OUTER", 95.0);
                scores.put("SCARF", 85.0);
                scores.put("HAT", 80.0);
                data.setRecommendationMessage("매우 추운 날씨입니다. 따뜻한 외투와 목도리를 착용하세요.");
                break;
            case "COLD":
                recommendedTypes.addAll(List.of("OUTER", "TOP", "BOTTOM"));
                scores.put("OUTER", 90.0);
                scores.put("TOP", 85.0);
                scores.put("BOTTOM", 85.0);
                data.setRecommendationMessage("추운 날씨입니다. 겉옷을 꼭 챙기세요.");
                break;
            case "MILD":
                recommendedTypes.addAll(List.of("TOP", "BOTTOM"));
                scores.put("TOP", 90.0);
                scores.put("BOTTOM", 90.0);
                data.setRecommendationMessage("온화한 날씨입니다. 가벼운 옷차림이 좋습니다.");
                break;
            case "WARM":
                recommendedTypes.addAll(List.of("TOP", "BOTTOM", "HAT"));
                scores.put("TOP", 85.0);
                scores.put("BOTTOM", 85.0);
                scores.put("HAT", 70.0);
                data.setRecommendationMessage("따뜻한 날씨입니다. 얇은 옷을 추천합니다.");
                break;
            case "HOT":
                recommendedTypes.addAll(List.of("TOP", "BOTTOM", "HAT", "ACCESSORY"));
                scores.put("TOP", 90.0);
                scores.put("BOTTOM", 85.0);
                scores.put("HAT", 80.0);
                scores.put("ACCESSORY", 75.0);
                data.setRecommendationMessage("더운 날씨입니다. 통풍이 잘 되는 옷과 자외선 차단을 위한 모자를 추천합니다.");
                break;
        }

        // 강수 시 추가
        if (weatherCondition.isPrecipitation()) {
            recommendedTypes.add("SHOES");
            scores.put("SHOES", 85.0);
            data.setRecommendationMessage(data.getRecommendationMessage() + " 비가 예상되니 방수 신발을 신으세요.");
        }

        data.setRecommendedClothesTypes(recommendedTypes);
        data.setConfidenceScores(scores);

        return data;
    }

    private LocalDateTime parseDateTime(String date, String time) {
        String t = time.length() < 4
            ? String.format("%04d", Integer.parseInt(time))
            : time;
        String ymdhm = date + t.substring(0, 2) + "00";
        return LocalDateTime.parse(ymdhm, DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }

    private double parseDouble(String v) {
        try {
            return Double.parseDouble(v);
        } catch (Exception e) {
            return 0;
        }
    }

    private void validateCoordinates(Double longitude, Double latitude) {
        if (longitude == null || latitude == null) {
            throw new InvalidCoordinateException("경도와 위도는 필수입니다.");
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

    /**
     * 단기예보용 base_date 계산 (KST 발표시 10분 이후부터 해당 시각)
     */
    private String calculateBaseDate(LocalDateTime now) {
        int[] hours = {23, 20, 17, 14, 11, 8, 5, 2};
        for (int h : hours) {
            LocalDateTime publish = now.withHour(h).withMinute(10).withSecond(0);
            if (!now.isBefore(publish)) {
                return publish.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            }
        }
        return now.minusDays(1).format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    }

    /**
     * 단기예보용 base_time 계산 ("HH00", 발표시각 10분 이후부터)
     */
    private String calculateBaseTime(LocalDateTime now) {
        int[] hours = {23, 20, 17, 14, 11, 8, 5, 2};
        for (int h : hours) {
            LocalDateTime publish = now.withHour(h).withMinute(10).withSecond(0);
            if (!now.isBefore(publish)) {
                return String.format("%02d00", h);
            }
        }
        return "2300";
    }

    private String generateResponseHash(WeatherApiResponse response) {
        try {
            String data = response.toString();
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(data.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (Exception e) {
            return String.valueOf(System.currentTimeMillis());
        }
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

    private List<Weather> parseAndSaveWeatherData(WeatherApiResponse response, GridCoordinate grid,
        Double longitude, Double latitude) {
        List<Weather> savedWeathers = new ArrayList<>();

        // API 응답 파싱 및 Weather 엔티티 생성
        // 실제 구현은 기존 WeatherService의 로직 참고

        return savedWeathers;
    }
}