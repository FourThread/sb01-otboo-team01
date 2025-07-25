package com.fourthread.ozang.domain.notification.event;

import com.fourthread.ozang.domain.dm.dto.DirectMessageDto;

public record DmReceivedEvent(
        DirectMessageDto dmDto
) {
}
