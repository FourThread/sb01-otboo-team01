package com.fourthread.ozang.app.domain.notification.event;

import com.fourthread.ozang.app.domain.dm.dto.DirectMessageDto;

public record DmReceivedEvent(
        DirectMessageDto dmDto
) {
}
