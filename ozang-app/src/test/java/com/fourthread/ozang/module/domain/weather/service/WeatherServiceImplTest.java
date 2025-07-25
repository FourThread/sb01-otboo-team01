package com.fourthread.ozang.module.domain.weather.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fourthread.ozang.core.domain.weather.client.KakaoApiClient;
import com.fourthread.ozang.core.domain.weather.client.WeatherApiClient;
import com.fourthread.ozang.core.domain.weather.dto.HumidityDto;
import com.fourthread.ozang.core.domain.weather.dto.PrecipitationDto;
import com.fourthread.ozang.core.domain.weather.dto.TemperatureDto;
import com.fourthread.ozang.core.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.core.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.core.domain.weather.dto.WindSpeedDto;
import com.fourthread.ozang.core.domain.weather.dto.type.PrecipitationType;
import com.fourthread.ozang.core.domain.weather.dto.type.SkyStatus;
import com.fourthread.ozang.core.domain.weather.dto.type.WindStrength;
import com.fourthread.ozang.core.domain.weather.entity.GridCoordinate;
import com.fourthread.ozang.core.domain.weather.entity.Weather;
import com.fourthread.ozang.core.domain.weather.exception.InvalidCoordinateException;
import com.fourthread.ozang.core.domain.weather.mapper.WeatherMapper;
import com.fourthread.ozang.core.domain.weather.repository.WeatherRepository;
import com.fourthread.ozang.core.domain.weather.service.WeatherCacheService;
import com.fourthread.ozang.core.domain.weather.service.WeatherServiceImpl;
import com.fourthread.ozang.core.domain.weather.util.CoordinateConverter;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
    private WeatherCacheService cacheService;

    private static final Double VALID_LATITUDE = 37.5610;
    private static final Double VALID_LONGITUDE = 126.9996;
    private static final Integer GRID_X = 60;
    private static final Integer GRID_Y = 127;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(weatherService, "defaultRetentionDays", 30);

        lenient().when(coordinateConverter.convertToGrid(VALID_LATITUDE, VALID_LONGITUDE))
            .thenReturn(new GridCoordinate(GRID_X, GRID_Y));
        lenient().when(kakaoApiClient.getLocationNames(VALID_LATITUDE, VALID_LONGITUDE))
            .thenReturn(List.of("서울특별시 중구"));
    }

    @Nested
    @DisplayName("날씨 정보 조회 테스트")
    class GetWeatherForecastTest {

        @ParameterizedTest
        @DisplayName("잘못된 좌표 입력 시 예외 발생")
        @ValueSource(doubles = {-91.0, 91.0, -181.0, 181.0})
        void getWeatherForecast_InvalidCoordinates(Double invalidValue) {
            // When & Then
            assertThatThrownBy(() -> weatherService.getWeatherForecast(invalidValue, VALID_LATITUDE))
                .isInstanceOf(InvalidCoordinateException.class);
        }
    }

    @Nested
    @DisplayName("5일 예보 조회 테스트")
    class GetFiveDayForecastTest {

        @Test
        @DisplayName("5일 예보 조회 - 캐시 히트")
        void getFiveDayForecast_CacheHit() {
            // Given
            List<WeatherDto> cachedForecast = List.of(createMockWeatherDto(), createMockWeatherDto());

            when(cacheService.getForecastFromCache(eq(VALID_LATITUDE), eq(VALID_LONGITUDE), anyString()))
                .thenReturn(cachedForecast);

            // When
            List<WeatherDto> result = weatherService.getFiveDayForecast(VALID_LONGITUDE, VALID_LATITUDE);

            // Then
            assertThat(result).isEqualTo(cachedForecast);
            verify(cacheService).getForecastFromCache(eq(VALID_LATITUDE), eq(VALID_LONGITUDE), anyString());
            //  API 호출이 발생하지 않아야 함 
            verify(weatherApiClient, never()).getWeatherForecast(any(GridCoordinate.class));
            verify(kakaoApiClient, never()).getLocationNames(anyDouble(), anyDouble());
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
            when(cacheService.getLocationFromCache(VALID_LATITUDE, VALID_LONGITUDE))
                .thenReturn(null);
            when(weatherMapper.toWeatherAPILocation(
                eq(VALID_LATITUDE), eq(VALID_LONGITUDE), eq(GRID_X), eq(GRID_Y), anyList()
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
            verify(cacheService).cacheLocation(eq(VALID_LATITUDE), eq(VALID_LONGITUDE), eq(expectedLocation));
        }
    }

    @Nested
    @DisplayName("배치 메서드 테스트")
    class BatchMethodsTest {

        @Test
        @DisplayName("기본 보관 기간으로 오래된 날씨 데이터 정리")
        void cleanupOldWeatherData_DefaultRetention() {
            // Given
            when(weatherRepository.countOldWeatherData(any(LocalDateTime.class))).thenReturn(10L);

            // When
            int deletedCount = weatherService.cleanupOldWeatherData();

            // Then
            assertThat(deletedCount).isEqualTo(10);
            verify(weatherRepository).countOldWeatherData(any(LocalDateTime.class));
            verify(weatherRepository).deleteOldWeatherData(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("사용자 정의 보관 기간으로 오래된 날씨 데이터 정리")
        void cleanupOldWeatherData_CustomRetention() {
            // Given
            int customRetentionDays = 7;
            when(weatherRepository.countOldWeatherData(any(LocalDateTime.class))).thenReturn(25L);

            // When
            int deletedCount = weatherService.cleanupOldWeatherData(customRetentionDays);

            // Then
            assertThat(deletedCount).isEqualTo(25);
            verify(weatherRepository).countOldWeatherData(any(LocalDateTime.class));
            verify(weatherRepository).deleteOldWeatherData(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("삭제할 데이터가 없는 경우")
        void cleanupOldWeatherData_NoDataToDelete() {
            // Given
            when(weatherRepository.countOldWeatherData(any(LocalDateTime.class))).thenReturn(0L);

            // When
            int deletedCount = weatherService.cleanupOldWeatherData();

            // Then
            assertThat(deletedCount).isZero();
            verify(weatherRepository).countOldWeatherData(any(LocalDateTime.class));
            verify(weatherRepository, never()).deleteOldWeatherData(any(LocalDateTime.class));
        }

        @ParameterizedTest
        @DisplayName("다양한 보관 기간 테스트")
        @ValueSource(ints = {1, 7, 15, 30, 60, 90})
        void cleanupOldWeatherData_VariousRetentionPeriods(int retentionDays) {
            // Given
            long expectedCount = retentionDays * 2L;
            when(weatherRepository.countOldWeatherData(any(LocalDateTime.class))).thenReturn(expectedCount);

            // When
            int deletedCount = weatherService.cleanupOldWeatherData(retentionDays);

            // Then
            assertThat(deletedCount).isEqualTo((int) expectedCount);
            verify(weatherRepository).countOldWeatherData(any(LocalDateTime.class));
            verify(weatherRepository).deleteOldWeatherData(any(LocalDateTime.class));
        }

        @Test
        @DisplayName("배치 작업 로깅 검증")
        void cleanupOldWeatherData_LoggingVerification() {
            // Given
            when(weatherRepository.countOldWeatherData(any(LocalDateTime.class))).thenReturn(100L);

            // When
            int deletedCount = weatherService.cleanupOldWeatherData(15);

            // Then
            assertThat(deletedCount).isEqualTo(100);

            verify(weatherRepository).countOldWeatherData(any(LocalDateTime.class));
            verify(weatherRepository).deleteOldWeatherData(any(LocalDateTime.class));
        }
    }

    private Weather createMockWeather() {
        LocalDateTime now = LocalDateTime.now();
        Weather weather = Weather.create(
            now,                              // forecastedAt
            now.plusHours(1),                // forecastAt
            createMockWeatherAPILocation(),  // apiLocation
            SkyStatus.CLEAR                  // skyStatus
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

}