package com.fourthread.ozang.module.domain.weather.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.WeatherDto;
import com.fourthread.ozang.module.domain.weather.dto.WeatherSummaryDto;
import com.fourthread.ozang.module.domain.weather.dto.external.WeatherApiResponse;
import com.fourthread.ozang.module.domain.weather.dto.type.PrecipitationType;
import com.fourthread.ozang.module.domain.weather.dto.type.SkyStatus;
import com.fourthread.ozang.module.domain.weather.dto.type.WindStrength;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mapstruct.factory.Mappers;

@DisplayName("Weather Mapper 테스트")
class WeatherMapperTest {

    private WeatherMapper weatherMapper;

    private static final WeatherAPILocation TEST_LOCATION = new WeatherAPILocation(
        37.5665, 126.9780, 60, 127, List.of("서울특별시 중구")
    );

    @BeforeEach
    void setUp() {
        weatherMapper = Mappers.getMapper(WeatherMapper.class);
    }

    @Nested
    @DisplayName("toDto 메서드 테스트")
    class ToDtoTest {

        @Test
        @DisplayName("Weather 엔티티를 DTO로 변환")
        void toDto_Success() {
            Weather weather = createCompleteWeather();

            WeatherDto dto = weatherMapper.toDto(weather);

            assertThat(dto).isNotNull();
            assertThat(dto.id()).isEqualTo(weather.getId());
            assertThat(dto.forecastedAt()).isEqualTo(weather.getForecastedAt());
            assertThat(dto.forecastAt()).isEqualTo(weather.getForecastAt());
            assertThat(dto.skyStatus()).isEqualTo(weather.getSkyStatus());

            assertThat(dto.location()).isNotNull();
            assertThat(dto.location().latitude()).isEqualTo(TEST_LOCATION.latitude());

            assertThat(dto.precipitation()).isNotNull();
            assertThat(dto.temperature()).isNotNull();
            assertThat(dto.humidity()).isNotNull();
            assertThat(dto.windSpeed()).isNotNull();
        }

        @Test
        @DisplayName("null Weather 처리")
        void toDto_NullWeather() {
            WeatherDto dto = weatherMapper.toDto(null);
            assertThat(dto).isNull();
        }
    }

    @Nested
    @DisplayName("toSummaryDto 메서드 테스트")
    class ToSummaryDtoTest {

        @Test
        @DisplayName("Weather를 WeatherSummaryDto로 변환")
        void toSummaryDto_Success() {
            // Given
            Weather weather = createCompleteWeather();

            // When
            WeatherSummaryDto summaryDto = weatherMapper.toSummaryDto(weather);

            // Then
            assertThat(summaryDto).isNotNull();
            assertThat(summaryDto.weatherId()).isEqualTo(weather.getId());
            assertThat(summaryDto.skyStatus()).isEqualTo(weather.getSkyStatus());
            assertThat(summaryDto.precipitation()).isEqualTo(weather.getPrecipitation());
            assertThat(summaryDto.temperature()).isEqualTo(weather.getTemperature());
        }

        @Test
        @DisplayName("null Weather 처리")
        void toSummaryDto_NullWeather() {
            // When
            WeatherSummaryDto summaryDto = weatherMapper.toSummaryDto(null);

            // Then
            assertThat(summaryDto).isNull();
        }
    }


    @Nested
    @DisplayName("fromApiResponse 메서드 테스트")
    class FromApiResponseTest {

        @Test
        @DisplayName("API 응답을 Weather 엔티티로 변환")
        void fromApiResponse_Success() {
            List<WeatherApiResponse.Item> items = createCompleteApiItems();

            Weather weather = weatherMapper.fromApiResponse(items, TEST_LOCATION);

            assertThat(weather).isNotNull();
            assertThat(weather.getLocation()).isNotNull();
            assertThat(weather.getSkyStatus()).isEqualTo(SkyStatus.CLEAR);
            assertThat(weather.getTemperature().current()).isEqualTo(20.5);
            assertThat(weather.getTemperature().min()).isEqualTo(15.0);
            assertThat(weather.getTemperature().max()).isEqualTo(25.0);
            assertThat(weather.getPrecipitation().type()).isEqualTo(PrecipitationType.RAIN);
            assertThat(weather.getPrecipitation().probability()).isEqualTo(80.0);
            assertThat(weather.getPrecipitation().amount()).isEqualTo(5.5);
            assertThat(weather.getHumidity().current()).isEqualTo(65.0);
            assertThat(weather.getWindSpeed().speed()).isEqualTo(7.5);
            assertThat(weather.getWindSpeed().asWord()).isEqualTo(WindStrength.MODERATE);
        }

