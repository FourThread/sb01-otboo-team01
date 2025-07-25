package com.ozang.common.domain.notification.event;

import com.ozang.common.domain.follow.dto.FollowDto;

public record FollowedEvent(
        FollowDto dto
) {
}
