package com.fourthread.ozang.module.domain.weather.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.type.SkyStatus;
import com.fourthread.ozang.module.domain.weather.entity.Weather;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("Weather Repository 테스트")
@EnableJpaRepositories(basePackages = "com.fourthread.ozang.module.domain.weather.repository")
@EnableJpaAuditing
class WeatherRepositoryTest {

    @Autowired
    private WeatherRepository weatherRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final Integer GRID_X = 60;
    private static final Integer GRID_Y = 127;
    private static final WeatherAPILocation TEST_LOCATION = new WeatherAPILocation(
        37.5665, 126.9780, GRID_X, GRID_Y, List.of("서울특별시 중구")
    );

    @Nested
    @DisplayName("기본 CRUD 테스트")
    class BasicCrudTest {

        @Test
        @DisplayName("Weather 엔티티 저장")
        void saveWeather() {
            // Given
            LocalDateTime fixedTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            Weather weather = createWeather(fixedTime);

            // When
            Weather saved = weatherRepository.save(weather);
            entityManager.flush();
            entityManager.clear();

            // Then
            assertThat(saved.getId()).isNotNull();
            assertThat(saved.getCreatedAt()).isNotNull();

            Weather found = weatherRepository.findById(saved.getId()).orElseThrow();
            assertThat(found.getForecastedAt()).isCloseTo(weather.getForecastedAt(), within(1, ChronoUnit.SECONDS));
            assertThat(found.getSkyStatus()).isEqualTo(weather.getSkyStatus());
        }

