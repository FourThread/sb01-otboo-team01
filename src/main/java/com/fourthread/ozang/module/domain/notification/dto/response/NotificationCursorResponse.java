package com.fourthread.ozang.module.domain.notification.dto.response;

import java.util.List;
import java.util.UUID;

public record NotificationCursorResponse(
        List<NotificationDto> data,
        String nextCursor,
        UUID nextIdAfter,
        boolean hasNext,
        long totalCount,
        String sortBy,
        String sortDirection
) {
}
