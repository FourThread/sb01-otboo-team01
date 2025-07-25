package com.ozang.common.domain.notification.event;

import com.ozang.common.domain.notification.dto.response.NotificationDto;

import java.time.Instant;

public record NotificationCreatedEvent(
        Instant createdAt,
        NotificationDto notificationDto
) {
    public NotificationCreatedEvent(NotificationDto notificationDto) {
        this(Instant.now(), notificationDto);
    }
}
