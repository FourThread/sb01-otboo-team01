package com.fourthread.ozang.core.domain.weather.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fourthread.ozang.core.domain.weather.dto.type.WeatherChangeType;
import java.time.LocalDateTime;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
public record WeatherChangeDto(
    WeatherChangeType changeType,
    String description,
    double oldValue,
    double newValue,
    String unit,
    LocalDateTime detectedAt,
    WeatherAPILocation location,
    Integer gridX,
    Integer gridY
) {

    public static WeatherChangeDto create(
        WeatherChangeType changeType,
        String description,
        double oldValue,
        double newValue,
        String unit,
        LocalDateTime detectedAt,
        WeatherAPILocation location
    ) {
        return new WeatherChangeDto(
            changeType,
            description,
            oldValue,
            newValue,
            unit,
            detectedAt,
            location,
            location.x(),
            location.y()
        );
    }
}
