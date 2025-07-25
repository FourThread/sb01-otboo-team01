package com.fourthread.ozang.domain.notification.event;

import com.fourthread.ozang.domain.user.dto.data.UserDto;
import java.util.UUID;

public record RoleChangedEvent(
        UserDto userDto,
        UUID requesterId
) {
}
