package com.ozang.web.notification.event;

import com.ozang.web.notification.dto.response.NotificationDto;

import java.time.Instant;

public record NotificationCreatedEvent(
        Instant createdAt,
        NotificationDto notificationDto
) {
    public NotificationCreatedEvent(NotificationDto notificationDto) {
        this(Instant.now(), notificationDto);
    }
}
