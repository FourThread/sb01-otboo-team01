package com.fourthread.ozang.module.domain.weather.entity;

import com.fourthread.ozang.module.domain.BaseEntity;
import com.fourthread.ozang.module.domain.weather.dto.type.PrecipitationType;
import com.fourthread.ozang.module.domain.weather.dto.type.SkyStatus;
import com.fourthread.ozang.module.domain.weather.dto.type.WindStrength;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Weather extends BaseEntity {

    @Column(nullable = false)
    private LocalDateTime forecastedAt;

    @Column(nullable = false)
    private LocalDateTime forecastAt;

    // 위치 정보
    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(nullable = false)
    private Integer x;

    @Column(nullable = false)
    private Integer y;

    @Column(length = 500)
    private String locationNames;

    // 날씨 정보
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SkyStatus skyStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PrecipitationType precipitationType;

    @Column(nullable = false)
    private Double precipitationAmount;

    @Column(nullable = false)
    private Double precipitationProbability;

    // 온도 정보
    @Column(nullable = false)
    private Double currentTemperature;

    @Column(nullable = false)
    private Double comparedToDayBeforeTemperature;

    @Column(nullable = false)
    private Double minTemperature;

    @Column(nullable = false)
    private Double maxTemperature;

    // 습도 정보
    @Column(nullable = false)
    private Double currentHumidity;

    @Column(nullable = false)
    private Double comparedToDayBeforeHumidity;

    // 풍속 정보
    @Column(nullable = false)
    private Double windSpeed;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WindStrength windStrength;

    public Weather(LocalDateTime forecastedAt, LocalDateTime forecastAt,
        Double latitude, Double longitude, Integer x, Integer y, String locationNames,
        SkyStatus skyStatus, PrecipitationType precipitationType,
        Double precipitationAmount, Double precipitationProbability,
        Double currentTemperature, Double comparedToDayBeforeTemperature,
        Double minTemperature, Double maxTemperature,
        Double currentHumidity, Double comparedToDayBeforeHumidity,
        Double windSpeed, WindStrength windStrength) {
        this.forecastedAt = forecastedAt;
        this.forecastAt = forecastAt;
        this.latitude = latitude;
        this.longitude = longitude;
        this.x = x;
        this.y = y;
        this.locationNames = locationNames;
        this.skyStatus = skyStatus;
        this.precipitationType = precipitationType;
        this.precipitationAmount = precipitationAmount;
        this.precipitationProbability = precipitationProbability;
        this.currentTemperature = currentTemperature;
        this.comparedToDayBeforeTemperature = comparedToDayBeforeTemperature;
        this.minTemperature = minTemperature;
        this.maxTemperature = maxTemperature;
        this.currentHumidity = currentHumidity;
        this.comparedToDayBeforeHumidity = comparedToDayBeforeHumidity;
        this.windSpeed = windSpeed;
        this.windStrength = windStrength;
    }
}