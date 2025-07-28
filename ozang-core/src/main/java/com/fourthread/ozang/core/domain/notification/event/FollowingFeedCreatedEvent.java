package com.fourthread.ozang.core.domain.notification.event;


import com.fourthread.ozang.core.domain.user.dto.data.UserSummary;

public record FollowingFeedCreatedEvent(
        UserSummary userSummary,
        String content
) {
}
