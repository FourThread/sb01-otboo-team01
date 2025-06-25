package com.fourthread.ozang.module.domain.weather.client;

import com.fourthread.ozang.module.domain.weather.dto.external.WeatherApiResponse;
import com.fourthread.ozang.module.domain.weather.entity.GridCoordinate;
import com.fourthread.ozang.module.domain.weather.exception.WeatherApiException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
@RequiredArgsConstructor
public class WeatherApiClient {

    private final RestTemplate restTemplate;

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.base-url:http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0}")
    private String baseUrl;

    private static final String FORECAST_ENDPOINT = "/getVilageFcst";
    private static final String NOWCAST_ENDPOINT = "/getUltraSrtNcst";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HHmm");

    //  단기예보 조회
    public WeatherApiResponse getWeatherForecast(GridCoordinate coordinate) {
        LocalDateTime baseDateTime = getBaseDateTime();
        String baseDate = baseDateTime.format(DATE_FORMATTER);
        String baseTime = baseDateTime.format(TIME_FORMATTER);

        log.info("기상청 단기예보 API 호출 - 기준시간: {} {}, 좌표: ({}, {})",
            baseDate, baseTime, coordinate.getX(), coordinate.getY());

        try {
            //  URL 생성
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + FORECAST_ENDPOINT)
                .queryParam("serviceKey", apiKey)
                .queryParam("pageNo", "1")
                .queryParam("numOfRows", "1000")
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", coordinate.getX())
                .queryParam("ny", coordinate.getY())
                .build(true)  // 인코딩 비활성화 (이미 인코딩된 경우)
                .toUriString();

            log.debug("요청 URL: {}", url);

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // API 호출
            ResponseEntity<WeatherApiResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, WeatherApiResponse.class);

            log.info("API 응답 수신 - 상태코드: {}", response.getStatusCode());

            WeatherApiResponse body = response.getBody();
            if (body == null) {
                throw new WeatherApiException("응답 본문이 비어있습니다", "NULL_RESPONSE");
            }

            return body;

        } catch (Exception e) {
            log.error("기상청 API 호출 실패", e);
            throw new WeatherApiException("기상청 API 호출 중 오류 발생: " + e.getMessage(), "API_CALL_ERROR");
        }
    }

    // 실황정보 조회 (필요시 사용)
    public WeatherApiResponse getWeatherNowcast(GridCoordinate coordinate) {
        LocalDateTime now = LocalDateTime.now();
        String baseDate = now.format(DATE_FORMATTER);
        String baseTime = now.withMinute(0).withSecond(0).format(TIME_FORMATTER);

        log.info("기상청 실황정보 API 호출 - 기준시간: {} {}, 좌표: ({}, {})",
            baseDate, baseTime, coordinate.getX(), coordinate.getY());

        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + NOWCAST_ENDPOINT)
                .queryParam("serviceKey", apiKey)
                .queryParam("pageNo", "1")
                .queryParam("numOfRows", "100")
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", coordinate.getX())
                .queryParam("ny", coordinate.getY())
                .build(false)
                .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<WeatherApiResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, WeatherApiResponse.class);

            return response.getBody();

        } catch (Exception e) {
            log.error("기상청 실황 API 호출 실패", e);
            throw new WeatherApiException("기상청 실황 API 호출 중 오류 발생", "NOWCAST_API_ERROR");
        }
    }

    // 기상청 API 발표 시간 계산
    private LocalDateTime getBaseDateTime() {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        int minute = now.getMinute();

        // API 제공 시간: 02:10, 05:10, 08:10, 11:10, 14:10, 17:10, 20:10, 23:10
        int[] baseTimes = {2, 5, 8, 11, 14, 17, 20, 23};

        LocalDateTime baseDateTime = null;

        for (int i = baseTimes.length - 1; i >= 0; i--) {
            int baseTime = baseTimes[i];
            // 현재 시각이 baseTime + 10분 이후인 경우
            if (hour > baseTime || (hour == baseTime && minute >= 10)) {
                baseDateTime = now.withHour(baseTime).withMinute(0).withSecond(0).withNano(0);
                break;
            }
        }

        // 당일 02:10 이전이면 전날 23시 데이터 사용
        if (baseDateTime == null) {
            baseDateTime = now.minusDays(1).withHour(23).withMinute(0).withSecond(0).withNano(0);
        }

        log.debug("기준 시간 계산: 현재 {} -> 기준시간 {}", now, baseDateTime);

        return baseDateTime;
    }
}
