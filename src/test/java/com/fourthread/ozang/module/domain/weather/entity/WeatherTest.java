package com.fourthread.ozang.module.domain.weather.entity;

import static org.assertj.core.api.Assertions.assertThat;

import com.fourthread.ozang.module.domain.weather.dto.HumidityDto;
import com.fourthread.ozang.module.domain.weather.dto.PrecipitationDto;
import com.fourthread.ozang.module.domain.weather.dto.TemperatureDto;
import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.WindSpeedDto;
import com.fourthread.ozang.module.domain.weather.dto.type.PrecipitationType;
import com.fourthread.ozang.module.domain.weather.dto.type.SkyStatus;
import com.fourthread.ozang.module.domain.weather.dto.type.WindStrength;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Weather 엔티티 테스트")
class WeatherTest {

    private static final LocalDateTime NOW = LocalDateTime.now();
    private static final WeatherAPILocation TEST_LOCATION = new WeatherAPILocation(
        37.5665, 126.9780, 60, 127, List.of("서울특별시 중구")
    );

    @Nested
    @DisplayName("Weather 생성 테스트")
    class WeatherCreationTest {

        @Test
        @DisplayName("Weather.create() 메서드로 정상 생성")
        void createWeather_Success() {
            Weather weather = Weather.create(NOW, NOW.plusHours(1), TEST_LOCATION, SkyStatus.CLEAR);

            assertThat(weather).isNotNull();
            assertThat(weather.getForecastedAt()).isEqualTo(NOW);
            assertThat(weather.getForecastAt()).isEqualTo(NOW.plusHours(1));
            assertThat(weather.getSkyStatus()).isEqualTo(SkyStatus.CLEAR);
            assertThat(weather.getLocation()).isNotNull();
            assertThat(weather.getPrecipitation()).isNotNull();
            assertThat(weather.getTemperature()).isNotNull();
            assertThat(weather.getHumidity()).isNotNull();
            assertThat(weather.getWindSpeed()).isNotNull();
        }

        @Test
        @DisplayName("Builder를 통한 생성")
        void createWeatherWithBuilder_Success() {
            Weather weather = Weather.builder()
                .forecastedAt(NOW)
                .forecastAt(NOW.plusHours(1))
                .location(WeatherLocation.from(TEST_LOCATION))
                .skyStatus(SkyStatus.CLOUDY)
                .precipitation(new PrecipitationInfo())
                .temperature(new TemperatureInfo())
                .humidity(new HumidityInfo())
                .wind(new WindInfo())
                .apiResponseHash("test-hash")
                .build();

            assertThat(weather).isNotNull();
            assertThat(weather.getSkyStatus()).isEqualTo(SkyStatus.CLOUDY);
            assertThat(weather.getApiResponseHash()).isEqualTo("test-hash");
        }
    }

    @Nested
    @DisplayName("Weather 데이터 업데이트 테스트")
    class WeatherUpdateTest {
        private Weather weather;

        @BeforeEach
        void setUp() {
            weather = Weather.create(NOW, NOW.plusHours(1), TEST_LOCATION, SkyStatus.CLEAR);
        }

        @ParameterizedTest
        @DisplayName("온도 데이터 업데이트")
        @CsvSource({
            "TMP,20.5",
            "TMN,15.0",
            "TMX,25.5"
        })
        void updateTemperatureDate(String category, String value) {
            weather.updateWeatherData(category, value);

            TemperatureDto temp = weather.getTemperature();
            switch (category) {
                case "TMP" -> assertThat(temp.current()).isEqualTo(20.5);
                case "TMN" -> assertThat(temp.min()).isEqualTo(15.0);
                case "TMX" -> assertThat(temp.max()).isEqualTo(25.5);
            }
        }

        @ParameterizedTest
        @DisplayName("하늘 상태 업데이트")
        @CsvSource({
            "1,CLEAR",
            "3,MOSTLY_CLOUDY",
            "4,CLOUDY",
            "999,CLEAR"  // 잘못된 코드
        })
        void updateSkyStatus(String code, SkyStatus expected) {
            weather.updateWeatherData("SKY", code);

            assertThat(weather.getSkyStatus()).isEqualTo(expected);
        }

