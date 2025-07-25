package com.fourthread.ozang.domain.notification.event;

import com.fourthread.ozang.module.domain.dm.dto.DirectMessageDto;

public record DmReceivedEvent(
        DirectMessageDto dmDto
) {
}
