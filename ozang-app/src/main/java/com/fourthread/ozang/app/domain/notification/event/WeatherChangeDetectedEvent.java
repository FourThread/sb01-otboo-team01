package com.fourthread.ozang.app.domain.notification.event;

import com.fourthread.ozang.app.domain.notification.entity.NotificationLevel;


public record WeatherChangeDetectedEvent(
        String region,
        String title,
        String content,
        NotificationLevel level
) {
}