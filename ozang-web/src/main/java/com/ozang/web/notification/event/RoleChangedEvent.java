package com.ozang.web.notification.event;

import com.ozang.web.user.dto.data.UserDto;

import java.util.UUID;

public record RoleChangedEvent(
        UserDto userDto,
        UUID requesterId
) {
}
