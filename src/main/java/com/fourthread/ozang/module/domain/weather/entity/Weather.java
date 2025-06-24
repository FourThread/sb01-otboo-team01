package com.fourthread.ozang.module.domain.weather.entity;

import com.fourthread.ozang.module.domain.BaseEntity;
import com.fourthread.ozang.module.domain.weather.dto.HumidityDto;
import com.fourthread.ozang.module.domain.weather.dto.PrecipitationDto;
import com.fourthread.ozang.module.domain.weather.dto.TemperatureDto;
import com.fourthread.ozang.module.domain.weather.dto.WeatherAPILocation;
import com.fourthread.ozang.module.domain.weather.dto.WindSpeedDto;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "weather_data", indexes = {
    @Index(name = "idx_weather_coordinate", columnList = "x, y"),
    @Index(name = "idx_weather_forecast_at", columnList = "forecast_at"),
    @Index(name = "idx_weather_location", columnList = "latitude, longitude"),
    @Index(name = "idx_weather_api_hash", columnList = "api_response_hash")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Weather extends BaseEntity {

    // =============== 기본 정보 ===============
    @Column(name = "forecasted_at", nullable = false)
    private LocalDateTime forecastedAt;

    @Column(name = "forecast_at", nullable = false)
    private LocalDateTime forecastAt;

    // =============== 위치 정보 ===============
    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @Column(name = "x", nullable = false)
    private Integer x;

    @Column(name = "y", nullable = false)
    private Integer y;

    @ElementCollection(fetch = FetchType.LAZY)
    @Column(name = "location_name")
    private List<String> locationNames;

    // =============== 날씨 상태 ===============
    @Enumerated(EnumType.STRING)
    @Column(name = "sky_status", nullable = false)
    private SkyStatus skyStatus;

    // =============== 강수 정보 ===============
    @Enumerated(EnumType.STRING)
    @Column(name = "precipitation_type", nullable = false)
    private PrecipitationType precipitationType;

    @Column(name = "precipitation_amount")
    private Double precipitationAmount;

    @Column(name = "precipitation_probability")
    private Double precipitationProbability;

    // =============== 온도 정보 ===============
    @Column(name = "temperature_current")
    private Double temperatureCurrent;

    @Column(name = "temperature_compared_to_day_before")
    private Double temperatureComparedToDayBefore;

    @Column(name = "temperature_min")
    private Double temperatureMin;

    @Column(name = "temperature_max")
    private Double temperatureMax;

    // =============== 습도 정보 ===============
    @Column(name = "humidity_current")
    private Double humidityCurrent;

    @Column(name = "humidity_compared_to_day_before")
    private Double humidityComparedToDayBefore;

    // =============== 바람 정보 ===============
    @Column(name = "wind_speed")
    private Double windSpeed;

    @Enumerated(EnumType.STRING)
    @Column(name = "wind_speed_level")
    private WindSpeedLevel windSpeedLevel;

    // =============== API 관련 정보 ===============
    @Column(name = "api_response_hash", unique = true, length = 100)
    private String apiResponseHash;

    // =============== 생성자 ===============
    public Weather(LocalDateTime forecastedAt, LocalDateTime forecastAt,
        WeatherAPILocation location, SkyStatus skyStatus) {
        this.forecastedAt = forecastedAt;
        this.forecastAt = forecastAt;
        this.latitude = location.latitude();
        this.longitude = location.longitude();
        this.x = location.x();
        this.y = location.y();
        this.locationNames = location.locationNames();
        this.skyStatus = skyStatus;
        this.precipitationType = PrecipitationType.NONE;
    }

    // =============== DTO 변환 메서드 ===============
    public WeatherAPILocation getLocation() {
        return new WeatherAPILocation(latitude, longitude, x, y, locationNames);
    }

    public PrecipitationDto getPrecipitation() {
        return new PrecipitationDto(precipitationType, precipitationAmount, precipitationProbability);
    }

    public TemperatureDto getTemperature() {
        return new TemperatureDto(temperatureCurrent, temperatureComparedToDayBefore, temperatureMin, temperatureMax);
    }

    public HumidityDto getHumidity() {
        return new HumidityDto(humidityCurrent, humidityComparedToDayBefore);
    }

    public WindSpeedDto getWindSpeed() {
        return new WindSpeedDto(windSpeed, windSpeedLevel);
    }

    // =============== 날씨 변화 감지 ===============
    public boolean hasSignificantChange(Weather previous) {
        if (previous == null) return true;

        // 기온 변화 감지 (5도 이상)
        if (hasSignificantTemperatureChange(previous)) {
            return true;
        }

        // 강수 타입 변화 감지
        if (hasPrecipitationTypeChange(previous)) {
            return true;
        }

        // 하늘 상태 급격한 변화 감지 (맑음 ↔ 흐림)
        if (hasSkyStatusChange(previous)) {
            return true;
        }

        return false;
    }

    private boolean hasSignificantTemperatureChange(Weather previous) {
        if (temperatureCurrent == null || previous.getTemperatureCurrent() == null) {
            return false;
        }
        double tempDiff = Math.abs(temperatureCurrent - previous.getTemperatureCurrent());
        return tempDiff >= 5.0;
    }

    private boolean hasPrecipitationTypeChange(Weather previous) {
        return precipitationType != previous.getPrecipitationType();
    }

    private boolean hasSkyStatusChange(Weather previous) {
        if (skyStatus == previous.getSkyStatus()) {
            return false;
        }

        // 맑음 ↔ 흐림 같은 극단적 변화만 감지
        return (skyStatus == SkyStatus.CLEAR && previous.getSkyStatus() == SkyStatus.CLOUDY) ||
            (skyStatus == SkyStatus.CLOUDY && previous.getSkyStatus() == SkyStatus.CLEAR);
    }

    // =============== 날씨 데이터 업데이트 ===============
    public void updateWeatherData(String category, String value) {
        try {
            switch (category) {
                // 온도 관련
                case "TMP" -> this.temperatureCurrent = parseDouble(value);
                case "TMN" -> this.temperatureMin = parseDouble(value);
                case "TMX" -> this.temperatureMax = parseDouble(value);

                // 습도
                case "REH" -> this.humidityCurrent = parseDouble(value);

                // 강수 관련
                case "POP" -> this.precipitationProbability = parseDouble(value);
                case "PTY" -> this.precipitationType = PrecipitationType.fromCode(value);
                case "PCP" -> this.precipitationAmount = parsePrecipitationAmount(value);
                case "R1H" -> this.precipitationAmount = parsePrecipitationAmount(value); // 1시간 강수량

                // 바람 관련
                case "WSD" -> {
                    this.windSpeed = parseDouble(value);
                    this.windSpeedLevel = WindSpeedLevel.fromSpeed(this.windSpeed);
                }

                // 하늘 상태
                case "SKY" -> this.skyStatus = SkyStatus.fromCode(value);

                // 실황정보 전용 카테고리 (기존 필드 재사용)
                case "T1H" -> this.temperatureCurrent = parseDouble(value); // 기온 (실황)
                case "RN1" -> this.precipitationAmount = parsePrecipitationAmount(value); // 1시간 강수량

                default -> {
                    // 알 수 없는 카테고리는 로그만 출력
                    System.out.println("Unknown weather category: " + category + " with value: " + value);
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to update weather data for category: " + category +
                " with value: " + value + ". Error: " + e.getMessage());
        }
    }

    // =============== 파싱 유틸리티 메서드 ===============
    private Double parseDouble(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Double.valueOf(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 기상청 API 강수량 데이터를 파싱하는 메서드
     *
     * 기상청 API에서 강수량은 다양한 형태로 반환됩니다:
     * - "강수없음" / "없음" / "0" → 0.0
     * - "1.0mm 미만" → 0.1 (측정 한계값)
     * - "30.0mm 이상" → 실제값 + 10.0 (추정값)
     * - "5.2mm" / "5.2" → 5.2 (정확한 값)
     *
     * @param rawValue 기상청 API에서 받은 원본 강수량 문자열
     * @return 파싱된 강수량 (mm), 파싱 실패시 null
     */
    private Double parsePrecipitationAmount(String rawValue) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return null;
        }

        String cleanValue = rawValue.trim();

        // 강수없음 케이스 처리
        if ("강수없음".equals(cleanValue) || "없음".equals(cleanValue) ||
            "0".equals(cleanValue) || "0.0".equals(cleanValue)) {
            return 0.0;
        }

        try {
            // "mm" 단위 제거
            cleanValue = cleanValue.replace("mm", "").trim();

            // "1.0 미만" 케이스 처리 (측정 한계값)
            if (cleanValue.contains("미만")) {
                return 0.1; // 0.1mm로 처리 (측정 가능한 최소값)
            }

            // "30.0 이상" 케이스 처리 (상한값 초과)
            if (cleanValue.contains("이상")) {
                String numericPart = cleanValue.replace("이상", "").trim();
                Double baseAmount = Double.valueOf(numericPart);
                return baseAmount + 10.0; // 실제값보다 높게 추정
            }

            // 일반적인 숫자값 파싱
            return Double.valueOf(cleanValue);

        } catch (NumberFormatException e) {
            // 파싱 실패시 null 반환 (로그 출력)
            System.err.println("Failed to parse precipitation amount: " + rawValue);
            return null;
        }
    }

    // =============== API 해시 관리 ===============
    public void setApiResponseHash(String hash) {
        this.apiResponseHash = hash;
    }

    // =============== 전날 대비 온도 계산 ===============
    public void calculateTemperatureComparison(Weather previousDayWeather) {
        if (previousDayWeather != null &&
            this.temperatureCurrent != null &&
            previousDayWeather.getTemperatureCurrent() != null) {
            this.temperatureComparedToDayBefore =
                this.temperatureCurrent - previousDayWeather.getTemperatureCurrent();
        }
    }

    // =============== 전날 대비 습도 계산 ===============
    public void calculateHumidityComparison(Weather previousDayWeather) {
        if (previousDayWeather != null &&
            this.humidityCurrent != null &&
            previousDayWeather.getHumidityCurrent() != null) {
            this.humidityComparedToDayBefore =
                this.humidityCurrent - previousDayWeather.getHumidityCurrent();
        }
    }

    // =============== 데이터 유효성 검증 ===============
    public boolean isValidWeatherData() {
        // 기본 위치 정보 검증
        if (latitude == null || longitude == null || x == null || y == null) {
            return false;
        }

        // 예보 시간 검증
        if (forecastedAt == null || forecastAt == null) {
            return false;
        }

        // 하늘 상태는 필수
        if (skyStatus == null) {
            return false;
        }

        return true;
    }

    // =============== 날씨 데이터 요약 정보 ===============
    public String getWeatherSummary() {
        StringBuilder summary = new StringBuilder();

        if (temperatureCurrent != null) {
            summary.append("기온: ").append(temperatureCurrent).append("°C ");
        }

        summary.append("날씨: ").append(skyStatus.getDescription()).append(" ");

        if (precipitationType != PrecipitationType.NONE) {
            summary.append("강수: ").append(precipitationType.getDescription()).append(" ");
        }

        if (windSpeed != null && windSpeed > 0) {
            summary.append("바람: ").append(windSpeedLevel.getDescription()).append(" ");
        }

        return summary.toString().trim();
    }

    // =============== 위험 날씨 여부 판단 ===============
    public boolean isDangerousWeather() {
        // 강한 바람
        if (windSpeedLevel == WindSpeedLevel.STRONG) {
            return true;
        }

        // 많은 강수량 (시간당 20mm 이상)
        if (precipitationAmount != null && precipitationAmount >= 20.0) {
            return true;
        }

        // 극한 온도 (영하 10도 이하 또는 영상 35도 이상)
        if (temperatureCurrent != null &&
            (temperatureCurrent <= -10.0 || temperatureCurrent >= 35.0)) {
            return true;
        }

        return false;
    }

    // =============== toString 오버라이드 ===============
    @Override
    public String toString() {
        return String.format("Weather{id=%s, location=(%s,%s), forecastAt=%s, summary='%s'}",
            getId(), latitude, longitude, forecastAt, getWeatherSummary());
    }
}