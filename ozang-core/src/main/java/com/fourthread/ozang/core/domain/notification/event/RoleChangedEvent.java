package com.fourthread.ozang.core.domain.notification.event;


import com.fourthread.ozang.core.domain.user.dto.data.UserDto;
import java.util.UUID;

public record RoleChangedEvent(
        UserDto userDto,
        UUID requesterId
) {
}
