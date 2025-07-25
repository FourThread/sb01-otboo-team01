package com.fourthread.ozang.app.domain.notification.event;

import com.fourthread.ozang.app.domain.user.dto.data.UserSummary;

public record FollowingFeedCreatedEvent(
        UserSummary userSummary,
        String content
) {
}
