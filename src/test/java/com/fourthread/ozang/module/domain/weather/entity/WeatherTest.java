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


}