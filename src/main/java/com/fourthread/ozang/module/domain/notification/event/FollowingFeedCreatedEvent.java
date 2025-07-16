package com.fourthread.ozang.module.domain.notification.event;

import com.fourthread.ozang.module.domain.user.dto.data.UserSummary;

public record FollowingFeedCreatedEvent(
        UserSummary userSummary,
        String content
) {
}
