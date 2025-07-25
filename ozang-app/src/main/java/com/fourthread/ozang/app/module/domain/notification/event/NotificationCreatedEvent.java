package com.fourthread.ozang.module.domain.notification.event;

import com.fourthread.ozang.module.domain.notification.dto.response.NotificationDto;

import java.time.Instant;

public record NotificationCreatedEvent(
        Instant createdAt,
        NotificationDto notificationDto
) {
    public NotificationCreatedEvent(NotificationDto notificationDto) {
        this(Instant.now(), notificationDto);
    }
}
