package com.fourthread.ozang.core.domain.notification.event;

import com.fourthread.ozang.core.domain.notification.dto.response.NotificationDto;

import java.time.Instant;

public record NotificationCreatedEvent(
        Instant createdAt,
        NotificationDto notificationDto
) {
    public NotificationCreatedEvent(NotificationDto notificationDto) {
        this(Instant.now(), notificationDto);
    }
}
