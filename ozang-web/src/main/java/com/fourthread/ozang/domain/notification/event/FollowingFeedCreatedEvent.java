package com.fourthread.ozang.domain.notification.event;

import com.fourthread.ozang.domain.user.dto.data.UserSummary;

public record FollowingFeedCreatedEvent(
        UserSummary userSummary,
        String content
) {
}
