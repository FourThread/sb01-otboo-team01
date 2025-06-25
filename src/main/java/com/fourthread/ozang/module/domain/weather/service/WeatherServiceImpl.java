package com.fourthread.ozang.module.domain.weather.service.impl;

import com.fourthread.ozang.module.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.module.domain.weather.dto.data.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.external.WeatherApiBody;
import com.fourthread.ozang.module.domain.weather.dto.external.WeatherApiHeader;
import com.fourthread.ozang.module.domain.weather.dto.external.WeatherApiItem;
import com.fourthread.ozang.module.domain.weather.dto.external.WeatherApiResponse;
import com.fourthread.ozang.module.domain.weather.dto.type.PrecipitationType;
import com.fourthread.ozang.module.domain.weather.dto.type.SkyStatus;
import com.fourthread.ozang.module.domain.weather.dto.type.WindStrength;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import com.fourthread.ozang.module.domain.weather.exception.InvalidCoordinateException;
import com.fourthread.ozang.module.domain.weather.exception.WeatherApiException;
import com.fourthread.ozang.module.domain.weather.exception.WeatherDataFetchException;
import com.fourthread.ozang.module.domain.weather.mapper.WeatherMapper;
import com.fourthread.ozang.module.domain.weather.repository.WeatherRepository;
import com.fourthread.ozang.module.domain.weather.service.WeatherService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
@RequiredArgsConstructorgit merge taehyoun/feat/#8/weather
@Transactional(readOnly = true)
public class WeatherServiceImpl implements WeatherService {

    private final WeatherRepository weatherRepository;
    private final WeatherMapper weatherMapper;
    private final RestTemplate restTemplate;

    @Value("${weather.api.key:sample_key}")
    private String apiKey;

    @Value("${weather.api.base-url:http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0}")
    private String baseUrl;

    private static final String FORECAST_ENDPOINT = "/getVilageFcst";
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");

    @Override
    @Transactional
    public WeatherDto getWeatherForecast(Double longitude, Double latitude) {
        log.debug("Getting weather forecast for longitude: {}, latitude: {}", longitude, latitude);

        validateCoordinates(longitude, latitude);

        // 기존 데이터 확인 (1시간 이내)
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Optional<Weather> existingWeather = weatherRepository.findLatestWeatherByLocation(latitude, longitude);

        if (existingWeather.isPresent() && existingWeather.get().getForecastedAt().isAfter(oneHourAgo)) {
            log.debug("Using cached weather data");
            return weatherMapper.toDto(existingWeather.get());
        }

        // 좌표 변환
        Map<String, Integer> gridCoords = convertToGrid(latitude, longitude);
        int nx = gridCoords.get("nx");
        int ny = gridCoords.get("ny");

        // 외부 API 호출 및 데이터 저장
        Weather weather = fetchAndSaveWeatherData(latitude, longitude, nx, ny);

        return weatherMapper.toDto(weather);
    }

    @Override
    public WeatherAPILocation getWeatherLocation(Double longitude, Double latitude) {
        log.debug("Getting weather location for longitude: {}, latitude: {}", longitude, latitude);

        validateCoordinates(longitude, latitude);

        Map<String, Integer> gridCoords = convertToGrid(latitude, longitude);
        List<String> locationNames = getLocationNames(latitude, longitude);

        return weatherMapper.toWeatherAPILocation(
            latitude, longitude,
            gridCoords.get("nx"), gridCoords.get("ny"),
            locationNames
        );
    }

    private void validateCoordinates(Double longitude, Double latitude) {
        if (longitude == null || latitude == null) {
            throw new InvalidCoordinateException("경도와 위도는 필수입니다.");
        }

        // 한국 영토 범위 대략적 검증
        if (latitude < 33.0 || latitude > 43.0 || longitude < 124.0 || longitude > 132.0) {
            throw new InvalidCoordinateException("한국 영토 범위를 벗어난 좌표입니다.");
        }
    }

    @Transactional
    private Weather fetchAndSaveWeatherData(Double latitude, Double longitude, int nx, int ny) {
        try {
            WeatherApiResponse apiResponse = callWeatherApi(nx, ny);

            if (apiResponse == null || apiResponse.response() == null) {
                throw new WeatherDataFetchException("API 응답이 null입니다.");
            }

            WeatherApiHeader header = apiResponse.response().header();
            if (header == null || !"00".equals(header.resultCode())) {
                String errorMsg = header != null ? header.resultMsg() : "Unknown error";
                String resultCode = header != null ? header.resultCode() : "UNKNOWN";
                throw new WeatherApiException("API 호출 실패: " + errorMsg, resultCode);
            }

            WeatherApiBody body = apiResponse.response().body();
            if (body == null || body.items() == null || body.items().item() == null || body.items().item().isEmpty()) {
                throw new WeatherDataFetchException("No result code and no items in API response");
            }

            List<WeatherApiItem> items = body.items().item();
            Weather weather = parseWeatherData(items, latitude, longitude, nx, ny);

            return weatherRepository.save(weather);

        } catch (WeatherApiException | WeatherDataFetchException e) {
            throw e;
        } catch (Exception e) {
            log.error("Weather data fetch failed", e);
            throw new WeatherDataFetchException("Weather data fetch failed", e);
        }
    }

    private WeatherApiResponse callWeatherApi(int nx, int ny) {
        LocalDateTime now = LocalDateTime.now();
        String baseDate = now.format(DATE_FORMATTER);
        String baseTime = getBaseTime(now);

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + FORECAST_ENDPOINT)
            .queryParam("serviceKey", apiKey)
            .queryParam("numOfRows", "300")
            .queryParam("pageNo", "1")
            .queryParam("dataType", "JSON")
            .queryParam("base_date", baseDate)
            .queryParam("base_time", baseTime)
            .queryParam("nx", nx)
            .queryParam("ny", ny)
            .build()
            .toUriString();

