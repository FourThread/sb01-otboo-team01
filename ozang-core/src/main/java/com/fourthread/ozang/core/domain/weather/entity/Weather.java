package com.fourthread.ozang.core.domain.weather.entity;


import com.fourthread.ozang.core.domain.BaseEntity;
import com.fourthread.ozang.core.domain.weather.dto.HumidityDto;
import com.fourthread.ozang.core.domain.weather.dto.PrecipitationDto;
import com.fourthread.ozang.core.domain.weather.dto.TemperatureDto;
import com.fourthread.ozang.core.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.core.domain.weather.dto.WindSpeedDto;
import com.fourthread.ozang.core.domain.weather.dto.type.PrecipitationType;
import com.fourthread.ozang.core.domain.weather.dto.type.SkyStatus;
import com.fourthread.ozang.core.domain.weather.dto.type.WindStrength;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "weathers", schema = "public")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Weather extends BaseEntity {

    @Column(nullable = false)
    private LocalDateTime forecastedAt;  // 예보 발표 시간

    @Column(nullable = false)
    private LocalDateTime forecastAt;     // 예보 대상 시간

    @Embedded
    private WeatherLocation location;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkyStatus skyStatus;

    @Embedded
    private PrecipitationInfo precipitation;

    @Embedded
    private TemperatureInfo temperature;

    @Embedded
    private HumidityInfo humidity;

    @Embedded
    private WindInfo wind;

    public static Weather create(
        LocalDateTime forecastedAt,
        LocalDateTime forecastAt,
        WeatherAPILocation apiLocation,
        SkyStatus skyStatus) {

        return Weather.builder()
            .forecastedAt(forecastedAt)
            .forecastAt(forecastAt)
            .location(WeatherLocation.from(apiLocation))
            .skyStatus(skyStatus)
            .precipitation(new PrecipitationInfo())
            .temperature(new TemperatureInfo())
            .humidity(new HumidityInfo())
            .wind(new WindInfo())
            .build();
    }

    public void updateWeatherData(String category, String value) {
        if (category == null || value == null || value.isEmpty()) {
            return;
        }

        try {
            switch (category) {
                case "TMP" -> this.temperature.setCurrent(parseDouble(value));
                case "TMN" -> this.temperature.setMin(parseDouble(value));
                case "TMX" -> this.temperature.setMax(parseDouble(value));
                case "SKY" -> this.skyStatus = parseSkyStatus(value);
                case "PTY" -> this.precipitation.setType(parsePrecipitationType(value));
                case "POP" -> this.precipitation.setProbability(parseDouble(value));
                case "PCP" -> this.precipitation.setAmount(parsePrecipitationAmount(value));
                case "REH" -> this.humidity.setCurrent(parseDouble(value));
                case "WSD" -> {
                    double speed = parseDouble(value);
                    this.wind.setSpeed(speed);
                    this.wind.setStrength(WindStrength.fromSpeed(speed));
                }
                case "VEC" -> this.wind.setDirection(parseDouble(value));
                case "UUU" -> this.wind.setUComponent(parseDouble(value));
                case "VVV" -> this.wind.setVComponent(parseDouble(value));
            }
        } catch (Exception e) {
            // 개별 데이터 파싱 실패는 무시
        }
    }

    public void calculateAndSetTemperatureStats(List<Double> temperatureValues) {
        if (temperatureValues == null || temperatureValues.isEmpty()) {
            return;
        }

        DoubleSummaryStatistics stats = temperatureValues.stream()
            .mapToDouble(Double::doubleValue)
            .summaryStatistics();

        this.temperature.setCurrent(stats.getAverage());

        // TMN, TMX 값이 없는 경우에만 통계값으로 설정
        if (this.temperature.getMin() == 0.0) {
            this.temperature.setMin(stats.getMin());
        }
        if (this.temperature.getMax() == 0.0) {
            this.temperature.setMax(stats.getMax());
        }
    }

    private Double parseDouble(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private SkyStatus parseSkyStatus(String code) {
        return switch (code) {
            case "1" -> SkyStatus.CLEAR;
            case "3" -> SkyStatus.MOSTLY_CLOUDY;
            case "4" -> SkyStatus.CLOUDY;
            default -> SkyStatus.CLEAR;
        };
    }

    private PrecipitationType parsePrecipitationType(String code) {
        return switch (code) {
            case "0" -> PrecipitationType.NONE;
            case "1" -> PrecipitationType.RAIN;
            case "2" -> PrecipitationType.RAIN_SNOW;
            case "3" -> PrecipitationType.SNOW;
            case "4" -> PrecipitationType.SHOWER;
            default -> PrecipitationType.NONE;
        };
    }

    private Double parsePrecipitationAmount(String value) {
        if (value == null || value.equals("강수없음")) {
            return 0.0;
        }

        // "1mm 미만" 처리
        if (value.contains("mm 미만")) {
            return 0.5;
        }

        // "30.0~50.0mm" 형태 처리
        if (value.contains("~")) {
            String[] parts = value.replace("mm", "").split("~");
            return Double.parseDouble(parts[0]);
        }

        // "50.0mm 이상" 처리
        if (value.contains("mm 이상")) {
            return 50.0;
        }

        // 일반 숫자 처리 (예: "6.2")
        try {
            return Double.parseDouble(value.replace("mm", "").trim());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    public WeatherAPILocation getLocation() {
        return location.toApiLocation();
    }

    public PrecipitationDto getPrecipitation() {
        return precipitation.toDto();
    }

    public TemperatureDto getTemperature() {
        return temperature.toDto();
    }

    public HumidityDto getHumidity() {
        return humidity.toDto();
    }

    public WindSpeedDto getWindSpeed() {
        return wind.toDto();
    }

    /**
     * 내부 Info 객체 접근을 위한 getter 메서드들 (날씨 변화 감지용)
     */
    public TemperatureInfo getTemperatureInfo() {
        return temperature;
    }

    public HumidityInfo getHumidityInfo() {
        return humidity;
    }

    public PrecipitationInfo getPrecipitationInfo() {
        return precipitation;
    }

    public WindInfo getWindInfo() {
        return wind;
    }
}