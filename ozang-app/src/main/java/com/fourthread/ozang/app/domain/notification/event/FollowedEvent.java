package com.fourthread.ozang.app.domain.notification.event;

import com.fourthread.ozang.app.domain.follow.dto.FollowDto;

public record FollowedEvent(
        FollowDto dto
) {
}
