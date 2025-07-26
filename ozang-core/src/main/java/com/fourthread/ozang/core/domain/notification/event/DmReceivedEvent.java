package com.fourthread.ozang.core.domain.notification.event;


import com.fourthread.ozang.core.domain.dm.dto.DirectMessageDto;

public record DmReceivedEvent(
        DirectMessageDto dmDto
) {
}
