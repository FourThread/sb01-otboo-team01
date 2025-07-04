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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WeatherServiceImpl implements WeatherService {

    private final WeatherRepository weatherRepository;
    private final WeatherMapper weatherMapper;
    private final WeatherApiClient weatherApiClient;
    private final KakaoApiClient kakaoApiClient;
    private final CoordinateConverter coordinateConverter;

    @Value("${batch.weather.retention-days:30}")
    private int defaultRetentionDays;

    @Override
    @Transactional
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

    @Transactional
    protected Weather fetchAndSaveWeatherData(Double latitude, Double longitude,
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

        WeatherApiResponse resp = weatherApiClient.callVilageFcst(
            grid, baseDate, baseTime
        );

        validateApiResponse(resp);

        List<String> locationNames = kakaoApiClient.getLocationNames(latitude, longitude);

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
            .mapToDouble(i -> parseDouble(i.fcstValue()))
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
}