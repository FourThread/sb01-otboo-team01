package com.ozang.common.domain.weather.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.ozang.common.domain.weather.dto.type.WeatherChangeType;
import java.time.LocalDateTime;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public record WeatherChangeDto(
    WeatherChangeType changeType,
    String description,
    double oldValue,
    double newValue,
    String unit,
    LocalDateTime detectedAt,
    WeatherAPILocation location
) {}
