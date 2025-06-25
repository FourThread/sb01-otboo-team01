package com.fourthread.ozang.module.domain.weather.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class KakaoApiClient {

    private final RestTemplate restTemplate;

    @Value("${kakao.api.key}")
    private String apiKey;

    @Value("${kakao.api.url}")
    private String baseUrl;

    private static final String COORD_TO_REGION_ENDPOINT = "/v2/local/geo/coord2regioncode.json";

    //  좌표를 행정구역 정보로 변환
    public List<String> getLocationNames(double latitude, double longitude) {
        //  API 키 확인
        if (!isApiKeyValid()) {
            log.warn("⚠️ 카카오 API 키가 설정되지 않음 - 기본값 반환");
            return getDefaultLocationName(latitude, longitude);
        }

        try {
            //  URL 생성
            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + COORD_TO_REGION_ENDPOINT)
                .queryParam("x", longitude)  // 경도
                .queryParam("y", latitude)   // 위도
                .queryParam("input_coord", "WGS84")
                .queryParam("output_coord", "WGS84")
                .build()
                .toUriString();

            log.info("🗺️ 카카오 API 호출 - 위도: {}, 경도: {}", latitude, longitude);

            //  HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + apiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            //  API 호출
            ResponseEntity<KakaoLocationResponse> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, KakaoLocationResponse.class);

            if (response.getBody() != null && response.getBody().documents() != null
                && !response.getBody().documents().isEmpty()) {

                KakaoLocationResponse.Document doc = response.getBody().documents().get(0);
                List<String> locationNames = new ArrayList<>();

                // 지역명 추출 (null 체크)
                if (doc.region1DepthName() != null && !doc.region1DepthName().trim().isEmpty()) {
                    locationNames.add(doc.region1DepthName());
                }
                if (doc.region2DepthName() != null && !doc.region2DepthName().trim().isEmpty()) {
                    locationNames.add(doc.region2DepthName());
                }
                if (doc.region3DepthName() != null && !doc.region3DepthName().trim().isEmpty()) {
                    locationNames.add(doc.region3DepthName());
                }

                log.info("✅ 카카오 API 응답: {}", locationNames);

                return locationNames.isEmpty() ? getDefaultLocationName(latitude, longitude) : locationNames;
            }

            log.warn("⚠카카오 API 응답이 비어있음");
            return getDefaultLocationName(latitude, longitude);

        } catch (Exception e) {
            log.error("카카오 API 호출 실패", e);
            return getDefaultLocationName(latitude, longitude);
        }
    }

    // API 키 유효성 확인
    private boolean isApiKeyValid() {
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("${kakao.api.key}");
    }

    // 기본 지역명 반환
    private List<String> getDefaultLocationName(double latitude, double longitude) {
        // 주요 도시 좌표 확인하여 대략적인 지역명 반환
        if (isNearLocation(latitude, longitude, 37.5665, 126.9780, 0.1)) {
            return List.of("서울특별시");
        } else if (isNearLocation(latitude, longitude, 35.1796, 129.0756, 0.1)) {
            return List.of("부산광역시");
        } else if (isNearLocation(latitude, longitude, 35.8714, 128.6014, 0.1)) {
            return List.of("대구광역시");
        } else if (isNearLocation(latitude, longitude, 37.4563, 126.7052, 0.1)) {
            return List.of("인천광역시");
        } else if (isNearLocation(latitude, longitude, 35.1595, 126.8526, 0.1)) {
            return List.of("광주광역시");
        } else if (isNearLocation(latitude, longitude, 36.3504, 127.3845, 0.1)) {
            return List.of("대전광역시");
        } else if (isNearLocation(latitude, longitude, 35.5384, 129.3114, 0.1)) {
            return List.of("울산광역시");
        } else if (isNearLocation(latitude, longitude, 33.4996, 126.5312, 0.1)) {
            return List.of("제주특별자치도");
        }

        return List.of("대한민국");
    }

    //  좌표 근접 확인
    private boolean isNearLocation(double lat1, double lon1, double lat2, double lon2, double threshold) {
        double distance = Math.sqrt(Math.pow(lat1 - lat2, 2) + Math.pow(lon1 - lon2, 2));
        return distance < threshold;
    }
}
record KakaoLocationResponse(
    @JsonProperty("meta") Meta meta,
    @JsonProperty("documents") List<Document> documents
) {
    record Meta(
        @JsonProperty("total_count") Integer totalCount
    ) {}

    record Document(
        @JsonProperty("region_type") String regionType,
        @JsonProperty("address_name") String addressName,
        @JsonProperty("region_1depth_name") String region1DepthName,
        @JsonProperty("region_2depth_name") String region2DepthName,
        @JsonProperty("region_3depth_name") String region3DepthName,
        @JsonProperty("region_4depth_name") String region4DepthName,
        @JsonProperty("code") String code,
        @JsonProperty("x") Double x,
        @JsonProperty("y") Double y
    ) {}
}
