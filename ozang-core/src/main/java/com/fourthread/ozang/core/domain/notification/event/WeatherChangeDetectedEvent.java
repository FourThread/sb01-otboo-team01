package com.fourthread.ozang.core.domain.notification.event;

import com.fourthread.ozang.core.domain.notification.entity.NotificationLevel;


public record WeatherChangeDetectedEvent(
        String title,
        String content,
        NotificationLevel level,
        Integer gridX,
        Integer gridY
) {
}
