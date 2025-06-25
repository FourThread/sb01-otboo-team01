package com.fourthread.ozang.module.domain.weather.client;

import com.fourthread.ozang.module.domain.weather.dto.KakaoLocationResponse;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@Slf4j
public class KakaoApiClient {

    private final RestTemplate restTemplate;
    private final String apiKey;
    private final String baseUrl;

    public KakaoApiClient(RestTemplate restTemplate,
        @Value("${kakao.api.key}") String apiKey,
        @Value("${kakao.api.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
    }

    public List<String> getLocationNames(double latitude, double longitude) {
        // =============== Kakao API 키 확인 ===============
        if (apiKey == null || apiKey.isEmpty() || "your_kakao_api_key".equals(apiKey)) {
            log.warn("⚠️ Kakao API 키가 설정되지 않음 - 기본값 반환");
            return List.of("알 수 없는 지역");
        }

        try {
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/v2/local/geo/coord2regioncode.json")
                .queryParam("x", longitude)
                .queryParam("y", latitude)
                .build()
                .toUriString();

            log.info("🗺️ Kakao API 요청: lat={}, lon={}", latitude, longitude);
            log.debug("🔗 Kakao API URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + apiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            KakaoLocationResponse response = restTemplate.exchange(
                url, HttpMethod.GET, entity, KakaoLocationResponse.class
            ).getBody();

            if (response != null && !response.documents().isEmpty()) {
                KakaoLocationResponse.Document doc = response.documents().get(0);

                // =============== null 값 처리 및 안전한 List 생성 ===============
                List<String> locationNames = new ArrayList<>();

                // 각 필드를 안전하게 추가 (null이나 빈 문자열 처리)
                addLocationIfValid(locationNames, doc.region1DepthName());
                addLocationIfValid(locationNames, doc.region2DepthName());
                addLocationIfValid(locationNames, doc.region3DepthName());

                log.info("🏘️ Kakao API 응답: {}", locationNames);

                // 빈 목록인 경우 기본값 반환
                if (locationNames.isEmpty()) {
                    log.warn("⚠️ Kakao API 응답에서 유효한 지역명 없음");
                    return List.of("알 수 없는 지역");
                }

                return locationNames;
            }

            log.warn("⚠️ Kakao API 응답이 비어있음");
            return List.of("알 수 없는 지역");

        } catch (Exception e) {
            log.error("❌ Kakao API 호출 실패: lat={}, lon={}", latitude, longitude, e);
            return List.of("알 수 없는 지역");
        }
    }

    // =============== null과 빈 문자열을 안전하게 처리하는 헬퍼 메소드 ===============
    private void addLocationIfValid(List<String> list, String location) {
        if (location != null && !location.trim().isEmpty()) {
            list.add(location.trim());
        }
    }
}
