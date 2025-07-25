package com.ozang.common.domain.notification.event;

import com.ozang.common.domain.user.dto.data.UserDto;

import java.util.UUID;

public record RoleChangedEvent(
        UserDto userDto,
        UUID requesterId
) {
}
