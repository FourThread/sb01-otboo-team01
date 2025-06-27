package com.fourthread.ozang.module.domain.weather.entity;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.fourthread.ozang.module.domain.weather.dto.TemperatureDto;
import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.type.PrecipitationType;
import com.fourthread.ozang.module.domain.weather.dto.type.SkyStatus;
import com.fourthread.ozang.module.domain.weather.dto.type.WindStrength;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
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
}