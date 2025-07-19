package com.fourthread.ozang.module.domain.weather.dto;

import com.fourthread.ozang.module.domain.weather.dto.type.WeatherChangeType;
import java.time.LocalDateTime;

public record WeatherChangeDto(
    WeatherChangeType changeType,
    String description,
    double oldValue,
    double newValue,
    String unit,
    LocalDateTime detectedAt,
    WeatherAPILocation location
) {}
