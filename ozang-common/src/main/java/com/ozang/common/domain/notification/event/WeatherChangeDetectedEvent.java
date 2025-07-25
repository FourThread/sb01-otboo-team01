package com.ozang.common.domain.notification.event;

import com.ozang.common.domain.weather.dto.WeatherChangeDto;
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