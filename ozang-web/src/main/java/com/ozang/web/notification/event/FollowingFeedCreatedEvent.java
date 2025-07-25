package com.ozang.web.notification.event;

import com.ozang.web.user.dto.data.UserSummary;

public record FollowingFeedCreatedEvent(
        UserSummary userSummary,
        String content
) {
}
