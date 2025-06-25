package com.fourthread.ozang.module.domain.weather.client;

import com.fourthread.ozang.module.domain.weather.dto.WeatherApiResponse;
import com.fourthread.ozang.module.domain.weather.dto.WeatherNowcastResponse;
import com.fourthread.ozang.module.domain.weather.entity.GridCoordinate;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class WeatherApiClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;

    public WeatherApiClient(RestTemplate restTemplate,
        @Value("${weather.api.key}") String apiKey,
        @Value("${weather.api.nowcast.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public WeatherApiResponse getWeatherForecast(GridCoordinate coordinate) {
        LocalDateTime baseDateTime = getBaseDateTime();
        String baseDate = baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = baseDateTime.format(DateTimeFormatter.ofPattern("HHmm"));

        // =============== 성공한 테스트 방식 적용 ===============
        try {
            // 엔드포인트 확인
            String fullUrl = baseUrl.contains("getVilageFcst") ? baseUrl :
                baseUrl.replace("getUltraSrtNcst", "getVilageFcst");

            StringBuilder urlBuilder = new StringBuilder(fullUrl);

            // =============== 이미 인코딩된 키를 추가 인코딩 없이 사용 ===============
            urlBuilder.append("?").append(URLEncoder.encode("serviceKey", StandardCharsets.UTF_8))
                .append("=").append(apiKey); // apiKey는 이미 인코딩됨

            // 나머지 파라미터들은 인코딩
            urlBuilder.append("&").append(URLEncoder.encode("pageNo", StandardCharsets.UTF_8))
                .append("=").append(URLEncoder.encode("1", StandardCharsets.UTF_8));

            urlBuilder.append("&").append(URLEncoder.encode("numOfRows", StandardCharsets.UTF_8))
                .append("=").append(URLEncoder.encode("1000", StandardCharsets.UTF_8));

            // =============== JSON 형식으로 확실히 요청 ===============
            urlBuilder.append("&").append(URLEncoder.encode("dataType", StandardCharsets.UTF_8))
                .append("=").append(URLEncoder.encode("JSON", StandardCharsets.UTF_8));

            // 기준 날짜와 시간
            urlBuilder.append("&").append(URLEncoder.encode("base_date", StandardCharsets.UTF_8))
                .append("=").append(URLEncoder.encode(baseDate, StandardCharsets.UTF_8));

            urlBuilder.append("&").append(URLEncoder.encode("base_time", StandardCharsets.UTF_8))
                .append("=").append(URLEncoder.encode(baseTime, StandardCharsets.UTF_8));

            // 좌표
            urlBuilder.append("&").append(URLEncoder.encode("nx", StandardCharsets.UTF_8))
                .append("=").append(URLEncoder.encode(coordinate.getX().toString(), StandardCharsets.UTF_8));

            urlBuilder.append("&").append(URLEncoder.encode("ny", StandardCharsets.UTF_8))
                .append("=").append(URLEncoder.encode(coordinate.getY().toString(), StandardCharsets.UTF_8));

            String url = urlBuilder.toString();
            log.info("🌤️ 기상청 API 요청 URL: {}", url);

            // =============== 더 안전한 HTTP 호출 방식 사용 ===============
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-type", "application/json");  // 샘플과 동일
            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("📡 기상청 API 호출 시작...");

            ResponseEntity<WeatherApiResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, WeatherApiResponse.class);

            log.info("📡 응답 상태: {}", response.getStatusCode());
            log.info("📝 Content-Type: {}", response.getHeaders().getContentType());

            WeatherApiResponse weatherResponse = response.getBody();

            if (weatherResponse != null) {
                // XML 구조에 맞춰 접근 방식 수정
                if (weatherResponse.header() != null) {
                    log.info("✅ 응답 파싱 성공 - 결과코드: {}",
                        weatherResponse.header().resultCode());
                } else if (weatherResponse.response() != null && weatherResponse.response().header() != null) {
                    log.info("✅ 응답 파싱 성공 - 결과코드: {}",
                        weatherResponse.response().header().resultCode());
                }
            }

            return weatherResponse;

        } catch (Exception e) {
            log.error("❌ 기상청 API 호출 실패", e);
            log.error("🔍 좌표: x={}, y={}", coordinate.getX(), coordinate.getY());
            log.error("🔍 날짜/시간: {} {}", baseDate, baseTime);
            throw e;
        }
    }

    private LocalDateTime getBaseDateTime() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();

        // API 제공 시간에 맞춰 조정 (02, 05, 08, 11, 14, 17, 20, 23시)
        int[] apiTimes = {2, 5, 8, 11, 14, 17, 20, 23};
        int baseHour = 23; // 기본값

        for (int time : apiTimes) {
            if (hour >= time) {
                baseHour = time;
            }
        }

        if (hour < 2) {
            return now.minusDays(1).withHour(23).withMinute(0).withSecond(0).withNano(0);
        }

        return now.withHour(baseHour).withMinute(0).withSecond(0).withNano(0);
    }
}
//@Component
//@Slf4j
//public class WeatherApiClient {
//
//    private final RestTemplate restTemplate;
//    private final String apiKey;
//    private final String forecastBaseUrl;
//    private final String nowcastBaseUrl;
//
//    public WeatherApiClient(RestTemplate restTemplate,
//        @Value("${weather.api.key}") String apiKey,
//        @Value("${weather.api.forecast.url}") String forecastBaseUrl,
//        @Value("${weather.api.nowcast.url}") String nowcastBaseUrl) {
//        this.restTemplate = restTemplate;
//        this.apiKey = apiKey;
//        this.forecastBaseUrl = forecastBaseUrl;
//        this.nowcastBaseUrl = nowcastBaseUrl;
//    }
//
//    /**
//     * 단기예보 조회
//     */
//    public WeatherApiResponse getWeatherForecast(GridCoordinate coordinate) {
//        LocalDateTime baseDateTime = getForecastBaseDateTime();
//        String baseDate = baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//        String baseTime = baseDateTime.format(DateTimeFormatter.ofPattern("HHmm"));
//
//        String url = UriComponentsBuilder.fromHttpUrl(forecastBaseUrl)
//            .queryParam("serviceKey", apiKey)
//            .queryParam("pageNo", "1")
//            .queryParam("numOfRows", "1000")
//            .queryParam("dataType", "JSON")
//            .queryParam("base_date", baseDate)
//            .queryParam("base_time", baseTime)
//            .queryParam("nx", coordinate.getX())
//            .queryParam("ny", coordinate.getY())
//            .build()
//            .toUriString();
//
//        log.debug("Forecast API request: {}", url);
//        return restTemplate.getForObject(url, WeatherApiResponse.class);
//    }
//
//    /**
//     * 실황정보 조회
//     */
//    public WeatherNowcastResponse getWeatherNowcast(GridCoordinate coordinate) {
//        LocalDateTime baseDateTime = getNowcastBaseDateTime();
//        String baseDate = baseDateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
//        String baseTime = baseDateTime.format(DateTimeFormatter.ofPattern("HHmm"));
//
//        String url = UriComponentsBuilder.fromHttpUrl(nowcastBaseUrl)
//            .queryParam("serviceKey", apiKey)
//            .queryParam("pageNo", "1")
//            .queryParam("numOfRows", "1000")
//            .queryParam("dataType", "JSON")
//            .queryParam("base_date", baseDate)
//            .queryParam("base_time", baseTime)
//            .queryParam("nx", coordinate.getX())
//            .queryParam("ny", coordinate.getY())
//            .build()
//            .toUriString();
//
//        log.debug("Nowcast API request: {}", url);
//        return restTemplate.getForObject(url, WeatherNowcastResponse.class);
//    }
//
//    private LocalDateTime getForecastBaseDateTime() {
//        LocalDateTime now = LocalDateTime.now();
//        int hour = now.getHour();
//
//        // 단기예보 API 제공 시간에 맞춰 조정 (02, 05, 08, 11, 14, 17, 20, 23시)
//        int[] apiTimes = {2, 5, 8, 11, 14, 17, 20, 23};
//        int baseHour = 23; // 기본값
//
//        for (int time : apiTimes) {
//            if (hour >= time) {
//                baseHour = time;
//            }
//        }
//
//        if (hour < 2) {
//            return now.minusDays(1).withHour(23).withMinute(0).withSecond(0).withNano(0);
//        }
//
//        return now.withHour(baseHour).withMinute(0).withSecond(0).withNano(0);
//    }
//
//    private LocalDateTime getNowcastBaseDateTime() {
//        LocalDateTime now = LocalDateTime.now();
//        // 실황정보는 매시 정각에 발표되므로 현재 시간의 정각 사용
//        return now.withMinute(0).withSecond(0).withNano(0);
//    }
//}