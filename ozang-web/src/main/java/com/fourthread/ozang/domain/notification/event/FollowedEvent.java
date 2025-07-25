package com.fourthread.ozang.domain.notification.event;

import com.fourthread.ozang.domain.follow.dto.FollowDto;

public record FollowedEvent(
        FollowDto dto
) {
}
