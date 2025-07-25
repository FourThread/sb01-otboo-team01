package com.fourthread.ozang.app.domain.notification.event;

import java.util.UUID;

public record FeedLikedEvent(
        UUID feedId,
        UUID feedUserId,
        String content,
        String likeByUserName
) {
}
