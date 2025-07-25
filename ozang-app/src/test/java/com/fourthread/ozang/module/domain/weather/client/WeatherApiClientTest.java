package com.fourthread.ozang.module.domain.weather.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fourthread.ozang.module.domain.weather.dto.external.WeatherApiResponse;
import com.fourthread.ozang.module.domain.weather.entity.GridCoordinate;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

@DisplayName("Weather API Client 테스트")
class WeatherApiClientTest {

    private WeatherApiClient weatherApiClient;
    private MockWebServer mockWebServer;
    private static final String SERVICE_KEY = "test-service-key";
    private static final GridCoordinate TEST_GRID = new GridCoordinate(60, 127);

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();

        weatherApiClient = new WeatherApiClient(webClient);
        ReflectionTestUtils.setField(weatherApiClient, "serviceKey", SERVICE_KEY);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    @Nested
    @DisplayName("getWeatherForecast 메서드 테스트")
    class GetWeatherForecastTest {

        @Test
        @DisplayName("정상적인 날씨 예보 조회")
        void getWeatherForecast_Success() throws Exception {
            // Given
            String mockResponse = createSuccessResponse();
            mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .setHeader("Content-Type", "application/json"));

            // When
            WeatherApiResponse response = weatherApiClient.getWeatherForecast(TEST_GRID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.response().header().resultCode()).isEqualTo("00");
            assertThat(response.response().body().items().item()).hasSize(3);

