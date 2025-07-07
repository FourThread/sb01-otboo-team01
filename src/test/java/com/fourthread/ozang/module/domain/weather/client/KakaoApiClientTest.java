package com.fourthread.ozang.module.domain.weather.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fourthread.ozang.module.domain.weather.exception.WeatherApiException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

@DisplayName("Kakao API Client 테스트")
class KakaoApiClientTest {

    private KakaoApiClient kakaoApiClient;
    private MockWebServer mockWebServer;
    private static final double TEST_LATITUDE = 37.5665;
    private static final double TEST_LONGITUDE = 126.9780;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .defaultHeader("Authorization", "KakaoAK test-key")
            .build();

        kakaoApiClient = new KakaoApiClient(webClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Disabled
    @Nested
    @DisplayName("getLocationNames 메서드 테스트")
    class GetLocationNamesTest {

        @Test
        @DisplayName("빈 응답 처리")
        void getLocationNames_EmptyResponse() {
            // Given
            String emptyResponse = """
                {
                    "meta": {
                        "total_count": 0,
                        "pageable_count": 0,
                        "is_end": true
                    },
                    "documents": []
                }
                """;
            mockWebServer.enqueue(new MockResponse()
                .setBody(emptyResponse)
                .setHeader("Content-Type", "application/json"));

            // When & Then
            assertThatThrownBy(() -> kakaoApiClient.getLocationNames(TEST_LATITUDE, TEST_LONGITUDE))
                .isInstanceOf(WeatherApiException.class)
                .hasMessage("카카오 API 지역코드 응답 없음")
                .satisfies(e -> {
                    WeatherApiException wae = (WeatherApiException) e;
                    assertThat(wae.getResultCode()).isEqualTo("KAKAO_NO_CONTENT");
                });
        }

        @Test
        @DisplayName("null 응답 처리")
        void getLocationNames_NullResponse() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setBody("")
                .setResponseCode(204));

            // When & Then
            assertThatThrownBy(() -> kakaoApiClient.getLocationNames(TEST_LATITUDE, TEST_LONGITUDE))
                .isInstanceOf(WeatherApiException.class)
                .hasMessage("카카오 API 지역코드 응답 없음");
        }

