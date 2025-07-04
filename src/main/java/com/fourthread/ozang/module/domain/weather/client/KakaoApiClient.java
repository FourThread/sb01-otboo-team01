package com.fourthread.ozang.module.domain.weather.client;

import com.fourthread.ozang.module.domain.weather.dto.external.KakaoLocalResponse;
import com.fourthread.ozang.module.domain.weather.exception.WeatherApiException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.util.retry.Retry;


@Component
@RequiredArgsConstructor
@Slf4j
public class KakaoApiClient {

    private final WebClient kakaoWebClient;

    public List<String> getLocationNames(double latitude, double longitude) {
        log.info("카카오 로컬 API 호출 - lat={}, lon={}", latitude, longitude);

        KakaoLocalResponse response = kakaoWebClient.get()
            .uri(uriBuilder -> uriBuilder
                .path("/v2/local/geo/coord2regioncode.json")
                .queryParam("x", longitude)
                .queryParam("y", latitude)
                .build())
            .retrieve()
            .bodyToMono(KakaoLocalResponse.class)
            .timeout(Duration.ofSeconds(3))
            .retryWhen(Retry.fixedDelay(1, Duration.ofMillis(300)))
            .block();

        if (response == null || response.documents().isEmpty()) {
            throw new WeatherApiException("카카오 API 지역코드 응답 없음", "KAKAO_NO_CONTENT");
        }

//        return response.documents().stream()
//            .map(doc -> doc.region2DepthName() + " " + doc.region3DepthName())
//            .collect(Collectors.toList());

        return response.documents().stream()
            .map(d -> List.of(
                d.region1DepthName(),         // 시·도
                d.region2DepthName(),         // 구
                d.region3DepthName()))        // 동
            .flatMap(List::stream)
            .filter(n -> n != null && !n.isBlank())
            .distinct()                          // ★ 중복 제거
            .toList();
    }

}