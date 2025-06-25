package com.fourthread.ozang.module.domain.weather.entity;

import com.fourthread.ozang.module.domain.BaseEntity;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "weather_alerts")
public class WeatherAlert extends BaseEntity {
    @Embedded
    private GridCoordinate gridCoordinate;

    @Enumerated(EnumType.STRING)
    private WeatherAlertType alertType;

    private String title;
    private String message;

    @Enumerated(EnumType.STRING)
    private AlertLevel alertLevel;

    private boolean processed;
    private LocalDateTime processedAt;
    private int targetUserCount;

    protected WeatherAlert() {}

    public static WeatherAlert createTemperatureAlert(GridCoordinate coordinate, WeatherAlertType type, double temperatureChange) {
        WeatherAlert alert = new WeatherAlert();
        alert.gridCoordinate = coordinate;
        alert.alertType = type;
        alert.alertLevel = AlertLevel.INFO;
        alert.title = "기온 변화 알림";
        alert.message = buildTemperatureMessage(type, temperatureChange);
        alert.processed = false;
        return alert;
    }

    public static WeatherAlert createPrecipitationAlert(GridCoordinate coordinate, WeatherAlertType type) {
        WeatherAlert alert = new WeatherAlert();
        alert.gridCoordinate = coordinate;
        alert.alertType = type;
        alert.alertLevel = AlertLevel.WARNING;
        alert.title = "날씨 변화 알림";
        alert.message = buildPrecipitationMessage(type);
        alert.processed = false;
        return alert;
    }

    private static String buildTemperatureMessage(WeatherAlertType type, double change) {
        return String.format("기온이 %.1f도 %s했습니다.", Math.abs(change),
            type == WeatherAlertType.TEMPERATURE_RISE ? "상승" : "하강");
    }

    private static String buildPrecipitationMessage(WeatherAlertType type) {
        return switch (type) {
            case PRECIPITATION_START -> "비가 시작될 예정입니다.";
            case PRECIPITATION_CHANGE -> "강수 형태가 변경될 예정입니다.";
            default -> "날씨가 변화할 예정입니다.";
        };
    }

    public void markAsProcessed(int userCount) {
        this.processed = true;
        this.processedAt = LocalDateTime.now();
        this.targetUserCount = userCount;
    }
}
