package com.fourthread.ozang.module.domain.notification.event;

import java.util.UUID;

public record FollowingFeedCreatedEvent(
        UUID userId,
        String content
) {
}
