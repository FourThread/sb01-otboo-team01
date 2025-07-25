package com.fourthread.ozang.domain.notification.event;

import com.fourthread.ozang.domain.weather.dto.WeatherChangeDto;
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