package com.ozang.web.notification.event;

import java.util.UUID;

public record FeedCommentedEvent(
        UUID feedAuthorUserId,
        String commentUserName,
        String content
) {
}
