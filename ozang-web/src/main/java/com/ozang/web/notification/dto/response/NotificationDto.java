package com.fourthread.ozang.module.domain.notification.dto.response;

import com.fourthread.ozang.module.domain.notification.entity.NotificationLevel;

import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
        UUID id,
        Instant createdAt,
        UUID receiverId,
        String title,
        String content,
        NotificationLevel level
) {
}
