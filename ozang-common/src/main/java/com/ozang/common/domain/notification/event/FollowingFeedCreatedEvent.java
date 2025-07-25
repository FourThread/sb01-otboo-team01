package com.ozang.common.domain.notification.event;

import com.ozang.common.domain.user.dto.data.UserSummary;

public record FollowingFeedCreatedEvent(
        UserSummary userSummary,
        String content
) {
}