        @ParameterizedTest
        @DisplayName("강수 형태 업데이트")
        @CsvSource({
            "0,NONE",
            "1,RAIN",
            "2,RAIN_SNOW",
            "3,SNOW",
            "4,SHOWER",
            "999,NONE"  // 잘못된 코드
        })
        void updatePrecipitationType(String code, PrecipitationType expected) {
            weather.updateWeatherData("PTY", code);

            assertThat(weather.getPrecipitation().type()).isEqualTo(expected);
        }

        @ParameterizedTest
        @DisplayName("강수량 업데이트 - 특수 케이스")
        @CsvSource({
            "강수없음,0.0",
            "1mm 미만,0.5",
            "30.0~50.0mm,30.0",
            "50.0mm 이상,50.0",
            "6.2,6.2",
            "invalid,0.0"
        })
        void updatePrecipitationAmount(String value, double expected) {
            weather.updateWeatherData("PCP", value);

            assertThat(weather.getPrecipitation().amount()).isEqualTo(expected);
        }

        @Test
        @DisplayName("풍속 업데이트 및 풍력 계산")
        void updateWindSpeed() {
            // When & Then - 약함
            weather.updateWeatherData("WSD", "3.5");
            assertThat(weather.getWindSpeed().speed()).isEqualTo(3.5);
            assertThat(weather.getWindSpeed().asWord()).isEqualTo(WindStrength.WEAK);

            // When & Then - 보통
            weather.updateWeatherData("WSD", "6.0");
            assertThat(weather.getWindSpeed().speed()).isEqualTo(6.0);
            assertThat(weather.getWindSpeed().asWord()).isEqualTo(WindStrength.MODERATE);

            // When & Then - 강함
            weather.updateWeatherData("WSD", "10.0");
            assertThat(weather.getWindSpeed().speed()).isEqualTo(10.0);
            assertThat(weather.getWindSpeed().asWord()).isEqualTo(WindStrength.STRONG);
        }

        @Test
        @DisplayName("null 또는 빈 값 처리")
        void updateWithNullOrEmptyValue() {
            // Given
            double originalTemp = weather.getTemperature().current();

            // When
            weather.updateWeatherData(null, "20.0");
            weather.updateWeatherData("TMP", null);
            weather.updateWeatherData("TMP", "");

            // Then
            assertThat(weather.getTemperature().current()).isEqualTo(originalTemp);
        }

