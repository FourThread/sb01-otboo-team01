package com.fourthread.ozang.module.domain.notification.event;

import com.fourthread.ozang.module.domain.weather.dto.WeatherChangeDto;
import java.time.Instant;
import java.util.List;

public record WeatherChangeDetectedEvent(
    Instant createdAt,
    List<WeatherChangeDto> weatherChanges
) {
    public WeatherChangeDetectedEvent(List<WeatherChangeDto> weatherChanges) {
        this(Instant.now(), weatherChanges);
    }
}