        log.debug("Calling weather API: {}", url);

        try {
            return restTemplate.getForObject(url, WeatherApiResponse.class);
        } catch (Exception e) {
            log.error("Failed to call weather API", e);
            throw new WeatherDataFetchException("외부 날씨 API 호출 실패", e);
        }
    }

    private String getBaseTime(LocalDateTime dateTime) {
        int hour = dateTime.getHour();

        // 기상청 API 발표시간: 02, 05, 08, 11, 14, 17, 20, 23시
        int[] baseTimes = {23, 20, 17, 14, 11, 8, 5, 2};

        for (int baseTime : baseTimes) {
            if (hour >= baseTime) {
                return String.format("%02d00", baseTime);
            }
        }

        // 만약 현재 시간이 02시 이전이라면 전날 23시 데이터 사용
        return "2300";
    }

    private Weather parseWeatherData(List<WeatherApiItem> items, Double latitude, Double longitude, int nx, int ny) {
        Map<String, String> weatherData = items.stream()
            .filter(item -> item.fcstDate() != null && item.fcstTime() != null)
            .collect(Collectors.toMap(
                item -> item.category(),
                item -> item.fcstValue(),
                (existing, replacement) -> replacement // 중복 시 최신 값 사용
            ));

        LocalDateTime forecastedAt = LocalDateTime.now();
        LocalDateTime forecastAt = parseDateTime(items.get(0).fcstDate(), items.get(0).fcstTime());

        // 온도 정보
        double currentTemp = Double.parseDouble(weatherData.getOrDefault("TMP", "0"));
        double minTemp = Double.parseDouble(weatherData.getOrDefault("TMN", String.valueOf(currentTemp)));
        double maxTemp = Double.parseDouble(weatherData.getOrDefault("TMX", String.valueOf(currentTemp)));

        // 습도
        double humidity = Double.parseDouble(weatherData.getOrDefault("REH", "50"));

        // 강수량 및 확률
        double precipitationAmount = Double.parseDouble(weatherData.getOrDefault("PCP", "0"));
        double precipitationProbability = Double.parseDouble(weatherData.getOrDefault("POP", "0"));

        // 풍속
        double windSpeed = Double.parseDouble(weatherData.getOrDefault("WSD", "0"));

        // 하늘 상태
        SkyStatus skyStatus = parseSkyStatus(weatherData.getOrDefault("SKY", "1"));

        // 강수 형태
        PrecipitationType precipitationType = parsePrecipitationType(weatherData.getOrDefault("PTY", "0"));

        List<String> locationNames = getLocationNames(latitude, longitude);

        return new Weather(
            forecastedAt, forecastAt,
            latitude, longitude, nx, ny, String.join(",", locationNames),
            skyStatus, precipitationType,
            precipitationAmount, precipitationProbability,
            currentTemp, 0.0, minTemp, maxTemp, // 전날 대비 온도는 별도 계산 필요
            humidity, 0.0, // 전날 대비 습도는 별도 계산 필요
            windSpeed, WindStrength.fromSpeed(windSpeed)
        );
    }

    private LocalDateTime parseDateTime(String date, String time) {
        try {
            String dateTimeStr = date + time;
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm");
            return LocalDateTime.parse(dateTimeStr, formatter);
        } catch (Exception e) {
            log.warn("Failed to parse date time: {} {}", date, time);
            return LocalDateTime.now().plusHours(1);
        }
    }

    private SkyStatus parseSkyStatus(String skyCode) {
        return switch (skyCode) {
            case "1" -> SkyStatus.CLEAR;
            case "3" -> SkyStatus.MOSTLY_CLOUDY;
            case "4" -> SkyStatus.CLOUDY;
            default -> SkyStatus.CLEAR;
        };
    }

    private PrecipitationType parsePrecipitationType(String ptyCode) {
        return switch (ptyCode) {
            case "0" -> PrecipitationType.NONE;
            case "1" -> PrecipitationType.RAIN;
            case "2" -> PrecipitationType.RAIN_SNOW;
            case "3" -> PrecipitationType.SNOW;
            case "4" -> PrecipitationType.SHOWER;
            default -> PrecipitationType.NONE;
        };
    }

    /**
     * 위경도를 기상청 격자 좌표로 변환
     * Lambert Conformal Conic 투영법 사용
     */
    private Map<String, Integer> convertToGrid(double lat, double lon) {
        double RE = 6371.00877; // 지구 반경(km)
        double GRID = 5.0; // 격자 간격(km)
        double SLAT1 = 30.0; // 투영 위도1(degree)
        double SLAT2 = 60.0; // 투영 위도2(degree)
        double OLON = 126.0; // 기준점 경도(degree)
        double OLAT = 38.0; // 기준점 위도(degree)
        double XO = 43; // 기준점 X좌표(GRID)
        double YO = 136; // 기준점 Y좌표(GRID)

        double DEGRAD = Math.PI / 180.0;
        double RADDEG = 180.0 / Math.PI;

        double re = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon = OLON * DEGRAD;
        double olat = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + (lat) * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int x = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int y = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);

        Map<String, Integer> result = new HashMap<>();
        result.put("nx", x);
        result.put("ny", y);
        return result;
    }

    private List<String> getLocationNames(Double latitude, Double longitude) {
        // 실제로는 역지오코딩 API를 호출하거나 지명 데이터베이스를 조회
        // 여기서는 간단한 더미 데이터 반환
        return Arrays.asList("서울특별시", "종로구", "청와대로");
    }
}