        @Test
        @DisplayName("잘못된 숫자 형식 처리")
        void updateWithInvalidNumberFormat() {
            // When
            weather.updateWeatherData("TMP", "invalid");

            // Then
            assertThat(weather.getTemperature().current()).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("WeatherLocation 테스트")
    class WeatherLocationTest {

        @Test
        @DisplayName("WeatherAPILocation에서 WeatherLocation으로 변환")
        void fromWeatherAPILocation() {
            // When
            WeatherLocation location = WeatherLocation.from(TEST_LOCATION);

            // Then
            assertThat(location.getLatitude()).isEqualTo(TEST_LOCATION.latitude());
            assertThat(location.getLongitude()).isEqualTo(TEST_LOCATION.longitude());
            assertThat(location.getX()).isEqualTo(TEST_LOCATION.x());
            assertThat(location.getY()).isEqualTo(TEST_LOCATION.y());
            assertThat(location.getLocationNames()).isEqualTo("서울특별시 중구");
        }

        @Test
        @DisplayName("WeatherLocation에서 WeatherAPILocation으로 변환")
        void toWeatherAPILocation() {
            // Given
            WeatherLocation location = WeatherLocation.from(TEST_LOCATION);

            // When
            WeatherAPILocation apiLocation = location.toApiLocation();

            // Then
            assertThat(apiLocation.latitude()).isEqualTo(TEST_LOCATION.latitude());
            assertThat(apiLocation.longitude()).isEqualTo(TEST_LOCATION.longitude());
            assertThat(apiLocation.x()).isEqualTo(TEST_LOCATION.x());
            assertThat(apiLocation.y()).isEqualTo(TEST_LOCATION.y());
            assertThat(apiLocation.locationNames()).containsExactly("서울특별시 중구");
        }

        @Test
        @DisplayName("여러 위치명 처리")
        void multipleLocationNames() {
            // Given
            WeatherAPILocation apiLocation = new WeatherAPILocation(
                37.5665, 126.9780, 60, 127,
                List.of("서울특별시", "중구", "명동")
            );

            // When
            WeatherLocation location = WeatherLocation.from(apiLocation);
            WeatherAPILocation converted = location.toApiLocation();

            // Then
            assertThat(location.getLocationNames()).isEqualTo("서울특별시,중구,명동");
            assertThat(converted.locationNames()).containsExactly("서울특별시", "중구", "명동");
        }

    }

    @Nested
    @DisplayName("Embedded 클래스 DTO 변환 테스트")
    class EmbeddedClassToDtoTest {

        @Test
        @DisplayName("PrecipitationInfo -> PrecipitationDto 변환")
        void precipitationInfoToDto() {
            // Given
            PrecipitationInfo info = new PrecipitationInfo(
                PrecipitationType.RAIN, 10.5, 80.0
            );

            // When
            PrecipitationDto dto = info.toDto();

            // Then
            assertThat(dto.type()).isEqualTo(PrecipitationType.RAIN);
            assertThat(dto.amount()).isEqualTo(10.5);
            assertThat(dto.probability()).isEqualTo(80.0);
        }

        @Test
        @DisplayName("TemperatureInfo -> TemperatureDto 변환")
        void temperatureInfoToDto() {
            // Given
            TemperatureInfo info = new TemperatureInfo(20.0, 2.5, 15.0, 25.0);

            // When
            TemperatureDto dto = info.toDto();

            // Then
            assertThat(dto.current()).isEqualTo(20.0);
            assertThat(dto.comparedToDayBefore()).isEqualTo(2.5);
            assertThat(dto.min()).isEqualTo(15.0);
            assertThat(dto.max()).isEqualTo(25.0);
        }

        @Test
        @DisplayName("HumidityInfo -> HumidityDto 변환")
        void humidityInfoToDto() {
            // Given
            HumidityInfo info = new HumidityInfo(65.0, 5.0);

            // When
            HumidityDto dto = info.toDto();

            // Then
            assertThat(dto.current()).isEqualTo(65.0);
            assertThat(dto.comparedToDayBefore()).isEqualTo(5.0);
        }

        @Test
        @DisplayName("WindInfo -> WindSpeedDto 변환")
        void windInfoToDto() {
            // Given
            WindInfo info = new WindInfo(7.5, 180.0, 0.0, 7.5, WindStrength.MODERATE);

            // When
            WindSpeedDto dto = info.toDto();

            // Then
            assertThat(dto.speed()).isEqualTo(7.5);
            assertThat(dto.asWord()).isEqualTo(WindStrength.MODERATE);
        }
    }

    @Nested
    @DisplayName("Weather 엔티티 전체 DTO 변환 테스트")
    class WeatherToDtoTest {

        @Test
        @DisplayName("Weather 엔티티의 모든 getter 메서드 테스트")
        void allGetterMethods() {
            // Given
            Weather weather = Weather.create(NOW, NOW.plusHours(1), TEST_LOCATION, SkyStatus.CLEAR);
            weather.updateWeatherData("TMP", "20.0");
            weather.updateWeatherData("TMN", "15.0");
            weather.updateWeatherData("TMX", "25.0");
            weather.updateWeatherData("REH", "60.0");
            weather.updateWeatherData("PTY", "1");
            weather.updateWeatherData("POP", "30.0");
            weather.updateWeatherData("PCP", "5.0");
            weather.updateWeatherData("WSD", "5.5");

            // When
            WeatherAPILocation location = weather.getLocation();
            PrecipitationDto precipitation = weather.getPrecipitation();
            TemperatureDto temperature = weather.getTemperature();
            HumidityDto humidity = weather.getHumidity();
            WindSpeedDto windSpeed = weather.getWindSpeed();

            // Then
            assertThat(location).isNotNull();
            assertThat(location.latitude()).isEqualTo(TEST_LOCATION.latitude());

            assertThat(precipitation).isNotNull();
            assertThat(precipitation.type()).isEqualTo(PrecipitationType.RAIN);
            assertThat(precipitation.amount()).isEqualTo(5.0);
            assertThat(precipitation.probability()).isEqualTo(30.0);

            assertThat(temperature).isNotNull();
            assertThat(temperature.current()).isEqualTo(20.0);
            assertThat(temperature.min()).isEqualTo(15.0);
            assertThat(temperature.max()).isEqualTo(25.0);

            assertThat(humidity).isNotNull();
            assertThat(humidity.current()).isEqualTo(60.0);

            assertThat(windSpeed).isNotNull();
            assertThat(windSpeed.speed()).isEqualTo(5.5);
            assertThat(windSpeed.asWord()).isEqualTo(WindStrength.MODERATE);
        }
    }
}