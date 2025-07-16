package com.fourthread.ozang.module.domain.notification.event;

import com.fourthread.ozang.module.domain.user.entity.User;

public record FollowingFeedCreatedEvent(
        User user,
        String content
) {
}
