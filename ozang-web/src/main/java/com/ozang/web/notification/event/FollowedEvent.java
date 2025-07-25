package com.ozang.web.notification.event;

import com.ozang.web.follow.dto.FollowDto;

public record FollowedEvent(
        FollowDto dto
) {
}
