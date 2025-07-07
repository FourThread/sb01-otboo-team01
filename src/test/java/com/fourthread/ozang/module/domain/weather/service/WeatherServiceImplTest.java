package com.fourthread.ozang.module.domain.weather.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.fourthread.ozang.module.domain.weather.exception.WeatherDataFetchException;
import com.fourthread.ozang.module.domain.weather.mapper.WeatherMapper;
import com.fourthread.ozang.module.domain.weather.repository.WeatherRepository;
import com.fourthread.ozang.module.domain.weather.util.CoordinateConverter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("날씨 서비스 테스트")
class WeatherServiceImplTest {

    @InjectMocks
    private WeatherServiceImpl weatherService;

    @Mock
    private WeatherRepository weatherRepository;

    @Mock
    private WeatherMapper weatherMapper;

    @Mock
    private WeatherApiClient weatherApiClient;

    @Mock
    private KakaoApiClient kakaoApiClient;

    @Mock
    private CoordinateConverter coordinateConverter;

    @Mock
    private Executor apiCallExecutor;

    @Captor
    private ArgumentCaptor<Weather> weatherCaptor;

    private static final Double VALID_LATITUDE = 37.5665;
    private static final Double VALID_LONGITUDE = 126.9780;
    private static final Integer GRID_X = 60;
    private static final Integer GRID_Y = 127;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run(); // 동기 실행
            return null;
        }).when(apiCallExecutor).execute(any(Runnable.class));

        // 기본 설정
        lenient().when(coordinateConverter.convertToGrid(anyDouble(), anyDouble()))
            .thenReturn(new GridCoordinate(GRID_X, GRID_Y));

        lenient().when(kakaoApiClient.getLocationNames(anyDouble(), anyDouble()))
            .thenReturn(List.of("서울특별시 중구"));
    }

    @Nested
    @DisplayName("날씨 정보 조회 테스트")
    class GetWeatherForecastTest {

        @Test
        @DisplayName("날씨 정보 조회")
        void getWeatherForecast_Success() {
            Weather mockWeather = createMockWeather();
            WeatherDto expectedDto = createMockWeatherDto();
            WeatherApiResponse mockApiResponse = createMockApiResponse();

            when(weatherRepository.findLatestByGridCoordinate(GRID_X, GRID_Y))
                .thenReturn(Optional.empty());
            when(weatherApiClient.getWeatherForecast(any(GridCoordinate.class)))
                .thenReturn(mockApiResponse);
            when(weatherMapper.fromApiResponse(anyList(), any(WeatherAPILocation.class)))
                .thenReturn(mockWeather);
            when(weatherRepository.save(any(Weather.class)))
                .thenReturn(mockWeather);
            when(weatherMapper.toDto(any(Weather.class)))
                .thenReturn(expectedDto);
            when(weatherMapper.toWeatherAPILocation(anyDouble(), anyDouble(), anyInt(), anyInt(), anyList()))
                .thenReturn(createMockWeatherAPILocation());


            WeatherDto result = weatherService.getWeatherForecast(VALID_LONGITUDE, VALID_LATITUDE);

            assertThat(result).isNotNull();
            assertThat(result.id()).isEqualTo(expectedDto.id());
            verify(weatherRepository).save(weatherCaptor.capture());
            assertThat(weatherCaptor.getValue().getApiResponseHash()).isNotNull();
        }

        @Test
        @DisplayName("캐시된 날씨 데이터 사용")
        void getWeatherForecast_UseCachedData() {
            // Given
            Weather cachedWeather = createCachedMockWeather();
            WeatherDto expectedDto = createMockWeatherDto();

            when(weatherRepository.findLatestByGridCoordinate(GRID_X, GRID_Y))
                .thenReturn(Optional.of(cachedWeather));
            when(weatherMapper.toDto(cachedWeather))
                .thenReturn(expectedDto);

            // When
            WeatherDto result = weatherService.getWeatherForecast(VALID_LONGITUDE, VALID_LATITUDE);

            // Then
            assertThat(result).isEqualTo(expectedDto);
            verify(weatherApiClient, never()).getWeatherForecast(any());
            verify(weatherRepository, never()).save(any());
        }

        @ParameterizedTest
        @DisplayName("유효하지 않은 좌표로 조회 시 예외 발생")
        @CsvSource({
            ",37.5665",      // null longitude
            "126.9780,",     // null latitude
            "123.0,37.5665", // longitude too small
            "133.0,37.5665", // longitude too large
            "126.9780,32.0", // latitude too small
            "126.9780,44.0"  // latitude too large
        })
        void getWeatherForecast_InvalidCoordinates(String longitude, String latitude) {
            // Given
            Double lon = longitude != null ? Double.parseDouble(longitude) : null;
            Double lat = latitude != null ? Double.parseDouble(latitude) : null;

            // When & Then
            assertThatThrownBy(() -> weatherService.getWeatherForecast(lon, lat))
                .isInstanceOf(InvalidCoordinateException.class);
        }

        @Test
        @DisplayName("API 응답이 null일 때 예외 발생")
        void getWeatherForecast_NullApiResponse() {
            // Given
            when(weatherRepository.findLatestByGridCoordinate(GRID_X, GRID_Y))
                .thenReturn(Optional.empty());
            when(weatherApiClient.getWeatherForecast(any(GridCoordinate.class)))
                .thenReturn(null);

            // When & Then
            assertThatThrownBy(() -> weatherService.getWeatherForecast(VALID_LONGITUDE, VALID_LATITUDE))
                .isInstanceOf(WeatherDataFetchException.class)
                .hasMessage("API 응답이 null입니다.");
        }

        @ParameterizedTest
        @DisplayName("API 에러 코드별 예외 처리")
        @CsvSource({
            "01,어플리케이션 에러 - base_date/base_time 파라미터 오류,WeatherApiException",
            "03,해당 조건의 데이터가 없습니다 - nx/ny 좌표 오류,WeatherDataFetchException",
            "10,잘못된 요청 파라미터입니다,InvalidCoordinateException",
            "30,등록되지 않은 서비스키,WeatherApiException"
        })
        void getWeatherForecast_ApiErrorCodes(String errorCode, String expectedMessage, String exceptionType) {
            // Given
            WeatherApiResponse errorResponse = createErrorApiResponse(errorCode);
            when(weatherRepository.findLatestByGridCoordinate(GRID_X, GRID_Y))
                .thenReturn(Optional.empty());
            when(weatherApiClient.getWeatherForecast(any(GridCoordinate.class)))
                .thenReturn(errorResponse);

            // When & Then
            assertThatThrownBy(() -> weatherService.getWeatherForecast(VALID_LONGITUDE, VALID_LATITUDE))
                .satisfies(throwable -> {
                    assertThat(throwable.getClass().getSimpleName()).isEqualTo(exceptionType);
                    assertThat(throwable.getMessage()).contains(expectedMessage);
                });
        }
    }

    @Nested
    @DisplayName("5일 예보 조회 테스트")
    class GetFiveDayForecastTest {

        @Test
        @DisplayName("5일 예보 조회")
        void getFiveDayForecast_Success() {
            // Given
            WeatherApiResponse mockResponse = createMockFiveDayApiResponse();
            List<WeatherDto> expectedDtos = createMockWeatherDtoList(5);

            when(weatherApiClient.callVilageFcst(any(), anyString(), anyString()))
                .thenReturn(mockResponse);
            when(coordinateConverter.convertToGrid(VALID_LATITUDE, VALID_LONGITUDE))
                .thenReturn(new GridCoordinate(GRID_X, GRID_Y));

            // When
            List<WeatherDto> result = weatherService.getFiveDayForecast(VALID_LONGITUDE, VALID_LATITUDE);

            // Then
            assertThat(result).isNotNull();
            verify(weatherApiClient).callVilageFcst(any(), anyString(), anyString());
            verify(kakaoApiClient).getLocationNames(VALID_LATITUDE, VALID_LONGITUDE);
        }

        @Test
        @DisplayName("빈 API 응답 처리")
        void getFiveDayForecast_EmptyResponse() {
            // Given
            WeatherApiResponse emptyResponse = createEmptyApiResponse();
            when(weatherApiClient.callVilageFcst(any(), anyString(), anyString()))
                .thenReturn(emptyResponse);

            // When & Then
            assertThatThrownBy(() -> weatherService.getFiveDayForecast(VALID_LONGITUDE, VALID_LATITUDE))
                .isInstanceOf(WeatherDataFetchException.class)
                .hasMessage("날씨 데이터가 없습니다.");
        }
    }

    @Nested
    @DisplayName("위치 정보 조회 테스트")
    class GetWeatherLocationTest {

        @Test
        @DisplayName("정상적인 위치 정보 조회")
        void getWeatherLocation_Success() {
            // Given
            WeatherAPILocation expectedLocation = createMockWeatherAPILocation();
            when(weatherMapper.toWeatherAPILocation(
                VALID_LATITUDE, VALID_LONGITUDE, GRID_X, GRID_Y, List.of("서울특별시 중구")
            )).thenReturn(expectedLocation);

            // When
            WeatherAPILocation result = weatherService.getWeatherLocation(VALID_LONGITUDE, VALID_LATITUDE);

            // Then
            assertThat(result).isEqualTo(expectedLocation);
            assertThat(result.latitude()).isEqualTo(VALID_LATITUDE);
            assertThat(result.longitude()).isEqualTo(VALID_LONGITUDE);
            assertThat(result.x()).isEqualTo(GRID_X);
            assertThat(result.y()).isEqualTo(GRID_Y);
            assertThat(result.locationNames()).contains("서울특별시 중구");
        }
    }

    @Nested
    @DisplayName("내부 메서드 테스트")
    class InternalMethodsTest {

        @Test
        @DisplayName("API 응답 해시 생성 테스트")
        void generateResponseHash_Success() throws Exception {
            // Given
            WeatherApiResponse response = createMockApiResponse();

            // When
            String hash = ReflectionTestUtils.invokeMethod(weatherService, "generateResponseHash", response);

            // Then
            assertThat(hash).isNotNull();
            assertThat(hash).hasSize(32);
        }

        @ParameterizedTest
        @DisplayName("base_date 계산 테스트")
        @CsvSource({
            "2025-06-30T01:30:00,20250629",  // 새벽 1시 30분 -> 전날
            "2025-06-30T02:15:00,20250630",  // 새벽 2시 15분 -> 당일
            "2025-06-30T23:45:00,20250630"   // 밤 11시 45분 -> 당일
        })
        void calculateBaseDate_Success(String inputTime, String expectedDate) {
            // Given
            LocalDateTime dateTime = LocalDateTime.parse(inputTime);

            // When
            String result = ReflectionTestUtils.invokeMethod(weatherService, "calculateBaseDate", dateTime);

            // Then
            assertThat(result).isEqualTo(expectedDate);
        }

        @ParameterizedTest
        @DisplayName("base_time 계산 테스트")
        @ValueSource(strings = {
            "2025-06-30T02:30:00", "2025-06-30T03:00:00", "2025-06-30T04:59:00"
        })
        void calculateBaseTime_Returns0200(String inputTime) {
            // Given
            LocalDateTime dateTime = LocalDateTime.parse(inputTime);

            // When
            String result = ReflectionTestUtils.invokeMethod(weatherService, "calculateBaseTime", dateTime);

            // Then
            assertThat(result).isEqualTo("0200");
        }
    }

    private Weather createMockWeather() {
        Weather weather = Weather.create(
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            createMockWeatherAPILocation(),
            SkyStatus.CLEAR
        );
        return weather;
    }

    private Weather createCachedMockWeather() {
        Weather weather = Weather.create(
            LocalDateTime.now().minusMinutes(30),
            LocalDateTime.now().plusHours(1),
            createMockWeatherAPILocation(),
            SkyStatus.CLEAR
        );
        return weather;
    }

    private WeatherDto createMockWeatherDto() {
        return new WeatherDto(
            UUID.randomUUID(),
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            createMockWeatherAPILocation(),
            SkyStatus.CLEAR,
            new PrecipitationDto(PrecipitationType.NONE, 0.0, 0.0),
            new HumidityDto(60.0, 5.0),
            new TemperatureDto(20.0, 2.0, 30.0, 25.0),
            new WindSpeedDto(3.5, WindStrength.WEAK)
        );
    }

    private WeatherAPILocation createMockWeatherAPILocation() {
        return new WeatherAPILocation(
            VALID_LATITUDE,
            VALID_LONGITUDE,
            GRID_X,
            GRID_Y,
            List.of("서울특별시 중구")
        );
    }

    private WeatherApiResponse createMockApiResponse() {
        List<WeatherApiResponse.Item> items = List.of(
            new WeatherApiResponse.Item("20250630", "0200", "TMP", "20250630", "0600", "20", GRID_X, GRID_Y),
            new WeatherApiResponse.Item("20250630", "0200", "SKY", "20250630", "0600", "1", GRID_X, GRID_Y),
            new WeatherApiResponse.Item("20250630", "0200", "PTY", "20250630", "0600", "0", GRID_X, GRID_Y)
        );

        return new WeatherApiResponse(
            new WeatherApiResponse.ResponseBody(
                new WeatherApiResponse.Header("00", "NORMAL_SERVICE"),
                new WeatherApiResponse.Body("JSON", new WeatherApiResponse.Items(items), 1, 3, 3)
            )
        );
    }

    private WeatherApiResponse createErrorApiResponse(String errorCode) {
        return new WeatherApiResponse(
            new WeatherApiResponse.ResponseBody(
                new WeatherApiResponse.Header(errorCode, "Error message"),
                null
            )
        );
    }

    private WeatherApiResponse createEmptyApiResponse() {
        return new WeatherApiResponse(
            new WeatherApiResponse.ResponseBody(
                new WeatherApiResponse.Header("00", "NORMAL_SERVICE"),
                new WeatherApiResponse.Body("JSON", new WeatherApiResponse.Items(Collections.emptyList()), 1, 0, 0)
            )
        );
    }

    private WeatherApiResponse createMockFiveDayApiResponse() {
        List<WeatherApiResponse.Item> items = new ArrayList<>();
        LocalDateTime baseTime = LocalDateTime.now();

        for (int day = 0; day < 5; day++) {
            LocalDateTime forecastTime = baseTime.plusDays(day);
            String fcstDate = forecastTime.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE);

            items.add(new WeatherApiResponse.Item("20250630", "0200", "TMP", fcstDate, "0900", "20", GRID_X, GRID_Y));
            items.add(new WeatherApiResponse.Item("20250630", "0200", "SKY", fcstDate, "0900", "1", GRID_X, GRID_Y));
            items.add(new WeatherApiResponse.Item("20250630", "0200", "PTY", fcstDate, "0900", "0", GRID_X, GRID_Y));
        }

        return new WeatherApiResponse(
            new WeatherApiResponse.ResponseBody(
                new WeatherApiResponse.Header("00", "NORMAL_SERVICE"),
                new WeatherApiResponse.Body("JSON", new WeatherApiResponse.Items(items), 1, items.size(), items.size())
            )
        );
    }

    private List<WeatherDto> createMockWeatherDtoList(int count) {
        return Stream.generate(this::createMockWeatherDto)
            .limit(count)
            .toList();
    }
}