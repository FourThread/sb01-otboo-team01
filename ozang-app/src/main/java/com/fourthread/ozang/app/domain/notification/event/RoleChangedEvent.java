package com.fourthread.ozang.app.domain.notification.event;

import com.fourthread.ozang.app.domain.user.dto.data.UserDto;

import java.util.UUID;

public record RoleChangedEvent(
        UserDto userDto,
        UUID requesterId
) {
}
