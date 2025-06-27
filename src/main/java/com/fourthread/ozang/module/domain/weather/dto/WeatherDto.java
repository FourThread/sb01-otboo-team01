package com.fourthread.ozang.module.domain.weather.dto;

import com.fourthread.ozang.module.domain.weather.dto.type.SkyStatus;
import java.time.LocalDateTime;
import java.util.UUID;

public record WeatherDto(
    UUID id,
    LocalDateTime forecastedAt, //예보된 시간
    LocalDateTime forecastAt, //예보 대상 시간
    WeatherAPILocation location,
    SkyStatus skyStatus,
    PrecipitationDto precipitation,
    HumidityDto humidity,
    TemperatureDto temperature,
    WindSpeedDto windSpeed
) {}