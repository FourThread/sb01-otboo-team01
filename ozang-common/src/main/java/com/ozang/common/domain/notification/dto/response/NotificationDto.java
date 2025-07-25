package com.ozang.common.domain.notification.dto.response;

import com.ozang.common.domain.notification.entity.NotificationLevel;

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
