package com.fourthread.ozang.core.domain.notification.event;

import com.fourthread.ozang.core.domain.notification.entity.NotificationLevel;


public record WeatherChangeDetectedEvent(
        String region,
        String title,
        String content,
        NotificationLevel level
) {
}