        @Test
        @DisplayName("빈 API 응답 처리")
        void fromApiResponse_EmptyItems() {
            // When & Then
            assertThatThrownBy(
                () -> weatherMapper.fromApiResponse(Collections.emptyList(), TEST_LOCATION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Weather items cannot be null or empty");
        }

        @Test
        @DisplayName("null API 응답 처리")
        void fromApiResponse_NullItems() {
            // When & Then
            assertThatThrownBy(() -> weatherMapper.fromApiResponse(null, TEST_LOCATION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Weather items cannot be null or empty");
        }

        @Test
        @DisplayName("null location 처리")
        void fromApiResponse_NullLocation() {
            // Given
            List<WeatherApiResponse.Item> items = createCompleteApiItems();

            // When & Then
            assertThatThrownBy(() -> weatherMapper.fromApiResponse(items, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Location cannot be null");
        }

        @Test
        @DisplayName("첫 번째 아이템이 null인 경우")
        void fromApiResponse_FirstItemNull() {
            List<WeatherApiResponse.Item> items = new ArrayList<>();
            items.add(null);

            assertThatThrownBy(() -> weatherMapper.fromApiResponse(items, TEST_LOCATION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("First weather item cannot be null");
        }

        @Test
        @DisplayName("부분적인 API 데이터 처리")
        void fromApiResponse_PartialData() {
            // Given - 온도 정보만 있는 경우
            List<WeatherApiResponse.Item> items = List.of(
                new WeatherApiResponse.Item("20240315", "0200", "TMP", "20240315", "0300", "20.5",
                    60, 127)
            );

            // When
            Weather weather = weatherMapper.fromApiResponse(items, TEST_LOCATION);

            // Then
            assertThat(weather).isNotNull();
            assertThat(weather.getTemperature().current()).isEqualTo(20.5);
            assertThat(weather.getTemperature().min()).isEqualTo(0.0); // 기본값
            assertThat(weather.getPrecipitation().type()).isEqualTo(PrecipitationType.NONE); // 기본값
        }

        @Test
        @DisplayName("잘못된 카테고리 데이터 무시")
        void fromApiResponse_InvalidCategory() {
            // Given
            List<WeatherApiResponse.Item> items = List.of(
                new WeatherApiResponse.Item("20240315", "0200", "INVALID", "20240315", "0300",
                    "999", 60, 127),
                new WeatherApiResponse.Item("20240315", "0200", "TMP", "20240315", "0300", "20.5",
                    60, 127)
            );

            // When
            Weather weather = weatherMapper.fromApiResponse(items, TEST_LOCATION);

            // Then
            assertThat(weather).isNotNull();
            assertThat(weather.getTemperature().current()).isEqualTo(20.5);
        }

    }

    @Nested
    @DisplayName("parseDateTime 메서드 테스트")
    class ParseDateTimeTest {

        @ParameterizedTest
        @DisplayName("정상적인 날짜/시간 파싱")
        @MethodSource("provideValidDateTimes")
        void parseDateTime_ValidInput(String date, String time, LocalDateTime expected) {
            // When
            LocalDateTime result = weatherMapper.parseDateTime(date, time);

            // Then
            assertThat(result).isEqualTo(expected);
        }

        @ParameterizedTest
        @DisplayName("시간 패딩 처리")
        @MethodSource("provideTimePaddingCases")
        void parseDateTime_TimePadding(String date, String time, String expectedTime) {
            // When
            LocalDateTime result = weatherMapper.parseDateTime(date, time);

            // Then
            assertThat(result.format(DateTimeFormatter.ofPattern("HH:mm"))).isEqualTo(expectedTime);
        }

        @ParameterizedTest
        @DisplayName("null 또는 잘못된 입력 처리")
        @NullSource
        void parseDateTime_NullInput(String nullValue) {
            // When
            LocalDateTime result1 = weatherMapper.parseDateTime(nullValue, "0300");
            LocalDateTime result2 = weatherMapper.parseDateTime("20240315", nullValue);

            // Then
            // 현재 시간이 반환되는지 확인 (약간의 시간차 허용)
            assertThat(result1).isCloseTo(LocalDateTime.now(), within(1, java.time.temporal.ChronoUnit.SECONDS));
            assertThat(result2).isCloseTo(LocalDateTime.now(), within(1, java.time.temporal.ChronoUnit.SECONDS));
        }

        @Test
        @DisplayName("잘못된 형식의 날짜/시간")
        void parseDateTime_InvalidFormat() {
            // When
            LocalDateTime result = weatherMapper.parseDateTime("invalid", "invalid");

            // Then
            assertThat(result).isCloseTo(LocalDateTime.now(), within(1, java.time.temporal.ChronoUnit.SECONDS));
        }

        private static Stream<Arguments> provideValidDateTimes() {
            return Stream.of(
                Arguments.of("20240315", "0300", LocalDateTime.of(2024, 3, 15, 3, 0)),
                Arguments.of("20240315", "1200", LocalDateTime.of(2024, 3, 15, 12, 0)),
                Arguments.of("20240315", "2359", LocalDateTime.of(2024, 3, 15, 23, 0))
            );
        }

        private static Stream<Arguments> provideTimePaddingCases() {
            return Stream.of(
                Arguments.of("20240315", "0", "00:00"),
                Arguments.of("20240315", "300", "03:00"),
                Arguments.of("20240315", "30", "00:00"),
                Arguments.of("20240315", "1230", "12:00")
            );
        }
    }

    @Nested
    @DisplayName("toWeatherAPILocation 메서드 테스트")
    class ToWeatherAPILocationTest {

        @Test
        @DisplayName("모든 파라미터를 사용한 WeatherAPILocation 생성")
        void toWeatherAPILocation_Success() {
            // Given
            Double latitude = 37.5665;
            Double longitude = 126.9780;
            Integer x = 60;
            Integer y = 127;
            List<String> locationNames = List.of("서울특별시", "중구", "명동");

            // When
            WeatherAPILocation location = weatherMapper.toWeatherAPILocation(
                latitude, longitude, x, y, locationNames
            );

            // Then
            assertThat(location).isNotNull();
            assertThat(location.latitude()).isEqualTo(latitude);
            assertThat(location.longitude()).isEqualTo(longitude);
            assertThat(location.x()).isEqualTo(x);
            assertThat(location.y()).isEqualTo(y);
            assertThat(location.locationNames()).isEqualTo(locationNames);
        }

        @Test
        @DisplayName("빈 위치명 리스트 처리")
        void toWeatherAPILocation_EmptyLocationNames() {
            // When
            WeatherAPILocation location = weatherMapper.toWeatherAPILocation(
                37.5665, 126.9780, 60, 127, Collections.emptyList()
            );

            // Then
            assertThat(location).isNotNull();
            assertThat(location.locationNames()).isEmpty();
        }
    }

    @Nested
    @DisplayName("통합 시나리오 테스트")
    class IntegrationScenarioTest {

        @Test
        @DisplayName("API 응답 -> Weather 엔티티 -> DTO 전체 변환 플로우")
        void fullConversionFlow() {
            // Given - API 응답
            List<WeatherApiResponse.Item> apiItems = createCompleteApiItems();

            // When - API 응답을 Weather로 변환
            Weather weather = weatherMapper.fromApiResponse(apiItems, TEST_LOCATION);

            // When - Weather를 DTO로 변환
            WeatherDto dto = weatherMapper.toDto(weather);
            WeatherSummaryDto summaryDto = weatherMapper.toSummaryDto(weather);

            // Then - 전체 데이터 일관성 확인
            assertThat(dto.id()).isEqualTo(weather.getId());
            assertThat(dto.skyStatus()).isEqualTo(SkyStatus.CLEAR);
            assertThat(dto.temperature().current()).isEqualTo(20.5);

            assertThat(summaryDto.weatherId()).isEqualTo(weather.getId());
            assertThat(summaryDto.skyStatus()).isEqualTo(dto.skyStatus());
            assertThat(summaryDto.temperature()).isEqualTo(dto.temperature());
        }
    }

    private Weather createCompleteWeather() {
        Weather weather = Weather.create(
            LocalDateTime.now(),
            LocalDateTime.now().plusHours(1),
            TEST_LOCATION,
            SkyStatus.CLEAR
        );

        weather.updateWeatherData("TMP", "20.5");
        weather.updateWeatherData("TMN", "15.0");
        weather.updateWeatherData("TMX", "25.0");
        weather.updateWeatherData("PTY", "1");
        weather.updateWeatherData("POP", "80.0");
        weather.updateWeatherData("PCP", "5.5");
        weather.updateWeatherData("REH", "65.0");
        weather.updateWeatherData("WSD", "7.5");

        return weather;
    }

    private List<WeatherApiResponse.Item> createCompleteApiItems() {
        return List.of(
            new WeatherApiResponse.Item("20250630", "0200", "TMP", "20250630", "0300", "20.5", 60,
                127),
            new WeatherApiResponse.Item("20250630", "0200", "TMN", "20250630", "0300", "15.0", 60,
                127),
            new WeatherApiResponse.Item("20250630", "0200", "TMX", "20250630", "0300", "25.0", 60,
                127),
            new WeatherApiResponse.Item("20250630", "0200", "SKY", "20250630", "0300", "1", 60,
                127),
            new WeatherApiResponse.Item("20250630", "0200", "PTY", "20250630", "0300", "1", 60,
                127),
            new WeatherApiResponse.Item("20250630", "0200", "POP", "20250630", "0300", "80", 60,
                127),
            new WeatherApiResponse.Item("20250630", "0200", "PCP", "20250630", "0300", "5.5", 60,
                127),
            new WeatherApiResponse.Item("20250630", "0200", "REH", "20250630", "0300", "65", 60,
                127),
            new WeatherApiResponse.Item("20250630", "0200", "WSD", "20250630", "0300", "7.5", 60,
                127),
            new WeatherApiResponse.Item("20250630", "0200", "VEC", "20250630", "0300", "180", 60,
                127),
            new WeatherApiResponse.Item("20250630", "0200", "UUU", "20250630", "0300", "0.0", 60,
                127),
            new WeatherApiResponse.Item("20250630", "0200", "VVV", "20250630", "0300", "7.5", 60,
                127)
        );
    }
}