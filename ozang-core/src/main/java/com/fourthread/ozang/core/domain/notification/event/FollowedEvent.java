package com.fourthread.ozang.core.domain.notification.event;


import com.fourthread.ozang.core.domain.follow.dto.FollowDto;

public record FollowedEvent(
        FollowDto dto
) {
}
