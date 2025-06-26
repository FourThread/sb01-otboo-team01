package com.fourthread.ozang.module.domain.weather.client;

import com.fourthread.ozang.module.domain.weather.dto.external.WeatherApiResponse;
import com.fourthread.ozang.module.domain.weather.entity.GridCoordinate;
import com.fourthread.ozang.module.domain.weather.exception.WeatherApiException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;


@Component
@RequiredArgsConstructor
@Slf4j
public class WeatherApiClient {

    private final WebClient weatherWebClient;

    @Value("${weather.api.key}")
    private String serviceKey;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HHmm");

    public WeatherApiResponse getWeatherForecast(GridCoordinate coord) {
        LocalDateTime baseDateTime = calculateBaseDateTime();
        String date = baseDateTime.format(DATE_FMT);
        String time = baseDateTime.format(TIME_FMT);

        log.info("단기예보 호출 - date={}, time={}, x={}, y={}", date, time, coord.getX(), coord.getY());

        return weatherWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/getVilageFcst")
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 1000)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", date)
                .queryParam("base_time", time)
                .queryParam("nx", coord.getX())
                .queryParam("ny", coord.getY())
                .build())
            .retrieve()
            .bodyToMono(WeatherApiResponse.class)
            .timeout(Duration.ofSeconds(5))
            .retryWhen(Retry.backoff(2, Duration.ofMillis(500)))
            .blockOptional()
            .orElseThrow(() -> new WeatherApiException("기상청 API 응답이 없습니다", "NO_CONTENT"));
    }

    // 초단기실황 조회 (필요시 사용)
    public WeatherApiResponse getWeatherNowcast(GridCoordinate coord) {
        LocalDateTime now = LocalDateTime.now();
        String date = now.format(DATE_FMT);
        String time = now.withMinute(0).withSecond(0).format(TIME_FMT);

        log.info("실황정보 호출 - date={}, time={}, x={}, y={}", date, time, coord.getX(), coord.getY());

        return weatherWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/getUltraSrtNcst")
                .queryParam("serviceKey", serviceKey)
                .queryParam("pageNo", 1)
                .queryParam("numOfRows", 100)
                .queryParam("dataType", "JSON")
                .queryParam("base_date", date)
                .queryParam("base_time", time)
                .queryParam("nx", coord.getX())
                .queryParam("ny", coord.getY())
                .build())
            .retrieve()
            .bodyToMono(WeatherApiResponse.class)
            .timeout(Duration.ofSeconds(5))
            .retryWhen(Retry.fixedDelay(2, Duration.ofMillis(500)))
            .blockOptional()
            .orElseThrow(() -> new WeatherApiException("기상청 실황 API 응답이 없습니다", "NO_CONTENT"));
    }

    /**
     * 초단기예보 API 제공 시각 계산
     * 제공 시각: [0200,0500,0800,1100,1400,1700,2000,2300] 이후 10분
     */
    private LocalDateTime calculateBaseDateTime() {
        LocalDateTime now = LocalDateTime.now();
        int[] hours = {2,5,8,11,14,17,20,23};
        for (int base : hours) {
            if (now.getHour() > base || (now.getHour()==base && now.getMinute()>=10)) {
                return now.withHour(base).withMinute(0).withSecond(0).withNano(0);
            }
        }
        return now.minusDays(1).withHour(23).withMinute(0).withSecond(0).withNano(0);
    }
}