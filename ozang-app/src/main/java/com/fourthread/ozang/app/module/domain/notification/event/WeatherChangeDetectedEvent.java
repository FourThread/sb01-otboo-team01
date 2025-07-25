package com.fourthread.ozang.module.domain.notification.event;

import com.fourthread.ozang.module.domain.notification.entity.NotificationLevel;


public record WeatherChangeDetectedEvent(
        String region,
        String title,
        String content,
        NotificationLevel level
) {
}