        @Test
        @DisplayName("타임아웃 처리")
        void getLocationNames_Timeout() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setBody(createSuccessResponse())
                .setBodyDelay(4, TimeUnit.SECONDS)); // 4초 지연

            // When & Then
            assertThatThrownBy(() -> kakaoApiClient.getLocationNames(TEST_LATITUDE, TEST_LONGITUDE))
                .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("재시도 메커니즘 테스트")
        void getLocationNames_RetryMechanism() throws Exception {
            // Given
            // 첫 번째 요청은 실패
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
            // 두 번째 요청은 성공
            mockWebServer.enqueue(new MockResponse()
                .setBody(createSuccessResponse())
                .setHeader("Content-Type", "application/json"));

            // When
            List<String> locationNames = kakaoApiClient.getLocationNames(TEST_LATITUDE, TEST_LONGITUDE);

            // Then
            assertThat(locationNames).hasSize(3);
            assertThat(mockWebServer.getRequestCount()).isEqualTo(2); // 2번 요청됨
        }

        @Test
        @DisplayName("특수 지역명 처리")
        void getLocationNames_SpecialLocationNames() throws Exception {
            // Given
            String specialResponse = """
                {
                    "meta": {
                        "total_count": 2,
                        "pageable_count": 2,
                        "is_end": true
                    },
                    "documents": [
                        {
                            "region_type": "H",
                            "region_1depth_name": "서울특별시",
                            "region_2depth_name": "중구",
                            "region_3depth_name": "",
                            "region_4depth_name": "",
                            "code": "1114000000",
                            "x": 126.9780,
                            "y": 37.5665
                        },
                        {
                            "region_type": "B",
                            "region_1depth_name": "서울특별시",
                            "region_2depth_name": "중구",
                            "region_3depth_name": "명동",
                            "region_4depth_name": "",
                            "code": "1114063000",
                            "x": 126.9780,
                            "y": 37.5665
                        }
                    ]
                }
                """;
            mockWebServer.enqueue(new MockResponse()
                .setBody(specialResponse)
                .setHeader("Content-Type", "application/json"));

            // When
            List<String> locationNames = kakaoApiClient.getLocationNames(TEST_LATITUDE, TEST_LONGITUDE);

            // Then
            assertThat(locationNames).hasSize(2);
            assertThat(locationNames).containsExactly(
                "중구 ",  // region_3depth_name이 빈 문자열인 경우
                "중구 명동"
            );
        }
    }

    @Disabled
    @Nested
    @DisplayName("응답 파싱 테스트")
    class ResponseParsingTest {

        @Test
        @DisplayName("다양한 region_type 처리")
        void parseResponse_VariousRegionTypes() throws Exception {
            // Given
            String response = """
                {
                    "meta": {
                        "total_count": 3,
                        "pageable_count": 3,
                        "is_end": true
                    },
                    "documents": [
                        {
                            "region_type": "H",
                            "region_1depth_name": "서울특별시",
                            "region_2depth_name": "중구",
                            "region_3depth_name": "명동",
                            "code": "1114063000",
                            "x": 126.9780,
                            "y": 37.5665
                        },
                        {
                            "region_type": "B",
                            "region_1depth_name": "서울특별시",
                            "region_2depth_name": "중구",
                            "region_3depth_name": "회현동",
                            "code": "1114064000",
                            "x": 126.9780,
                            "y": 37.5665
                        },
                        {
                            "region_type": "A",
                            "region_1depth_name": "서울특별시",
                            "region_2depth_name": "중구",
                            "region_3depth_name": "남대문로",
                            "code": "1114000000",
                            "x": 126.9780,
                            "y": 37.5665
                        }
                    ]
                }
                """;
            mockWebServer.enqueue(new MockResponse()
                .setBody(response)
                .setHeader("Content-Type", "application/json"));

            // When
            List<String> locationNames = kakaoApiClient.getLocationNames(TEST_LATITUDE, TEST_LONGITUDE);

            // Then
            assertThat(locationNames).hasSize(3);
            assertThat(locationNames).allMatch(name -> name.contains("중구"));
        }
    }

    @Nested
    @DisplayName("에러 처리 테스트")
    class ErrorHandlingTest {

        @Test
        @DisplayName("4xx 에러 처리")
        void handleClientError() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(401)
                .setBody("""
                    {
                        "msg": "authentication failed",
                        "code": -401
                    }
                    """));

            // When & Then
            assertThatThrownBy(() -> kakaoApiClient.getLocationNames(TEST_LATITUDE, TEST_LONGITUDE))
                .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("5xx 에러 처리")
        void handleServerError() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setBody("Internal Server Error"));

            // When & Then
            assertThatThrownBy(() -> kakaoApiClient.getLocationNames(TEST_LATITUDE, TEST_LONGITUDE))
                .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("잘못된 JSON 응답 처리")
        void handleInvalidJson() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setBody("invalid json")
                .setHeader("Content-Type", "application/json"));

            // When & Then
            assertThatThrownBy(() -> kakaoApiClient.getLocationNames(TEST_LATITUDE, TEST_LONGITUDE))
                .isInstanceOf(Exception.class);
        }
    }

    private String createSuccessResponse() {
        return """
            {
                "meta": {
                    "total_count": 3,
                    "pageable_count": 3,
                    "is_end": true
                },
                "documents": [
                    {
                        "region_type": "H",
                        "address_name": "서울특별시 중구",
                        "region_1depth_name": "서울특별시",
                        "region_2depth_name": "중구",
                        "region_3depth_name": "",
                        "region_4depth_name": "",
                        "code": "1114000000",
                        "x": 126.9780,
                        "y": 37.5665
                    },
                    {
                        "region_type": "B",
                        "address_name": "서울특별시 중구 명동",
                        "region_1depth_name": "서울특별시",
                        "region_2depth_name": "중구",
                        "region_3depth_name": "명동",
                        "region_4depth_name": "",
                        "code": "1114063000",
                        "x": 126.9780,
                        "y": 37.5665
                    },
                    {
                        "region_type": "B",
                        "address_name": "서울특별시 중구 회현동",
                        "region_1depth_name": "서울특별시",
                        "region_2depth_name": "중구",
                        "region_3depth_name": "회현동",
                        "region_4depth_name": "",
                        "code": "1114064000",
                        "x": 126.9780,
                        "y": 37.5665
                    }
                ]
            }
            """;
    }
}