            // 요청 검증
            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getPath())
                .contains("/getVilageFcst")
                .contains("serviceKey=" + SERVICE_KEY)
                .contains("nx=60")
                .contains("ny=127");
        }

        @Test
        @DisplayName("타임아웃 발생 시 예외 처리")
        void getWeatherForecast_Timeout() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setBody(createSuccessResponse())
                .setBodyDelay(6, TimeUnit.SECONDS)); // 6초 지연

            // When & Then
            assertThatThrownBy(() -> weatherApiClient.getWeatherForecast(TEST_GRID))
                .isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("재시도 메커니즘 테스트")
        void getWeatherForecast_RetryMechanism() throws Exception {
            // Given
            // 첫 번째, 두 번째 요청은 실패
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
            mockWebServer.enqueue(new MockResponse().setResponseCode(500));
            // 세 번째 요청은 성공
            mockWebServer.enqueue(new MockResponse()
                .setBody(createSuccessResponse())
                .setHeader("Content-Type", "application/json"));

            // When
            WeatherApiResponse response = weatherApiClient.getWeatherForecast(TEST_GRID);

            // Then
            assertThat(response).isNotNull();
            assertThat(mockWebServer.getRequestCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("base_date와 base_time 계산 검증")
        void getWeatherForecast_BaseDateTimeCalculation() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setBody(createSuccessResponse())
                .setHeader("Content-Type", "application/json"));

            // When
            weatherApiClient.getWeatherForecast(TEST_GRID);

            // Then
            RecordedRequest request = mockWebServer.takeRequest();
            String path = request.getPath();

            // base_date와 base_time이 포함되어 있는지 확인
            assertThat(path).contains("base_date=");
            assertThat(path).contains("base_time=");

            // 날짜 형식 확인 (yyyyMMdd)
            assertThat(path).matches(".*base_date=\\d{8}.*");
            // 시간 형식 확인 (HHmm)
            assertThat(path).matches(".*base_time=\\d{4}.*");
        }
    }

    @Nested
    @DisplayName("callVilageFcst 메서드 테스트")
    class CallVilageFcstTest {

        @Test
        @DisplayName("단기예보 조회 - 커스텀 날짜/시간")
        void callVilageFcst_CustomDateTime() throws Exception {
            // Given
            String baseDate = "20250701";
            String baseTime = "0200";
            mockWebServer.enqueue(new MockResponse()
                .setBody(createSuccessResponse())
                .setHeader("Content-Type", "application/json"));

            // When
            WeatherApiResponse response = weatherApiClient.callVilageFcst(TEST_GRID, baseDate, baseTime);

            // Then
            assertThat(response).isNotNull();

            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getPath())
                .contains("base_date=" + baseDate)
                .contains("base_time=" + baseTime);
        }

        @Test
        @DisplayName("대용량 데이터 요청 (numOfRows=1000)")
        void callVilageFcst_LargeDataRequest() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setBody(createLargeResponse())
                .setHeader("Content-Type", "application/json"));

            // When
            WeatherApiResponse response = weatherApiClient.callVilageFcst(
                TEST_GRID, "20250701", "0200"
            );

            // Then
            assertThat(response.response().body().items().item()).hasSize(100);

            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getPath()).contains("numOfRows=1000");
        }

        @Test
        @DisplayName("null 응답 처리")
        void callVilageFcst_NullResponse() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setBody("")
                .setResponseCode(204)); // No Content

            // When
            WeatherApiResponse response = weatherApiClient.callVilageFcst(
                TEST_GRID, "20250701", "0200"
            );

            // Then
            assertThat(response).isNull();
        }
    }

    @Nested
    @DisplayName("getWeatherNowcast 메서드 테스트")
    class GetWeatherNowcastTest {

        @Test
        @DisplayName("초단기실황 조회")
        void getWeatherNowcast_Success() throws Exception {
            // Given
            String mockResponse = createNowcastResponse();
            mockWebServer.enqueue(new MockResponse()
                .setBody(mockResponse)
                .setHeader("Content-Type", "application/json"));

            // When
            WeatherApiResponse response = weatherApiClient.getWeatherNowcast(TEST_GRID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.response().header().resultCode()).isEqualTo("00");

            RecordedRequest request = mockWebServer.takeRequest();
            assertThat(request.getPath())
                .contains("/getUltraSrtNcst")
                .contains("numOfRows=10");
        }

        @Test
        @DisplayName("초단기실황 시간 계산 검증")
        void getWeatherNowcast_TimeCalculation() throws Exception {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setBody(createNowcastResponse())
                .setHeader("Content-Type", "application/json"));

            // When
            weatherApiClient.getWeatherNowcast(TEST_GRID);

            // Then
            RecordedRequest request = mockWebServer.takeRequest();
            String path = request.getPath();

            // 현재 시간 기준으로 계산되었는지 확인
            LocalDateTime now = LocalDateTime.now();
            String expectedDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            assertThat(path).contains("base_date=" + expectedDate);
        }
    }

    @Nested
    @DisplayName("에러 응답 처리 테스트")
    class ErrorResponseTest {

        @Test
        @DisplayName("기상청 API 에러 코드 처리")
        void handleApiErrorCodes() throws Exception {
            // Given
            String errorResponse = createErrorResponse("03", "No data");
            mockWebServer.enqueue(new MockResponse()
                .setBody(errorResponse)
                .setHeader("Content-Type", "application/json"));

            // When
            WeatherApiResponse response = weatherApiClient.getWeatherForecast(TEST_GRID);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.response().header().resultCode()).isEqualTo("03");
            assertThat(response.response().header().resultMsg()).isEqualTo("No data");
        }

        @Test
        @DisplayName("네트워크 오류 처리")
        void handleNetworkError() {
            // Given
            mockWebServer.enqueue(new MockResponse()
                .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST));

            // When & Then
            assertThatThrownBy(() -> weatherApiClient.getWeatherForecast(TEST_GRID))
                .isInstanceOf(Exception.class);
        }
    }

    private String createSuccessResponse() {
        return """
            {
                "response": {
                    "header": {
                        "resultCode": "00",
                        "resultMsg": "NORMAL_SERVICE"
                    },
                    "body": {
                        "dataType": "JSON",
                        "items": {
                            "item": [
                                {
                                    "baseDate": "20250701",
                                    "baseTime": "0200",
                                    "category": "TMP",
                                    "fcstDate": "20250701",
                                    "fcstTime": "0300",
                                    "fcstValue": "20",
                                    "nx": 60,
                                    "ny": 127
                                },
                                {
                                    "baseDate": "20250701",
                                    "baseTime": "0200",
                                    "category": "SKY",
                                    "fcstDate": "20250701",
                                    "fcstTime": "0300",
                                    "fcstValue": "1",
                                    "nx": 60,
                                    "ny": 127
                                },
                                {
                                    "baseDate": "20250701",
                                    "baseTime": "0200",
                                    "category": "PTY",
                                    "fcstDate": "20250701",
                                    "fcstTime": "0300",
                                    "fcstValue": "0",
                                    "nx": 60,
                                    "ny": 127
                                }
                            ]
                        },
                        "pageNo": 1,
                        "numOfRows": 3,
                        "totalCount": 3
                    }
                }
            }
            """;
    }

    private String createLargeResponse() {
        StringBuilder items = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) items.append(",");
            items.append(String.format("""
                {
                    "baseDate": "20250701",
                    "baseTime": "0200",
                    "category": "TMP",
                    "fcstDate": "20250701",
                    "fcstTime": "%02d00",
                    "fcstValue": "%d",
                    "nx": 60,
                    "ny": 127
                }
                """, i % 24, 15 + (i % 10)));
        }
        items.append("]");

        return String.format("""
            {
                "response": {
                    "header": {
                        "resultCode": "00",
                        "resultMsg": "NORMAL_SERVICE"
                    },
                    "body": {
                        "dataType": "JSON",
                        "items": {
                            "item": %s
                        },
                        "pageNo": 1,
                        "numOfRows": 100,
                        "totalCount": 100
                    }
                }
            }
            """, items);
    }

    private String createNowcastResponse() {
        return """
            {
                "response": {
                    "header": {
                        "resultCode": "00",
                        "resultMsg": "NORMAL_SERVICE"
                    },
                    "body": {
                        "dataType": "JSON",
                        "items": {
                            "item": [
                                {
                                    "baseDate": "20250701",
                                    "baseTime": "1400",
                                    "category": "T1H",
                                    "obsrValue": "18.5",
                                    "nx": 60,
                                    "ny": 127
                                }
                            ]
                        },
                        "pageNo": 1,
                        "numOfRows": 10,
                        "totalCount": 1
                    }
                }
            }
            """;
    }

    private String createErrorResponse(String errorCode, String errorMsg) {
        return String.format("""
            {
                "response": {
                    "header": {
                        "resultCode": "%s",
                        "resultMsg": "%s"
                    }
                }
            }
            """, errorCode, errorMsg);
    }
}