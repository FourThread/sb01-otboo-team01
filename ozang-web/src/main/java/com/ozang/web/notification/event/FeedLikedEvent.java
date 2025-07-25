package com.ozang.web.notification.event;

import java.util.UUID;

public record FeedLikedEvent(
        UUID feedId,
        UUID feedUserId,
        String content,
        String likeByUserName
) {
}