        @Test
        @DisplayName("Weather 엔티티 조회")
        void findById() {
            // Given
            Weather weather = weatherRepository.save(createWeather(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
            entityManager.flush();
            entityManager.clear();

            // When
            Optional<Weather> found = weatherRepository.findById(weather.getId());

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getId()).isEqualTo(weather.getId());
        }

        @Test
        @DisplayName("Weather 엔티티 수정")
        void updateWeather() {
            // Given
            Weather weather = weatherRepository.save(createWeather(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
            entityManager.flush();
            entityManager.clear();

            // When
            Weather found = weatherRepository.findById(weather.getId()).orElseThrow();
            found.updateWeatherData("TMP", "25.0");
            found.updateWeatherData("SKY", "4");
            entityManager.flush();
            entityManager.clear();

            // Then
            Weather updated = weatherRepository.findById(weather.getId()).orElseThrow();
            assertThat(updated.getTemperature().current()).isEqualTo(25.0);
            assertThat(updated.getSkyStatus()).isEqualTo(SkyStatus.CLOUDY);
        }

        @Test
        @DisplayName("Weather 엔티티 삭제")
        void deleteWeather() {
            // Given
            Weather weather = weatherRepository.save(createWeather(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS)));
            entityManager.flush();

            // When
            weatherRepository.delete(weather);
            entityManager.flush();
            entityManager.clear();

            // Then
            Optional<Weather> found = weatherRepository.findById(weather.getId());
            assertThat(found).isEmpty();
        }
    }

    @Nested
    @DisplayName("커스텀 쿼리 메서드 테스트")
    class CustomQueryTest {

        @Test
        @DisplayName("격자 좌표로 최신 날씨 조회")
        void findLatestByGridCoordinate() {
            // Given
            LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            Weather weather1 = createWeather(now.minusHours(3));
            Weather weather2 = createWeather(now.minusHours(2));
            Weather weather3 = createWeather(now.minusHours(1)); // 가장 최신
            Weather weatherDifferentGrid = createWeatherWithDifferentGrid(now);

            weatherRepository.save(weather1);
            weatherRepository.save(weather2);
            weatherRepository.save(weather3);
            weatherRepository.save(weatherDifferentGrid);
            entityManager.flush();
            entityManager.clear();

            // When
            Optional<Weather> latest = weatherRepository.findLatestByGridCoordinate(GRID_X, GRID_Y);

            // Then
            assertThat(latest).isPresent();
            assertThat(latest.get().getId()).isEqualTo(weather3.getId());
            assertThat(latest.get().getForecastedAt()).isEqualTo(weather3.getForecastedAt());
        }

        @Test
        @DisplayName("격자 좌표에 해당하는 데이터가 없을 때")
        void findLatestByGridCoordinate_NoData() {
            // Given
            Weather weather = createWeatherWithDifferentGrid(LocalDateTime.now());
            weatherRepository.save(weather);
            entityManager.flush();
            entityManager.clear();

            // When
            Optional<Weather> result = weatherRepository.findLatestByGridCoordinate(999, 999);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("동일 격자에 여러 날씨 데이터가 있을 때 최신 데이터만 반환")
        void findLatestByGridCoordinate_MultipleData() {
            // Given
            LocalDateTime baseTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
            for (int i = 0; i < 10; i++) {
                Weather weather = createWeather(baseTime.minusHours(i));
                weatherRepository.save(weather);
            }
            entityManager.flush();
            entityManager.clear();

            // When
            Optional<Weather> latest = weatherRepository.findLatestByGridCoordinate(GRID_X, GRID_Y);

            // Then
            assertThat(latest).isPresent();
            assertThat(latest.get().getForecastedAt()).isEqualTo(baseTime);
        }
    }

    @Nested
    @DisplayName("API 응답 해시 유니크 제약 테스트")
    class ApiResponseHashTest {

        @Test
        @DisplayName("동일한 API 응답 해시로 저장 시도 시 예외 발생")
        void duplicateApiResponseHash() {
            // Given
            String duplicateHash = "duplicate-hash-value";
            Weather weather1 = createWeather(LocalDateTime.now());
            weather1.setApiResponseHash(duplicateHash);
            weatherRepository.save(weather1);
            entityManager.flush();

            Weather weather2 = createWeather(LocalDateTime.now().plusHours(1));
            weather2.setApiResponseHash(duplicateHash);

            // When & Then
            assertThatThrownBy(() -> {
                weatherRepository.save(weather2);
                entityManager.flush();
            }).isInstanceOf(Exception.class);
        }

        @Test
        @DisplayName("서로 다른 API 응답 해시는 정상 저장")
        void differentApiResponseHash() {
            // Given
            Weather weather1 = createWeather(LocalDateTime.now());
            weather1.setApiResponseHash("hash-1");
            Weather weather2 = createWeather(LocalDateTime.now().plusHours(1));
            weather2.setApiResponseHash("hash-2");

            // When & Then
            assertThatCode(() -> {
                weatherRepository.save(weather1);
                weatherRepository.save(weather2);
                entityManager.flush();
            }).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("API 응답 해시가 null인 경우 허용")
        void nullApiResponseHash() {
            // Given
            Weather weather1 = createWeather(LocalDateTime.now());
            weather1.setApiResponseHash(null);
            Weather weather2 = createWeather(LocalDateTime.now().plusHours(1));
            weather2.setApiResponseHash(null);

            // When & Then
            assertThatCode(() -> {
                weatherRepository.save(weather1);
                weatherRepository.save(weather2);
                entityManager.flush();
            }).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Embedded 타입 필드 영속성 테스트")
    class EmbeddedFieldPersistenceTest {

        @Test
        @DisplayName("WeatherLocation 필드 저장 및 조회")
        void weatherLocationPersistence() {
            // Given
            Weather weather = createWeather(LocalDateTime.now());
            Weather saved = weatherRepository.save(weather);
            entityManager.flush();
            entityManager.clear();

            // When
            Weather found = weatherRepository.findById(saved.getId()).orElseThrow();

            // Then
            assertThat(found.getLocation()).isNotNull();
            assertThat(found.getLocation().latitude()).isEqualTo(TEST_LOCATION.latitude());
            assertThat(found.getLocation().longitude()).isEqualTo(TEST_LOCATION.longitude());
            assertThat(found.getLocation().x()).isEqualTo(TEST_LOCATION.x());
            assertThat(found.getLocation().y()).isEqualTo(TEST_LOCATION.y());
            assertThat(found.getLocation().locationNames()).containsExactly("서울특별시 중구");
        }

        @Test
        @DisplayName("날씨 데이터 필드 저장 및 조회")
        void weatherDataPersistence() {
            // Given
            Weather weather = createWeather(LocalDateTime.now());
            weather.updateWeatherData("TMP", "22.5");
            weather.updateWeatherData("REH", "65.0");
            weather.updateWeatherData("WSD", "7.5");
            weather.updateWeatherData("PTY", "1");
            weather.updateWeatherData("POP", "80.0");
            weather.updateWeatherData("PCP", "10.5");

            Weather saved = weatherRepository.save(weather);
            entityManager.flush();
            entityManager.clear();

            // When
            Weather found = weatherRepository.findById(saved.getId()).orElseThrow();

            // Then
            assertThat(found.getTemperature().current()).isEqualTo(22.5);
            assertThat(found.getHumidity().current()).isEqualTo(65.0);
            assertThat(found.getWindSpeed().speed()).isEqualTo(7.5);
            assertThat(found.getPrecipitation().type().toString()).isEqualTo("RAIN");
            assertThat(found.getPrecipitation().probability()).isEqualTo(80.0);
            assertThat(found.getPrecipitation().amount()).isEqualTo(10.5);
        }
    }

    @Nested
    @DisplayName("대량 데이터 처리 테스트")
    class BulkDataTest {

        @Test
        @DisplayName("대량 데이터 저장 성능")
        void bulkSavePerformance() {
            // Given
            int dataCount = 10000;
            LocalDateTime baseTime = LocalDateTime.now();

            // When
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < dataCount; i++) {
                Weather weather = createWeather(baseTime.minusMinutes(i));
                weather.setApiResponseHash("hash-" + i);
                weatherRepository.save(weather);
            }
            entityManager.flush();
            long endTime = System.currentTimeMillis();

            // Then
            long executionTime = endTime - startTime;
            assertThat(weatherRepository.count()).isEqualTo(dataCount);
            assertThat(executionTime).isLessThan(5000); // 5초 이내 완료
        }

        @Test
        @DisplayName("대량 데이터에서 최신 데이터 조회 성능")
        void findLatestFromBulkData() {
            // Given
            int dataCount = 100;
            LocalDateTime latestTime = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

            for (int i = 1; i <= dataCount; i++) {
                Weather weather = createWeather(latestTime.minusHours(i));
                weatherRepository.save(weather);
            }

            Weather latestWeather = createWeather(latestTime);
            weatherRepository.save(latestWeather);
            entityManager.flush();
            entityManager.clear();

            // When
            long startTime = System.currentTimeMillis();
            Optional<Weather> found = weatherRepository.findLatestByGridCoordinate(GRID_X, GRID_Y);
            long endTime = System.currentTimeMillis();

            // Then
            assertThat(found).isPresent();
            assertThat(found.get().getForecastedAt()).isEqualTo(latestTime);
            assertThat(endTime - startTime).isLessThan(100); // 100ms 이내 조회
        }
    }

    private Weather createWeather(LocalDateTime forecastedAt) {
        Weather weather = Weather.create(
            forecastedAt,
            forecastedAt.plusHours(1),
            TEST_LOCATION,
            SkyStatus.CLEAR
        );
        return weather;
    }

    private Weather createWeatherWithDifferentGrid(LocalDateTime time) {
        WeatherAPILocation differentLocation = new WeatherAPILocation(
            35.1796, 129.0756, 98, 76, List.of("부산광역시 해운대구")
        );
        return Weather.create(
            time,
            time.plusHours(1),
            differentLocation,
            SkyStatus.CLOUDY
        );
    }
}