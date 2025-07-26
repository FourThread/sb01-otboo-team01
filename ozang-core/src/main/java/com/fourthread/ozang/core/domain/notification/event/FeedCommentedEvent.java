package com.fourthread.ozang.core.domain.notification.event;

import java.util.UUID;

public record FeedCommentedEvent(
        UUID feedAuthorUserId,
        String commentUserName,
        String content
) {
}
